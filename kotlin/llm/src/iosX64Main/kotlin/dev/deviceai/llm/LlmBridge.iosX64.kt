package dev.deviceai.llm

import dev.deviceai.llm.native.*
import kotlinx.cinterop.*
import kotlin.time.measureTime

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
actual object LlmBridge {

    actual fun initLlm(modelPath: String, config: LlmConfig): Boolean =
        llm_init(modelPath, config.contextSize, config.maxThreads, config.useGpu)

    actual fun shutdown() = llm_shutdown()

    actual fun generate(prompt: String, config: LlmConfig): LlmResult {
        var text = ""
        val elapsed = measureTime {
            val result = llm_generate(
                prompt, config.systemPrompt,
                config.maxTokens, config.temperature,
                config.topP, config.topK, config.repeatPenalty
            )
            text = result?.toKString()?.also { llm_free_string(result) } ?: ""
        }
        return LlmResult(
            text = text,
            tokenCount = text.split(" ").size,
            promptTokenCount = prompt.split(" ").size,
            finishReason = FinishReason.STOP,
            generationTimeMs = elapsed.inWholeMilliseconds
        )
    }

    actual fun generateStream(prompt: String, config: LlmConfig, callback: LlmStream) {
        val ref = StableRef.create(callback)

        val onToken = staticCFunction { token: CPointer<ByteVar>?, user: COpaquePointer? ->
            val cb = user!!.asStableRef<LlmStream>().get()
            val piece = token?.toKString() ?: return@staticCFunction
            cb.onToken(piece)
        }

        val onComplete = staticCFunction { fullTextPtr: CPointer<ByteVar>?, _: Int, user: COpaquePointer? ->
            val cb = user!!.asStableRef<LlmStream>().get()
            val text = fullTextPtr?.toKString() ?: ""
            cb.onComplete(
                LlmResult(
                    text = text,
                    tokenCount = text.split(" ").size,
                    promptTokenCount = 0,
                    finishReason = FinishReason.STOP,
                    generationTimeMs = 0L
                )
            )
        }

        val onError = staticCFunction { message: CPointer<ByteVar>?, user: COpaquePointer? ->
            val cb = user!!.asStableRef<LlmStream>().get()
            cb.onError(message?.toKString() ?: "Unknown error")
        }

        llm_generate_stream(
            prompt, config.systemPrompt,
            config.maxTokens, config.temperature,
            config.topP, config.topK, config.repeatPenalty,
            onToken, onComplete, onError,
            ref.asCPointer()
        )

        ref.dispose()
    }

    actual fun cancelGeneration() = llm_cancel()
}
