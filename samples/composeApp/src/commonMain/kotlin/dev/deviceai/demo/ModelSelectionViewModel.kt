package dev.deviceai.demo

import dev.deviceai.llm.models.LlmCatalog
import dev.deviceai.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Model card state ──────────────────────────────────────────────────────────

data class ModelCardState(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val badges: List<String>,
    val isDownloaded: Boolean,
    val isActive: Boolean,
    val downloadProgress: Float?    // null=not downloading, 0–1=in progress
)

// ── Curated voice model definitions ──────────────────────────────────────────

private data class VoiceModelDef(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val badges: List<String>
)

private val CURATED_VOICE_MODELS = listOf(
    VoiceModelDef(
        id = "ggml-tiny.en.bin",
        name = "Whisper Tiny (English)",
        description = "Fastest model. Great for quick transcription on any device.",
        sizeBytes = 77_704_315L,
        badges = listOf("Recommended", "75 MB")
    ),
    VoiceModelDef(
        id = "ggml-base.en.bin",
        name = "Whisper Base (English)",
        description = "Balanced accuracy and speed. Suitable for most use cases.",
        sizeBytes = 148_004_219L,
        badges = listOf("142 MB")
    ),
    VoiceModelDef(
        id = "ggml-small.en.bin",
        name = "Whisper Small (English)",
        description = "Higher accuracy at the cost of slower inference and more memory.",
        sizeBytes = 488_636_917L,
        badges = listOf("466 MB")
    )
)

private const val WHISPER_HF_BASE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ModelSelectionViewModel {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Voice models
    private val _voiceModels = MutableStateFlow<List<ModelCardState>>(emptyList())
    val voiceModels: StateFlow<List<ModelCardState>> = _voiceModels.asStateFlow()

    // Chat models
    private val _chatModels = MutableStateFlow<List<ModelCardState>>(emptyList())
    val chatModels: StateFlow<List<ModelCardState>> = _chatModels.asStateFlow()

    // Active model paths
    private val _activeVoicePath = MutableStateFlow<String?>(null)
    val activeVoicePath: StateFlow<String?> = _activeVoicePath.asStateFlow()

    private val _activeChatPath = MutableStateFlow<String?>(null)
    val activeChatPath: StateFlow<String?> = _activeChatPath.asStateFlow()

    // Active model IDs (used to drive card state)
    private val _activeVoiceId = MutableStateFlow<String?>(null)
    private val _activeChatId = MutableStateFlow<String?>(null)

    val canOpenApp: Boolean
        get() = _activeVoicePath.value != null

    // ── Initialization ────────────────────────────────────────────────────────

    fun initialize() {
        scope.launch {
            withContext(Dispatchers.IO) { ModelRegistry.initialize() }

            // Seed voice card list with download status from local store
            val initialVoice = CURATED_VOICE_MODELS.map { def ->
                val local = ModelRegistry.getLocalModel(def.id)
                ModelCardState(
                    id               = def.id,
                    name             = def.name,
                    description      = def.description,
                    sizeBytes        = def.sizeBytes,
                    badges           = def.badges,
                    isDownloaded     = local != null,
                    isActive         = false,
                    downloadProgress = null
                )
            }
            _voiceModels.value = initialVoice

            // Seed chat card list
            val initialChat = LlmCatalog.all.map { info ->
                val local = ModelRegistry.getLocalModel(info.id)
                ModelCardState(
                    id               = info.id,
                    name             = info.name,
                    description      = info.description,
                    sizeBytes        = info.sizeBytes,
                    badges           = listOf(info.parameters, info.quantization),
                    isDownloaded     = local != null,
                    isActive         = false,
                    downloadProgress = null
                )
            }
            _chatModels.value = initialChat

            // Auto-select first downloaded voice model
            val firstVoice = initialVoice.firstOrNull { it.isDownloaded }
            if (firstVoice != null) {
                activateVoice(firstVoice.id)
            }

            // Auto-select first downloaded chat model
            val firstChat = initialChat.firstOrNull { it.isDownloaded }
            if (firstChat != null) {
                activateChat(firstChat.id)
            }
        }
    }

    // ── Download ──────────────────────────────────────────────────────────────

    fun downloadVoiceModel(id: String) {
        val def = CURATED_VOICE_MODELS.firstOrNull { it.id == id } ?: return
        val url = "$WHISPER_HF_BASE/$id"

        scope.launch {
            // Mark as downloading
            updateVoiceCard(id) { it.copy(downloadProgress = 0f) }

            val result = withContext(Dispatchers.IO) {
                ModelRegistry.downloadRawFile(
                    modelId   = id,
                    url       = url,
                    modelType = LocalModelType.WHISPER,
                    onProgress = { progress ->
                        scope.launch {
                            updateVoiceCard(id) {
                                it.copy(downloadProgress = (progress.percentComplete / 100f).coerceIn(0f, 1f))
                            }
                        }
                    }
                )
            }

            result.fold(
                onSuccess = { local ->
                    updateVoiceCard(id) { it.copy(isDownloaded = true, downloadProgress = null) }
                    // Auto-activate if no voice model is active yet
                    if (_activeVoiceId.value == null) {
                        activateVoice(id)
                    }
                },
                onFailure = {
                    updateVoiceCard(id) { it.copy(downloadProgress = null) }
                }
            )
        }
    }

    fun downloadChatModel(id: String) {
        val info = LlmCatalog.all.firstOrNull { it.id == id } ?: return
        val url = "https://huggingface.co/${info.repoId}/resolve/main/${info.filename}"

        scope.launch {
            // Mark as downloading
            updateChatCard(id) { it.copy(downloadProgress = 0f) }

            val result = withContext(Dispatchers.IO) {
                ModelRegistry.downloadRawFile(
                    modelId   = id,
                    url       = url,
                    modelType = LocalModelType("LLM"),
                    onProgress = { progress ->
                        scope.launch {
                            updateChatCard(id) {
                                it.copy(downloadProgress = (progress.percentComplete / 100f).coerceIn(0f, 1f))
                            }
                        }
                    }
                )
            }

            result.fold(
                onSuccess = { local ->
                    updateChatCard(id) { it.copy(isDownloaded = true, downloadProgress = null) }
                    // Auto-activate if no chat model is active yet
                    if (_activeChatId.value == null) {
                        activateChat(id)
                    }
                },
                onFailure = {
                    updateChatCard(id) { it.copy(downloadProgress = null) }
                }
            )
        }
    }

    // ── Activation ────────────────────────────────────────────────────────────

    fun setActiveVoiceModel(id: String) {
        activateVoice(id)
    }

    fun setActiveChatModel(id: String) {
        activateChat(id)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun activateVoice(id: String) {
        val path = ModelRegistry.getLocalModel(id)?.modelPath ?: return
        _activeVoiceId.value = id
        _activeVoicePath.value = path
        _voiceModels.value = _voiceModels.value.map { card ->
            card.copy(isActive = card.id == id)
        }
    }

    private fun activateChat(id: String) {
        val path = ModelRegistry.getLocalModel(id)?.modelPath ?: return
        _activeChatId.value = id
        _activeChatPath.value = path
        _chatModels.value = _chatModels.value.map { card ->
            card.copy(isActive = card.id == id)
        }
    }

    private fun updateVoiceCard(id: String, transform: (ModelCardState) -> ModelCardState) {
        _voiceModels.value = _voiceModels.value.map { if (it.id == id) transform(it) else it }
    }

    private fun updateChatCard(id: String, transform: (ModelCardState) -> ModelCardState) {
        _chatModels.value = _chatModels.value.map { if (it.id == id) transform(it) else it }
    }
}
