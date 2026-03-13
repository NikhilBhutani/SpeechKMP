package dev.deviceai.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun LlmTabContent(viewModel: LlmViewModel, padding: PaddingValues) {
    val navigator = LocalNavigator.currentOrThrow
    val modelSelVm: ModelSelectionViewModel = koinInject()

    val state by viewModel.state.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val tokensPerSec by viewModel.tokensPerSec.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()
    val activeChatPath by modelSelVm.activeChatPath.collectAsState()

    // Extract display name from file path
    val activeModelName: String? = activeChatPath?.let { path ->
        val filename = path.substringAfterLast('/').substringAfterLast('\\')
        val nameNoExt = filename.substringBeforeLast('.')
        nameNoExt.replace('-', ' ').replace('_', ' ')
            .split(' ')
            .take(3)
            .joinToString(" ")
            .take(18)
            .trim()
    }

    // Initialize when path changes
    LaunchedEffect(activeChatPath) {
        activeChatPath?.let { viewModel.initialize(it) }
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
            ModelChip(
                name = activeModelName,
                onClick = { navigator.push(ModelPickerScreen("LLM")) }
            )
        }

        // Stats row — only show when Ready
        if (state is LlmState.Ready) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tpsStr = if (tokensPerSec > 0f) {
                    val whole = tokensPerSec.toLong()
                    val dec = ((tokensPerSec - whole) * 10).toLong()
                    "$whole.$dec t/s"
                } else "— t/s"
                val latStr = if (latencyMs > 0L) "$latencyMs ms" else "— ms"

                StatCard(
                    label = "TOKENS/SEC",
                    value = tpsStr,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "LATENCY",
                    value = latStr,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Content area
        Box(modifier = Modifier.weight(1f)) {
            when (val s = state) {
                is LlmState.NotAvailable -> LlmEmptyState()
                is LlmState.Loading      -> LlmLoadingContent()
                is LlmState.Ready        -> LlmChatContent(messages, isGenerating, viewModel)
                is LlmState.Error        -> LlmErrorContent(s.msg)
            }
        }

        // Input bar
        LlmInputBar(
            isGenerating = isGenerating,
            onSend = { text -> viewModel.sendMessage(text) }
        )
    }
}

// ── ModelChip ─────────────────────────────────────────────────────────────────

@Composable
private fun ModelChip(name: String?, onClick: () -> Unit) {
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

// ── StatCard ──────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = SpaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceLine)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Muted
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = White
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun LlmEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Muted
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No model selected",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tap 'Select model' above to get started.",
            style = MaterialTheme.typography.labelSmall,
            color = SpaceLine,
            textAlign = TextAlign.Center
        )
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LlmLoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Amber)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Loading model...",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun LlmErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap 'Select model' above to choose a different model.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            textAlign = TextAlign.Center
        )
    }
}

// ── Chat content ──────────────────────────────────────────────────────────────

@Composable
private fun LlmChatContent(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    viewModel: LlmViewModel
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty()) {
        LlmEmptyState()
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            LlmChatBubble(message)
        }
    }
}

@Composable
private fun LlmChatBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            // Robot avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SpaceCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) Amber else SpaceCard
            ) {
                val displayText = if (message.isStreaming && message.text.isEmpty()) "▌"
                                  else if (message.isStreaming) message.text + " ▌"
                                  else message.text
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Black else White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            if (!isUser && !message.isStreaming && message.text.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                val timeStr = formatTimestamp(message.timestampMs)
                val tokenStr = if (message.tokenCount > 0) "· ${message.tokenCount} tokens" else ""
                Text(
                    text = "$timeStr $tokenStr".trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun LlmInputBar(
    isGenerating: Boolean,
    onSend: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceCard)
            .border(
                width = 1.dp,
                color = SpaceLine,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
            .padding(12.dp)
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = {
                Text(
                    "Type a message or command...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Amber,
                unfocusedBorderColor = SpaceLine,
                focusedTextColor = White,
                unfocusedTextColor = White,
                cursorColor = Amber,
                focusedPlaceholderColor = Muted,
                unfocusedPlaceholderColor = Muted
            )
        )
        Spacer(Modifier.width(8.dp))
        val canSend = inputText.isNotBlank() && !isGenerating
        IconButton(
            onClick = {
                if (canSend) {
                    onSend(inputText.trim())
                    inputText = ""
                }
            },
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (canSend) Amber else SpaceCard,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (canSend) Black else Muted
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatTimestamp(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    // Simple HH:MM display using epoch arithmetic (no platform date APIs)
    val totalSeconds = epochMs / 1000L
    val hours = (totalSeconds / 3600L) % 24L
    val minutes = (totalSeconds / 60L) % 60L
    val h = hours.toString().padStart(2, '0')
    val m = minutes.toString().padStart(2, '0')
    return "$h:$m"
}
