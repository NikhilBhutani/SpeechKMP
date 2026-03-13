package dev.deviceai.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
fun TtsTabContent(padding: PaddingValues) {
    val navigator = LocalNavigator.currentOrThrow
    val modelSelVm: ModelSelectionViewModel = koinInject()
    val activeVoicePath by modelSelVm.activeVoicePath.collectAsState()

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
            TtsModelChip(
                name = null,
                onClick = { navigator.push(ModelPickerScreen("TTS")) }
            )
        }

        // Placeholder body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Muted
                )
                Text(
                    text = "TTS coming soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Text(
                    text = "Select a TTS model above to get started.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpaceLine
                )
            }
        }
    }
}

@Composable
private fun TtsModelChip(name: String?, onClick: () -> Unit) {
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
