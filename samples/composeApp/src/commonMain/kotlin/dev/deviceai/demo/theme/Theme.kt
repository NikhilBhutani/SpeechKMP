package dev.deviceai.demo.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Website color tokens ──────────────────────────────────────────────────────
// Source: deviceai.dev globals.css
val Amber     = Color(0xFFF59E0B)   // --color-amber
val AmberDark = Color(0xFFD97706)   // --color-amber-d
val Cyan      = Color(0xFF06B6D4)   // --color-cyan
val Muted     = Color(0xFF71717A)   // --color-muted

val Black     = Color(0xFF000000)
val Space     = Color(0xFF0A0A0A)   // near-black surface
val SpaceCard = Color(0xFF111111)   // card surface
val SpaceLine = Color(0xFF1F1F1F)   // subtle divider / outline
val White     = Color(0xFFFFFFFF)

private val ColorScheme = darkColorScheme(
    primary              = Amber,
    onPrimary            = Black,
    primaryContainer     = Color(0xFF1C1400),
    onPrimaryContainer   = Color(0xFFFBBF24),

    secondary            = Cyan,
    onSecondary          = Black,
    secondaryContainer   = Color(0xFF001A1F),
    onSecondaryContainer = Color(0xFF67E8F9),

    background           = Black,
    onBackground         = White,

    surface              = Space,
    onSurface            = White,

    surfaceVariant       = SpaceCard,
    onSurfaceVariant     = Muted,

    outline              = SpaceLine,

    error                = Color(0xFFEF4444),
    onError              = Black,
    errorContainer       = Color(0xFF1A0000),
    onErrorContainer     = Color(0xFFFCA5A5),
)

@Composable
fun DeviceAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
