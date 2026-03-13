package dev.deviceai.demo

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.deviceai.demo.theme.Amber
import dev.deviceai.demo.theme.Black
import dev.deviceai.demo.theme.Muted
import dev.deviceai.demo.theme.SpaceCard
import dev.deviceai.demo.theme.SpaceLine
import dev.deviceai.demo.theme.White
import org.koin.compose.koinInject

@Composable
fun SpeechTabContent(viewModel: SpeechViewModel, padding: PaddingValues) {
    val navigator = LocalNavigator.currentOrThrow
    val modelSelVm: ModelSelectionViewModel = koinInject()

    val recordingState by viewModel.recordingState.collectAsState()
    val activeVoicePath by modelSelVm.activeVoicePath.collectAsState()
    val voiceModels by modelSelVm.voiceModels.collectAsState()

    // Look up the curated display name for the active model
    val activeModelName: String? = if (activeVoicePath != null) {
        voiceModels.firstOrNull { it.isActive }?.name
    } else null

    // Initialize when active voice path changes
    LaunchedEffect(activeVoicePath) {
        activeVoicePath?.let { viewModel.initialize(it) }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetRecording() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Black)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "DeviceAI",
                    fontWeight = FontWeight.SemiBold,
                    color = White,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.weight(1f))
            // Right: model chip
            SttModelChip(
                name = activeModelName,
                onClick = { navigator.push(ModelPickerScreen("STT")) }
            )
        }

        // Recording UI
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            TranscriptArea(recordingState)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 52.dp),
        ) {
            StatusLabel(recordingState)
            Spacer(Modifier.height(24.dp))
            MicButton(
                recordingState = recordingState,
                onClick = { viewModel.onMicButtonClicked() }
            )
        }
    }
}

// ── STT Model Chip ────────────────────────────────────────────────────────────

@Composable
private fun SttModelChip(name: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = SpaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (name != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Amber, CircleShape)
                )
                Text(
                    text = name.take(18),
                    style = MaterialTheme.typography.labelMedium,
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Select model",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ── Transcript area ────────────────────────────────────────────────────────────

@Composable
private fun TranscriptArea(state: RecordingState) {
    when (state) {
        is RecordingState.Idle -> HintText("Tap the mic below\nand start speaking")

        is RecordingState.Recording -> WaveformBars()

        is RecordingState.Transcribing -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = Amber,
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Transcribing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
        }

        is RecordingState.Result -> TranscriptCard(state.text)

        is RecordingState.Error -> {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Muted,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun WaveformBars() {
    val transition = rememberInfiniteTransition(label = "waveform")
    val barCount = 7
    val heights = (0 until barCount).map { i ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 350 + i * 70,
                    easing = EaseInOut
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(56.dp)
        ) {
            heights.forEach { heightFraction ->
                val h by heightFraction
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight(h)
                        .background(Amber, RoundedCornerShape(4.dp))
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
    }
}

@Composable
private fun TranscriptCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SpaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceLine)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "TRANSCRIPTION",
                style = MaterialTheme.typography.labelSmall,
                color = Amber,
                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = White
            )
        }
    }
}

// ── Status label ──────────────────────────────────────────────────────────────

@Composable
private fun StatusLabel(state: RecordingState) {
    val (label, color) = when (state) {
        is RecordingState.Idle         -> "Ready to record" to Muted
        is RecordingState.Recording    -> "Recording  ·  tap to stop" to MaterialTheme.colorScheme.error
        is RecordingState.Transcribing -> "Processing audio..." to Amber
        is RecordingState.Result       -> "Tap to record again" to Muted
        is RecordingState.Error        -> "Error  ·  tap to retry" to MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color
    )
}

// ── Mic / Stop button ─────────────────────────────────────────────────────────

@Composable
private fun MicButton(recordingState: RecordingState, onClick: () -> Unit) {
    val isRecording    = recordingState is RecordingState.Recording
    val isTranscribing = recordingState is RecordingState.Transcribing

    Box(contentAlignment = Alignment.Center) {
        // Pulsing ring — only visible while recording
        if (isRecording) {
            val pulse = rememberInfiniteTransition(label = "pulse")
            val scale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.65f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = EaseOut),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseScale"
            )
            val alpha by pulse.animateFloat(
                initialValue = 0.55f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseAlpha"
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = alpha),
                        CircleShape
                    )
            )
        }

        FloatingActionButton(
            onClick = { if (!isTranscribing) onClick() },
            modifier = Modifier.size(76.dp),
            containerColor = when {
                isRecording    -> MaterialTheme.colorScheme.error
                isTranscribing -> SpaceCard
                else           -> Amber
            },
            contentColor = when {
                isTranscribing -> Muted
                else           -> Black
            }
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
