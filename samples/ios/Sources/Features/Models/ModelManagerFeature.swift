import ComposableArchitecture

@Reducer
struct ModelManagerFeature {

    @ObservableState
    struct State: Equatable {
        var sttEntries: IdentifiedArrayOf<SttEntry> = []
        var llmEntries: IdentifiedArrayOf<LlmEntry> = []
        var selectedSttId: String? = nil
        var selectedLlmId: String? = nil
    }

    // MARK: - Presentation entries (wrap domain model + download status)

    struct SttEntry: Identifiable, Equatable {
        var id: String { model.id }
        let model: AppSttModel
        var downloadStatus: ModelDownloadStatus = .notDownloaded
    }

    struct LlmEntry: Identifiable, Equatable {
        var id: String { model.id }
        let model: AppLlmModel
        var downloadStatus: ModelDownloadStatus = .notDownloaded
    }

    enum ModelDownloadStatus: Equatable {
        case notDownloaded
        case downloading(Double)
        case downloaded
    }

    // MARK: - Actions

    enum Action {
        case appeared
        case sttDownloadTapped(String)
        case llmDownloadTapped(String)
        case sttSelectTapped(String)
        case llmSelectTapped(String)

        case _sttStatusChecked(String, Bool)
        case _llmStatusChecked(String, Bool)
        case _sttProgress(String, Double)
        case _sttCompleted(String)
        case _sttFailed(String)
        case _llmProgress(String, Double)
        case _llmCompleted(String)
        case _llmFailed(String)

        case delegate(Delegate)
        enum Delegate: Equatable {
            case sttModelSelected(path: String)
            case llmModelSelected(path: String)
        }
    }

    @Dependency(\.modelUseCase) var modelUseCase

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {

            // ── Populate catalogue + check which are already downloaded ────────

            case .appeared:
                // Populate entries from catalogue (idempotent — skip if already populated)
                if state.sttEntries.isEmpty {
                    state.sttEntries = .init(
                        uniqueElements: modelUseCase.allSttModels().map { SttEntry(model: $0) }
                    )
                }
                if state.llmEntries.isEmpty {
                    state.llmEntries = .init(
                        uniqueElements: modelUseCase.allLlmModels().map { LlmEntry(model: $0) }
                    )
                }
                let sttIds = state.sttEntries.map(\.id)
                let llmIds = state.llmEntries.map(\.id)
                return .merge(
                    .run { [sttIds] send in
                        for id in sttIds {
                            let ok = await modelUseCase.isSttDownloaded(id)
                            await send(._sttStatusChecked(id, ok))
                        }
                    },
                    .run { [llmIds] send in
                        for id in llmIds {
                            let ok = await modelUseCase.isLlmDownloaded(id)
                            await send(._llmStatusChecked(id, ok))
                        }
                    }
                )

            case ._sttStatusChecked(let id, let downloaded):
                state.sttEntries[id: id]?.downloadStatus = downloaded ? .downloaded : .notDownloaded
                if downloaded, state.selectedSttId == nil {
                    return .send(.sttSelectTapped(id))
                }
                return .none

            case ._llmStatusChecked(let id, let downloaded):
                state.llmEntries[id: id]?.downloadStatus = downloaded ? .downloaded : .notDownloaded
                if downloaded, state.selectedLlmId == nil {
                    return .send(.llmSelectTapped(id))
                }
                return .none

            // ── Downloads ──────────────────────────────────────────────────────

            case .sttDownloadTapped(let id):
                guard state.sttEntries[id: id]?.downloadStatus == .notDownloaded else { return .none }
                state.sttEntries[id: id]?.downloadStatus = .downloading(0)
                return .run { [id] send in
                    do {
                        for try await fraction in modelUseCase.downloadStt(id) {
                            await send(._sttProgress(id, fraction))
                        }
                        await send(._sttCompleted(id))
                    } catch {
                        await send(._sttFailed(id))
                    }
                }
                .cancellable(id: "stt-dl-\(id)")

            case .llmDownloadTapped(let id):
                guard state.llmEntries[id: id]?.downloadStatus == .notDownloaded else { return .none }
                state.llmEntries[id: id]?.downloadStatus = .downloading(0)
                return .run { [id] send in
                    do {
                        for try await fraction in modelUseCase.downloadLlm(id) {
                            await send(._llmProgress(id, fraction))
                        }
                        await send(._llmCompleted(id))
                    } catch {
                        await send(._llmFailed(id))
                    }
                }
                .cancellable(id: "llm-dl-\(id)")

            case ._sttProgress(let id, let f):
                state.sttEntries[id: id]?.downloadStatus = .downloading(f)
                return .none

            case ._sttCompleted(let id):
                state.sttEntries[id: id]?.downloadStatus = .downloaded
                if state.selectedSttId == nil { return .send(.sttSelectTapped(id)) }
                return .none

            case ._sttFailed(let id):
                state.sttEntries[id: id]?.downloadStatus = .notDownloaded
                return .none

            case ._llmProgress(let id, let f):
                state.llmEntries[id: id]?.downloadStatus = .downloading(f)
                return .none

            case ._llmCompleted(let id):
                state.llmEntries[id: id]?.downloadStatus = .downloaded
                if state.selectedLlmId == nil { return .send(.llmSelectTapped(id)) }
                return .none

            case ._llmFailed(let id):
                state.llmEntries[id: id]?.downloadStatus = .notDownloaded
                return .none

            // ── Selection ──────────────────────────────────────────────────────

            case .sttSelectTapped(let id):
                guard state.sttEntries[id: id]?.downloadStatus == .downloaded else { return .none }
                state.selectedSttId = id
                let path = modelUseCase.localSttPath(id)
                return .send(.delegate(.sttModelSelected(path: path)))

            case .llmSelectTapped(let id):
                guard state.llmEntries[id: id]?.downloadStatus == .downloaded else { return .none }
                state.selectedLlmId = id
                let path = modelUseCase.localLlmPath(id)
                return .send(.delegate(.llmModelSelected(path: path)))

            case .delegate:
                return .none
            }
        }
    }
}
