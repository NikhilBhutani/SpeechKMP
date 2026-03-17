import SwiftUI

@Observable
@MainActor
final class ModelsViewModel {

    // MARK: - Presentation entries

    struct SttEntry: Identifiable {
        var id: String { model.id }
        let model: AppSttModel
        var downloadStatus: DownloadStatus = .notDownloaded
    }

    struct LlmEntry: Identifiable {
        var id: String { model.id }
        let model: AppLlmModel
        var downloadStatus: DownloadStatus = .notDownloaded
    }

    enum DownloadStatus: Equatable {
        case notDownloaded
        case downloading(Double)
        case downloaded
    }

    // MARK: - State

    var sttEntries: [SttEntry] = []
    var llmEntries: [LlmEntry] = []
    var selectedSttId: String? = nil
    var selectedLlmId: String? = nil

    var onSttSelected: (String) -> Void = { _ in }
    var onLlmSelected: (String) -> Void = { _ in }

    private let models: any ModelRepository
    private var downloadTasks: [String: Task<Void, Never>] = [:]

    init(models: any ModelRepository) {
        self.models = models
    }

    // MARK: - Public interface

    func loadIfNeeded() {
        guard sttEntries.isEmpty else { return }
        sttEntries = models.allSttModels().map { SttEntry(model: $0) }
        llmEntries = models.allLlmModels().map { LlmEntry(model: $0) }
        Task { await checkDownloadStatus() }
    }

    func downloadStt(_ id: String) {
        guard let idx = sttEntries.firstIndex(where: { $0.id == id }),
              sttEntries[idx].downloadStatus == .notDownloaded else { return }
        sttEntries[idx].downloadStatus = .downloading(0)
        downloadTasks[id] = Task {
            do {
                for try await fraction in models.downloadStt(id) {
                    if let i = sttEntries.firstIndex(where: { $0.id == id }) {
                        sttEntries[i].downloadStatus = .downloading(fraction)
                    }
                }
                if let i = sttEntries.firstIndex(where: { $0.id == id }) {
                    sttEntries[i].downloadStatus = .downloaded
                }
                if selectedSttId == nil { selectStt(id) }
            } catch {
                if let i = sttEntries.firstIndex(where: { $0.id == id }) {
                    sttEntries[i].downloadStatus = .notDownloaded
                }
            }
        }
    }

    func downloadLlm(_ id: String) {
        guard let idx = llmEntries.firstIndex(where: { $0.id == id }),
              llmEntries[idx].downloadStatus == .notDownloaded else { return }
        llmEntries[idx].downloadStatus = .downloading(0)
        downloadTasks[id] = Task {
            do {
                for try await fraction in models.downloadLlm(id) {
                    if let i = llmEntries.firstIndex(where: { $0.id == id }) {
                        llmEntries[i].downloadStatus = .downloading(fraction)
                    }
                }
                if let i = llmEntries.firstIndex(where: { $0.id == id }) {
                    llmEntries[i].downloadStatus = .downloaded
                }
                if selectedLlmId == nil { selectLlm(id) }
            } catch {
                if let i = llmEntries.firstIndex(where: { $0.id == id }) {
                    llmEntries[i].downloadStatus = .notDownloaded
                }
            }
        }
    }

    func selectStt(_ id: String) {
        guard sttEntries.first(where: { $0.id == id })?.downloadStatus == .downloaded else { return }
        selectedSttId = id
        onSttSelected(models.localSttPath(id))
    }

    func selectLlm(_ id: String) {
        guard llmEntries.first(where: { $0.id == id })?.downloadStatus == .downloaded else { return }
        selectedLlmId = id
        onLlmSelected(models.localLlmPath(id))
    }

    // MARK: - Private

    private func checkDownloadStatus() async {
        for entry in sttEntries {
            let ok = await models.isSttDownloaded(entry.id)
            if let i = sttEntries.firstIndex(where: { $0.id == entry.id }) {
                sttEntries[i].downloadStatus = ok ? .downloaded : .notDownloaded
            }
            if ok && selectedSttId == nil { selectStt(entry.id) }
        }
        for entry in llmEntries {
            let ok = await models.isLlmDownloaded(entry.id)
            if let i = llmEntries.firstIndex(where: { $0.id == entry.id }) {
                llmEntries[i].downloadStatus = ok ? .downloaded : .notDownloaded
            }
            if ok && selectedLlmId == nil { selectLlm(entry.id) }
        }
    }
}
