import SwiftUI

@Observable
@MainActor
final class ChatViewModel {
    var modelPath: String? = nil
    var messages: [ChatMessage] = []
    var inputText = ""
    var isGenerating = false
    var streamingText = ""
    var errorMessage: String? = nil

    private let chat: any ChatRepository
    private var generationTask: Task<Void, Never>?

    init(chat: any ChatRepository) {
        self.chat = chat
    }

    func sendTapped() {
        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !isGenerating else { return }
        guard let modelPath else {
            errorMessage = "No LLM model selected. Tap the cpu icon to download one."
            return
        }
        messages.append(ChatMessage(role: .user, text: text))
        inputText = ""
        isGenerating = true
        streamingText = ""
        errorMessage = nil
        let path = modelPath
        generationTask = Task {
            do {
                for try await token in await chat.send(text, modelPath: path) {
                    streamingText += token
                }
                if !streamingText.isEmpty {
                    messages.append(ChatMessage(role: .assistant, text: streamingText))
                }
                streamingText = ""
                isGenerating = false
            } catch {
                errorMessage = error.localizedDescription
                streamingText = ""
                isGenerating = false
            }
        }
    }

    func cancelTapped() {
        generationTask?.cancel()
        chat.cancel()
        isGenerating = false
        streamingText = ""
    }

    func clearTapped() {
        messages = []
        streamingText = ""
        errorMessage = nil
        Task { await chat.clearHistory() }
    }
}

// MARK: - Presentation model

struct ChatMessage: Identifiable {
    let id = UUID()
    let role: Role
    let text: String

    enum Role { case user, assistant }
}
