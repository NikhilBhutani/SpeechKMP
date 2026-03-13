package dev.deviceai.demo

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import dev.deviceai.demo.theme.Amber
import dev.deviceai.demo.theme.Black
import dev.deviceai.demo.theme.Muted
import dev.deviceai.demo.theme.SpaceCard
import org.koin.compose.koinInject

class MainScreen : Screen {

    @Composable
    override fun Content() {
        var selectedTab by remember { mutableStateOf(0) }
        val speechVm: SpeechViewModel = koinInject()
        val llmVm: LlmViewModel = koinInject()

        val tabs = listOf(
            Triple("CHAT", Icons.Default.SmartToy, 0),
            Triple("STT", Icons.Default.Mic, 1),
            Triple("TTS", Icons.AutoMirrored.Filled.VolumeUp, 2),
            Triple("SETTINGS", Icons.Default.Settings, 3)
        )

        Scaffold(
            containerColor = Black,
            bottomBar = {
                NavigationBar(
                    containerColor = SpaceCard
                ) {
                    tabs.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Amber,
                                selectedTextColor = Amber,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = Color(0xFF1C1400)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> LlmTabContent(llmVm, padding)
                1 -> SpeechTabContent(speechVm, padding)
                2 -> TtsTabContent(padding)
                3 -> SettingsTabContent(padding)
            }
        }
    }
}
