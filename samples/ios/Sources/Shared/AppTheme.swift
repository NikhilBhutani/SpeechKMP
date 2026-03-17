import SwiftUI

/// Design tokens for DeviceAI Labs sample app.
/// Mirrors the dark purple + cobalt palette from the KMP Compose theme.
enum AppTheme {
    /// Primary accent — cobalt blue, matches deviceai.dev brand.
    static let accent = Color(red: 0, green: 0.384, blue: 1)         // #0062FF

    /// Deep space background — top of gradient.
    static let backgroundTop = Color(red: 0.039, green: 0.067, blue: 0.125)  // #0A1120

    /// Navy surface — bottom of gradient.
    static let backgroundBottom = Color(red: 0.086, green: 0.122, blue: 0.188) // #161F30

    static let backgroundGradient = LinearGradient(
        colors: [backgroundTop, backgroundBottom],
        startPoint: .top,
        endPoint: .bottom
    )
}

// MARK: - Liquid Glass progressive enhancement

extension View {
    /// Applies Liquid Glass card effect on iOS 26+; no-op on iOS 17-25.
    @ViewBuilder
    func liquidGlassCardIfAvailable() -> some View {
        if #available(iOS 26, *) {
            self.glassBackgroundEffect()
        } else {
            self
        }
    }

    /// Applies Liquid Glass badge effect on iOS 26+.
    @ViewBuilder
    func liquidGlassBadgeIfAvailable() -> some View {
        if #available(iOS 26, *) {
            self.glassBackgroundEffect(in: Circle())
        } else {
            self
        }
    }

    /// Applies Liquid Glass to a button on iOS 26+.
    @ViewBuilder
    func liquidGlassIfAvailable() -> some View {
        if #available(iOS 26, *) {
            self.glassBackgroundEffect()
        } else {
            self
        }
    }

    /// Liquid Glass chat bubble on iOS 26+ (assistant only).
    @ViewBuilder
    func liquidGlassBubbleIfAvailable(isUser: Bool) -> some View {
        if #available(iOS 26, *), !isUser {
            self.glassBackgroundEffect(in: RoundedRectangle(cornerRadius: 18))
        } else {
            self
        }
    }
}
