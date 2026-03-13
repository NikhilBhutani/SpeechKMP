package dev.deviceai.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.deviceai.demo.theme.Amber
import dev.deviceai.demo.theme.Black
import dev.deviceai.demo.theme.SpaceCard
import dev.deviceai.demo.theme.SpaceLine
import dev.deviceai.demo.theme.Muted
import dev.deviceai.demo.theme.White

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(SpaceCard, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Title
                Text(
                    text = "DeviceAI",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )

                Spacer(Modifier.height(12.dp))

                // Subtitle
                Text(
                    text = "On-device LLM · Speech · TTS\nRuns fully offline, no cloud required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(40.dp))

                // Get Started button
                Button(
                    onClick = { navigator.replace(MainScreen()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = Black
                    )
                ) {
                    Text(
                        text = "Get Started",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Black
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Footer
                Text(
                    text = "Free & open source  ·  Apache 2.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpaceLine
                )
            }
        }
    }
}
