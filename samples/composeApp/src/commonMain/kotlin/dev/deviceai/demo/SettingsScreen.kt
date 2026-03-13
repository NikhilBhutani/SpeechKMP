package dev.deviceai.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceai.demo.theme.Amber
import dev.deviceai.demo.theme.Black
import dev.deviceai.demo.theme.Muted
import dev.deviceai.demo.theme.SpaceCard
import dev.deviceai.demo.theme.SpaceLine
import dev.deviceai.demo.theme.White

@Composable
fun SettingsTabContent(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Black)
    ) {
        // Header row
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
            Text(
                text = "v0.2.0-alpha",
                style = MaterialTheme.typography.labelSmall,
                color = Muted
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Model Storage",
                    description = "Manage downloaded models"
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Engine",
                    description = "llama.cpp · whisper.cpp · sherpa-onnx"
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Smartphone,
                    title = "Platform",
                    description = getPlatformName()
                )
            }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = "Open Source",
                    description = "github.com/deviceai-labs/deviceai"
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SpaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
    }
}

// Simple platform detection via expect/actual would be ideal, but for commonMain
// we use a neutral string. Platform-specific overrides can be added via actual.
internal expect fun getPlatformName(): String
