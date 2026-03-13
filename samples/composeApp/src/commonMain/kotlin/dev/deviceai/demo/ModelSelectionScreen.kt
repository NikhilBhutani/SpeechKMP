package dev.deviceai.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.deviceai.demo.theme.Amber
import dev.deviceai.demo.theme.AmberDark
import dev.deviceai.demo.theme.Black
import dev.deviceai.demo.theme.Muted
import dev.deviceai.demo.theme.SpaceCard
import dev.deviceai.demo.theme.SpaceLine
import dev.deviceai.demo.theme.White
import org.koin.compose.koinInject

class ModelPickerScreen(private val category: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ModelSelectionViewModel = koinInject()

        LaunchedEffect(Unit) {
            viewModel.initialize()
        }

        val voiceModels by viewModel.voiceModels.collectAsState()
        val chatModels by viewModel.chatModels.collectAsState()

        val title = when (category) {
            "STT" -> "Select Voice Model"
            "LLM" -> "Select Chat Model"
            "TTS" -> "Select TTS Model"
            else  -> "Select Model"
        }

        val models = when (category) {
            "STT" -> voiceModels
            "LLM" -> chatModels
            else  -> emptyList()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Black,
                        titleContentColor = White,
                        navigationIconContentColor = White
                    )
                )
            },
            containerColor = Black
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    category == "TTS" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TTS models coming soon.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    models.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading models...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Muted
                            )
                        }
                    }
                    else -> {
                        ModelCardList(
                            models = models,
                            onDownload = { id ->
                                when (category) {
                                    "STT" -> viewModel.downloadVoiceModel(id)
                                    "LLM" -> viewModel.downloadChatModel(id)
                                }
                            },
                            onUse = { id ->
                                when (category) {
                                    "STT" -> {
                                        viewModel.setActiveVoiceModel(id)
                                        navigator.pop()
                                    }
                                    "LLM" -> {
                                        viewModel.setActiveChatModel(id)
                                        navigator.pop()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Model list ────────────────────────────────────────────────────────────────

@Composable
private fun ModelCardList(
    models: List<ModelCardState>,
    onDownload: (String) -> Unit,
    onUse: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        items(models) { card ->
            PickerModelCard(
                card = card,
                onDownload = { onDownload(card.id) },
                onUse = { onUse(card.id) }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Model card ────────────────────────────────────────────────────────────────

@Composable
private fun PickerModelCard(
    card: ModelCardState,
    onDownload: () -> Unit,
    onUse: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SpaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Name + badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = White,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    card.badges.forEach { badge ->
                        val isRecommended = badge.equals("Recommended", ignoreCase = true)
                        BadgeChip(label = badge, isRecommended = isRecommended)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Description
            Text(
                text = card.description,
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )

            Spacer(Modifier.height(12.dp))

            // Divider
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = SpaceLine
            ) {}

            Spacer(Modifier.height(12.dp))

            // Action area
            when {
                card.isActive -> PickerInUseChip()
                card.downloadProgress != null -> PickerDownloadProgressRow(card.downloadProgress)
                card.isDownloaded -> {
                    OutlinedButton(
                        onClick = onUse,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Amber),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber)
                    ) {
                        Text(
                            text = "Use",
                            style = MaterialTheme.typography.labelLarge,
                            color = Amber
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber,
                            contentColor = Black
                        )
                    ) {
                        Text(
                            text = "Download  ·  ${formatMbCard(card.sizeBytes)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Black
                        )
                    }
                }
            }
        }
    }
}

// ── Badge chip ────────────────────────────────────────────────────────────────

@Composable
private fun BadgeChip(label: String, isRecommended: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isRecommended) Color(0xFF1C1400) else SpaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceLine)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isRecommended) Amber else Muted,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

// ── "In use" chip ─────────────────────────────────────────────────────────────

@Composable
private fun PickerInUseChip() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Amber
    ) {
        Text(
            text = "●  In use",
            style = MaterialTheme.typography.labelLarge,
            color = Black,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// ── Download progress row ─────────────────────────────────────────────────────

@Composable
private fun PickerDownloadProgressRow(progress: Float) {
    val percent = (progress * 100).toInt()
    Column {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = Amber,
            trackColor = SpaceLine
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatMbCard(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1024.0) {
        val whole = mb.toLong()
        val decimal = ((mb - whole) * 10).toLong()
        "$whole.${decimal} MB"
    } else {
        val gb = mb / 1024.0
        val whole = gb.toLong()
        val decimal = ((gb - whole) * 10).toLong()
        "$whole.${decimal} GB"
    }
}
