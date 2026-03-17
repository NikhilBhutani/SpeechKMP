import SwiftUI
import ComposableArchitecture

struct LaunchView: View {
    let store: StoreOf<LaunchFeature>

    var body: some View {
        ZStack {
            AppTheme.backgroundGradient.ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()
                BrandingView()
                Spacer()
                statusSection
                Spacer()
            }
            .padding(.horizontal, 32)
        }
    }

    // MARK: - Status section

    @ViewBuilder
    private var statusSection: some View {
        switch store.phase {
        case .checking:
            VStack(spacing: 16) {
                ProgressView()
                    .tint(.white)
                Text("Checking models…")
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.6))
            }

        case .downloading:
            VStack(spacing: 20) {
                downloadRow(label: "Whisper Tiny (STT)", progress: store.sttProgress)
                downloadRow(label: "SmolLM 135M (LLM)", progress: store.llmProgress)
                Text("One-time download · runs fully on-device")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.5))
            }
            .frame(maxWidth: .infinity)

        case .ready:
            VStack(spacing: 20) {
                Label("Models ready", systemImage: "checkmark.circle.fill")
                    .foregroundStyle(AppTheme.accent)
                    .font(.subheadline.weight(.semibold))

                Button(action: { store.send(.getStartedTapped) }) {
                    Text("Get Started")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppTheme.accent)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .liquidGlassIfAvailable()
            }

        case .failed(let msg):
            VStack(spacing: 16) {
                Text(msg)
                    .font(.callout)
                    .foregroundStyle(.red.opacity(0.85))
                    .multilineTextAlignment(.center)
                Button("Retry") { store.send(.retryTapped) }
                    .buttonStyle(.bordered)
                    .tint(.white)
            }
        }
    }

    private func downloadRow(label: String, progress: Double) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(label)
                    .font(.subheadline)
                    .foregroundStyle(.white)
                Spacer()
                Text("\(Int(progress * 100))%")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.white.opacity(0.6))
            }
            ProgressView(value: progress)
                .tint(AppTheme.accent)
        }
    }
}

// MARK: - Branding

private struct BrandingView: View {
    @State private var pulse = false

    var body: some View {
        VStack(spacing: 28) {
            ZStack {
                // Glow ring
                Circle()
                    .fill(AppTheme.accent.opacity(0.2))
                    .frame(width: 140, height: 140)
                    .scaleEffect(pulse ? 1.12 : 1.0)
                    .animation(
                        .easeInOut(duration: 2.2).repeatForever(autoreverses: true),
                        value: pulse
                    )
                // Icon container
                Circle()
                    .fill(AppTheme.accent.opacity(0.15))
                    .frame(width: 88, height: 88)
                    .overlay(
                        Image(systemName: "waveform.and.mic")
                            .font(.system(size: 38, weight: .light))
                            .foregroundStyle(.white)
                    )
                    .liquidGlassBadgeIfAvailable()
            }
            .onAppear { pulse = true }

            VStack(spacing: 8) {
                Text("DeviceAI Labs")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)
                Text("On-device AI · LLM · STT · TTS\nruns entirely on your iPhone")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.6))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
            }
        }
    }
}
