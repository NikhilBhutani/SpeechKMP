package dev.deviceai.llm

import dev.deviceai.llm.rag.RagRetriever

/**
 * Per-request generation parameters.
 * System prompt is passed as [LlmMessage] with role [LlmRole.SYSTEM] instead.
 *
 * @param maxTokens          Maximum tokens to generate per request (default 512)
 * @param temperature        Sampling temperature — higher = more creative (default 0.7)
 * @param topP               Nucleus sampling probability threshold (default 0.9)
 * @param topK               Top-K sampling limit (default 40)
 * @param repeatPenalty      Penalty for repeating tokens (default 1.1)
 * @param enableRag          Explicitly opt in to RAG augmentation (default false).
 *                           RAG only activates when this is true AND [ragStore] is set.
 * @param ragStore           Retriever for offline RAG. Providing a store alone does NOT
 *                           activate RAG — [enableRag] must also be true.
 * @param ragTopK            Number of chunks to retrieve and inject (default 3).
 * @param ragPromptTemplate  System prompt template for injected context. Must contain
 *                           the placeholder `{context}` which is replaced with the
 *                           retrieved chunks separated by `---`. (default: see below)
 */
data class LlmGenConfig(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,

    // ── RAG ──────────────────────────────────────────────────────────
    val enableRag: Boolean = false,
    val ragStore: RagRetriever? = null,
    val ragTopK: Int = 3,
    val ragPromptTemplate: String = DEFAULT_RAG_TEMPLATE,
) {
    companion object {
        const val DEFAULT_RAG_TEMPLATE = """Use the following context to answer the question:

{context}"""
    }
}
