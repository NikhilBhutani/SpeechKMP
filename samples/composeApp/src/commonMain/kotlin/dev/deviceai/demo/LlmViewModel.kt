package dev.deviceai.demo

import dev.deviceai.llm.LlmBridge
import dev.deviceai.llm.LlmGenConfig
import dev.deviceai.llm.LlmInitConfig
import dev.deviceai.llm.LlmMessage
import dev.deviceai.llm.LlmRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── LLM loading state ─────────────────────────────────────────────────────────

sealed class LlmState {
    object NotAvailable : LlmState()                          // no model path provided
    object Loading : LlmState()                               // LlmBridge.initLlm() in progress
    object Ready : LlmState()                                 // model loaded, chat works
    data class Error(val msg: String) : LlmState()
}

// ── Chat message model ────────────────────────────────────────────────────────

enum class Role { USER, ASSISTANT }

data class ChatMessage(
    val role: Role,
    val text: String,
    val isStreaming: Boolean = false,
    val id: Long = 0L,
    val timestampMs: Long = 0L,
    val tokenCount: Int = 0
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class LlmViewModel {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Check native availability once at init — catches UnsatisfiedLinkError (extends Throwable)
    private val nativeAvailable: Boolean =
        runCatching { LlmBridge.toString(); true }.getOrElse { false }

    private val _state = MutableStateFlow<LlmState>(LlmState.NotAvailable)
    val state: StateFlow<LlmState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Stats
    private val _tokensPerSec = MutableStateFlow(0f)
    val tokensPerSec: StateFlow<Float> = _tokensPerSec.asStateFlow()

    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private var nextId = 0L
    private fun nextMessageId() = nextId++

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialize the LLM with the provided model path.
     * If [modelPath] is null, sets state to [LlmState.NotAvailable].
     * Safe to call multiple times — no-ops if already in Loading or Ready state.
     */
    fun initialize(modelPath: String?) {
        if (modelPath == null) {
            _state.value = LlmState.NotAvailable
            return
        }
        if (!nativeAvailable) {
            _state.value = LlmState.NotAvailable
            return
        }
        if (_state.value is LlmState.Loading || _state.value is LlmState.Ready) return

        loadModel(modelPath)
    }

    fun loadModel(path: String) {
        scope.launch {
            _state.value = LlmState.Loading
            val ok = runCatching {
                withContext(Dispatchers.IO) { LlmBridge.initLlm(path, LlmInitConfig()) }
            }.getOrElse { false }
            _state.value = if (ok) LlmState.Ready
                           else LlmState.Error("Failed to load model. Check the file path.")
        }
    }

    fun sendMessage(text: String) {
        if (_isGenerating.value || text.isBlank()) return

        val nowMs = currentTimeMs()
        val userMsg = ChatMessage(role = Role.USER, text = text, id = nextMessageId(), timestampMs = nowMs)
        val assistantMsg = ChatMessage(
            role = Role.ASSISTANT, text = "", isStreaming = true, id = nextMessageId(), timestampMs = nowMs
        )
        _messages.value = _messages.value + userMsg + assistantMsg
        _isGenerating.value = true

        // Reset stats
        _tokensPerSec.value = 0f
        _latencyMs.value = 0L

        // Build a clean LlmMessage list: system prompt first, then conversation history
        // up to and including the new user message.
        val systemMessage = LlmMessage(
            role = LlmRole.SYSTEM,
            content = "You are a helpful assistant."
        )
        val historyMessages = _messages.value
            .filter { it.id <= userMsg.id }
            .map { msg ->
                LlmMessage(
                    role = if (msg.role == Role.USER) LlmRole.USER else LlmRole.ASSISTANT,
                    content = msg.text
                )
            }
        val llmMessages = listOf(systemMessage) + historyMessages

        val genStartMs = currentTimeMs()
        var tokenCount = 0
        var firstTokenMs = -1L

        LlmBridge.generateStream(llmMessages, LlmGenConfig())
            .onEach { token ->
                tokenCount++
                val elapsedMs = currentTimeMs() - genStartMs

                // Record latency to first token
                if (firstTokenMs < 0L) {
                    firstTokenMs = elapsedMs
                    _latencyMs.value = firstTokenMs
                }

                // Update tokens/sec
                val elapsedSec = elapsedMs / 1000f
                if (elapsedSec > 0f) {
                    _tokensPerSec.value = tokenCount / elapsedSec
                }

                val current = _messages.value.toMutableList()
                val idx = current.indexOfLast { it.role == Role.ASSISTANT && it.isStreaming }
                if (idx >= 0) {
                    current[idx] = current[idx].copy(
                        text = current[idx].text + token,
                        tokenCount = tokenCount
                    )
                    _messages.value = current
                }
            }
            .onCompletion { markStreamingComplete() }
            .catch { e -> markError(e.message ?: "Unknown error") }
            .launchIn(scope)
    }

    fun cancelGeneration() {
        runCatching { LlmBridge.cancelGeneration() }
        val current = _messages.value.toMutableList()
        val idx = current.indexOfLast { it.role == Role.ASSISTANT && it.isStreaming }
        if (idx >= 0) {
            current[idx] = current[idx].copy(isStreaming = false)
            _messages.value = current
        }
        _isGenerating.value = false
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun markStreamingComplete() {
        val current = _messages.value.toMutableList()
        val idx = current.indexOfLast { it.role == Role.ASSISTANT && it.isStreaming }
        if (idx >= 0) {
            current[idx] = current[idx].copy(isStreaming = false)
            _messages.value = current
        }
        _isGenerating.value = false
    }

    private fun markError(message: String) {
        val current = _messages.value.toMutableList()
        val idx = current.indexOfLast { it.role == Role.ASSISTANT && it.isStreaming }
        if (idx >= 0) {
            current[idx] = current[idx].copy(text = "Error: $message", isStreaming = false)
            _messages.value = current
        }
        _isGenerating.value = false
    }
}
