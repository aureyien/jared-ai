package com.music.sttnotes.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.sttnotes.data.api.ApiConfig
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.SttProvider
import com.music.sttnotes.data.i18n.AppLanguage
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.data.stt.SttLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val openaiUsage7Days by viewModel.openaiUsage7Days.collectAsState()
    val openaiUsage30Days by viewModel.openaiUsage30Days.collectAsState()
    val openaiInputTokens7Days by viewModel.openaiInputTokens7Days.collectAsState()
    val openaiOutputTokens7Days by viewModel.openaiOutputTokens7Days.collectAsState()
    val openaiInputTokens30Days by viewModel.openaiInputTokens30Days.collectAsState()
    val openaiOutputTokens30Days by viewModel.openaiOutputTokens30Days.collectAsState()
    val anthropicUsage7Days by viewModel.anthropicUsage7Days.collectAsState()
    val anthropicUsage30Days by viewModel.anthropicUsage30Days.collectAsState()
    val anthropicInputTokens7Days by viewModel.anthropicInputTokens7Days.collectAsState()
    val anthropicOutputTokens7Days by viewModel.anthropicOutputTokens7Days.collectAsState()
    val anthropicInputTokens30Days by viewModel.anthropicInputTokens30Days.collectAsState()
    val anthropicOutputTokens30Days by viewModel.anthropicOutputTokens30Days.collectAsState()
    val isLoadingUsage by viewModel.isLoadingUsage.collectAsState()
    val strings = rememberStrings()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Language Section
            SettingsSection(title = strings.appLanguage) {
                AppLanguageSelector(
                    selected = uiState.appLanguage,
                    onSelect = viewModel::setAppLanguage
                )
            }

            // STT Language Section
            SettingsSection(title = strings.transcriptionLanguage) {
                SttLanguageSelector(
                    selected = uiState.sttLanguage,
                    onSelect = viewModel::setSttLanguage
                )
            }

            // STT Provider Section
            SettingsSection(title = strings.transcriptionStt) {
                SttProviderSelector(
                    selected = uiState.sttProvider,
                    onSelect = viewModel::setSttProvider
                )
            }

            // LLM Provider Section
            SettingsSection(title = strings.llmProcessing) {
                LlmProviderSelector(
                    selected = uiState.llmProvider,
                    onSelect = viewModel::setLlmProvider
                )
            }

            // API Keys Section - Always visible to configure all keys
            SettingsSection(title = strings.apiKeys) {
                // Groq - used for STT and LLM
                ApiKeyField(
                    label = strings.groqApiKey,
                    value = uiState.groqApiKey,
                    onValueChange = viewModel::setGroqApiKey,
                    hint = strings.freeConsole,
                    isActive = uiState.sttProvider == SttProvider.GROQ || uiState.llmProvider == LlmProvider.GROQ,
                    activeLabel = strings.active,
                    configuredLabel = strings.configured,
                    showLabel = strings.show,
                    hideLabel = strings.hide
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OpenAI - used for STT and LLM
                ApiKeyField(
                    label = strings.openaiApiKey,
                    value = uiState.openaiApiKey,
                    onValueChange = viewModel::setOpenaiApiKey,
                    hint = "platform.openai.com",
                    isActive = uiState.sttProvider == SttProvider.OPENAI || uiState.llmProvider == LlmProvider.OPENAI,
                    activeLabel = strings.active,
                    configuredLabel = strings.configured,
                    showLabel = strings.show,
                    hideLabel = strings.hide
                )

                Spacer(modifier = Modifier.height(12.dp))

                // xAI - LLM only
                ApiKeyField(
                    label = strings.xaiApiKey,
                    value = uiState.xaiApiKey,
                    onValueChange = viewModel::setXaiApiKey,
                    hint = "console.x.ai",
                    isActive = uiState.llmProvider == LlmProvider.XAI,
                    activeLabel = strings.active,
                    configuredLabel = strings.configured,
                    showLabel = strings.show,
                    hideLabel = strings.hide
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Anthropic - LLM only
                ApiKeyField(
                    label = strings.anthropicApiKey,
                    value = uiState.anthropicApiKey,
                    onValueChange = viewModel::setAnthropicApiKey,
                    hint = "console.anthropic.com",
                    isActive = uiState.llmProvider == LlmProvider.ANTHROPIC,
                    activeLabel = strings.active,
                    configuredLabel = strings.configured,
                    showLabel = strings.show,
                    hideLabel = strings.hide
                )
            }

            // LLM System Prompt
            if (uiState.llmProvider != LlmProvider.NONE) {
                SettingsSection(title = strings.llmSystemPrompt) {
                    OutlinedTextField(
                        value = uiState.llmSystemPrompt,
                        onValueChange = viewModel::setLlmSystemPrompt,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        label = { Text(strings.instructionsForLlm) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = viewModel::resetLlmSystemPrompt
                        ) {
                            Text(strings.resetToDefault)
                        }
                    }
                }
            }

            // Admin Keys Section (for usage tracking)
            SettingsSection(title = strings.adminKeysForUsageTracking) {
                // OpenAI Admin Key
                ApiKeyField(
                    label = strings.openaiAdminKey,
                    value = uiState.openaiAdminKey,
                    onValueChange = viewModel::setOpenaiAdminKey,
                    hint = "Required for billing/usage API access",
                    isActive = false,
                    activeLabel = strings.active,
                    configuredLabel = strings.configured,
                    showLabel = strings.show,
                    hideLabel = strings.hide
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Anthropic Admin Key
                ApiKeyField(
                    label = strings.anthropicAdminKey,
                    value = uiState.anthropicAdminKey,
                    onValueChange = viewModel::setAnthropicAdminKey,
                    hint = "Starts with sk-ant-admin...",
                    isActive = false,
                    activeLabel = strings.active,
                    configuredLabel = strings.configured,
                    showLabel = strings.show,
                    hideLabel = strings.hide
                )
            }

            // Usage Statistics Section (shows if either admin key is configured)
            if (uiState.openaiAdminKey.isNotBlank() || uiState.anthropicAdminKey.isNotBlank()) {
                SettingsSection(title = strings.usageStatistics) {
                    EInkCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // OpenAI Usage
                            if (uiState.openaiAdminKey.isNotBlank()) {
                                Text(
                                    text = "OpenAI (GPT)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.last7Days,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isLoadingUsage) "..." else "$${String.format("%.2f", openaiUsage7Days)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (openaiInputTokens7Days > 0 || openaiOutputTokens7Days > 0) {
                                            Text(
                                                text = "In: ${String.format("%,d", openaiInputTokens7Days)} | Out: ${String.format("%,d", openaiOutputTokens7Days)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.last30Days,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isLoadingUsage) "..." else "$${String.format("%.2f", openaiUsage30Days)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (openaiInputTokens30Days > 0 || openaiOutputTokens30Days > 0) {
                                            Text(
                                                text = "In: ${String.format("%,d", openaiInputTokens30Days)} | Out: ${String.format("%,d", openaiOutputTokens30Days)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Divider if both admin keys configured
                            if (uiState.openaiAdminKey.isNotBlank() && uiState.anthropicAdminKey.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(16.dp))
                            }

                            // Anthropic Usage
                            if (uiState.anthropicAdminKey.isNotBlank()) {
                                Text(
                                    text = "Anthropic (Claude)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.last7Days,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isLoadingUsage) "..." else "$${String.format("%.2f", anthropicUsage7Days)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (anthropicInputTokens7Days > 0 || anthropicOutputTokens7Days > 0) {
                                            Text(
                                                text = "In: ${String.format("%,d", anthropicInputTokens7Days)} | Out: ${String.format("%,d", anthropicOutputTokens7Days)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.last30Days,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isLoadingUsage) "..." else "$${String.format("%.2f", anthropicUsage30Days)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (anthropicInputTokens30Days > 0 || anthropicOutputTokens30Days > 0) {
                                            Text(
                                                text = "In: ${String.format("%,d", anthropicInputTokens30Days)} | Out: ${String.format("%,d", anthropicOutputTokens30Days)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = strings.requiresAdminKey,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Refresh button
                            EInkButton(
                                onClick = { viewModel.refreshUsageStats() },
                                enabled = !isLoadingUsage,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.refreshUsage)
                                }
                            }
                        }
                    }
                }
            }

            // Chat Font Size Section
            SettingsSection(title = strings.chatFontSize) {
                ChatFontSizeSelector(
                    currentSize = uiState.chatFontSize,
                    onSizeChange = viewModel::setChatFontSize,
                    previewLabel = strings.preview,
                    userMessage = strings.previewUserMessage,
                    assistantMessage = strings.previewAssistantMessage
                )
            }

            // Volume Button Scroll Section (E-Ink Optimization)
            SettingsSection(title = strings.volumeScrollTitle) {
                // Enable/Disable toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.volumeScrollEnable)
                        Text(
                            text = strings.volumeScrollDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.volumeButtonScrollEnabled,
                        onCheckedChange = viewModel::setVolumeButtonScrollEnabled
                    )
                }

                if (uiState.volumeButtonScrollEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Scroll distance slider
                    Text(
                        text = strings.volumeScrollDistance,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = uiState.volumeButtonScrollDistance,
                            onValueChange = { viewModel.setVolumeButtonScrollDistance(it) },
                            valueRange = 0.3f..1.0f,
                            steps = 6, // 30%, 40%, 50%, 60%, 70%, 80%, 90%, 100%
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${(uiState.volumeButtonScrollDistance * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.widthIn(min = 60.dp)
                        )
                    }
                }
            }

            // Share Feature Section (with hidden activation)
            var shareFeatureActivated by remember { mutableStateOf(uiState.shareEnabled) }

            SettingsSectionWithGesture(
                title = strings.shareFeature,
                onLongPress = { shareFeatureActivated = true }
            ) {
                if (shareFeatureActivated) {
                    // Enable/Disable toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.enableSharing)
                        Switch(
                            checked = uiState.shareEnabled,
                            onCheckedChange = viewModel::setShareEnabled
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Token
                    ApiKeyField(
                        label = strings.shareApiToken,
                        value = uiState.shareApiToken,
                        onValueChange = viewModel::setShareApiToken,
                        hint = "readtoken.app API token",
                        isActive = uiState.shareEnabled,
                        activeLabel = strings.active,
                        configuredLabel = strings.configured,
                        showLabel = strings.show,
                        hideLabel = strings.hide
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Expiration days slider
                    Text(
                        text = strings.expirationDays,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = uiState.shareExpirationDays.toFloat(),
                            onValueChange = { viewModel.setShareExpirationDays(it.toInt()) },
                            valueRange = 1f..30f,
                            steps = 28,  // 30 values (1-30) = 28 intermediate steps
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${uiState.shareExpirationDays}d",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.widthIn(min = 60.dp)
                        )
                    }
                } else {
                    // Show hint when not activated
                    Text(
                        text = strings.hiddenFeatureHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Whisper Models Section
            val downloadState by viewModel.downloadState.collectAsState()

            SettingsSection(title = strings.whisperModels) {
                com.music.sttnotes.data.stt.WhisperModel.entries.forEach { model ->
                    val isDownloaded = remember(model) { viewModel.isModelDownloaded(model) }

                    EInkCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = model.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isDownloaded) {
                                        Text(
                                            text = strings.modelDownloaded,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = strings.modelNotDownloaded,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (isDownloaded) {
                                        EInkButton(
                                            onClick = { viewModel.selectModel(model) },
                                            filled = false,
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text(strings.selectModel, fontSize = 12.sp)
                                        }
                                        EInkButton(
                                            onClick = { viewModel.deleteModel(model) },
                                            filled = false,
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text(strings.delete, fontSize = 12.sp)
                                        }
                                    } else {
                                        when (downloadState) {
                                            is com.music.sttnotes.data.stt.DownloadState.Downloading -> {
                                                val state = downloadState as com.music.sttnotes.data.stt.DownloadState.Downloading
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "${strings.downloading} ${(state.progress * 100).toInt()}%",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Text(
                                                        text = "${state.downloadedMB}/${state.totalMB} MB",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            else -> {
                                                EInkButton(
                                                    onClick = { viewModel.downloadModel(model) },
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Text(strings.download, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Info Section
            SettingsSection(title = strings.about) {
                Text(
                    text = strings.aboutText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsSectionWithGesture(
    title: String,
    onLongPress: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SttProviderSelector(
    selected: SttProvider,
    onSelect: (SttProvider) -> Unit
) {
    Column {
        SttProvider.entries.forEach { provider ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == provider,
                    onClick = { onSelect(provider) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when (provider) {
                            SttProvider.LOCAL -> "Local (Whisper.cpp)"
                            SttProvider.GROQ -> "Cloud (Groq Whisper)"
                            SttProvider.OPENAI -> "Cloud (OpenAI Whisper)"
                        }
                    )
                    Text(
                        text = when (provider) {
                            SttProvider.LOCAL -> "Hors-ligne, ~6s pour 16s audio"
                            SttProvider.GROQ -> "En ligne, ~1s, \$0.04/heure"
                            SttProvider.OPENAI -> "En ligne, \$0.006/min (\$0.36/h)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LlmProviderSelector(
    selected: LlmProvider,
    onSelect: (LlmProvider) -> Unit
) {
    Column {
        LlmProvider.entries.forEach { provider ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == provider,
                    onClick = { onSelect(provider) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when (provider) {
                            LlmProvider.NONE -> "Disabled"
                            LlmProvider.GROQ -> "Groq (Llama 3.3 70B)"
                            LlmProvider.OPENAI -> "OpenAI (GPT-5-mini)"
                            LlmProvider.XAI -> "xAI (Grok 4.1 Fast)"
                            LlmProvider.ANTHROPIC -> "Anthropic (Claude Haiku 4.5)"
                        }
                    )
                    Text(
                        text = when (provider) {
                            LlmProvider.NONE -> "Raw transcription"
                            LlmProvider.GROQ -> "\$0.05/1M input, \$0.08/1M output (cheapest)"
                            LlmProvider.OPENAI -> "\$0.25/1M input, \$2/1M output"
                            LlmProvider.XAI -> "\$0.20/1M input, \$0.50/1M output"
                            LlmProvider.ANTHROPIC -> "\$1/1M input, \$5/1M output"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SttLanguageSelector(
    selected: SttLanguage,
    onSelect: (SttLanguage) -> Unit
) {
    Column {
        SttLanguage.entries.forEach { language ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == language,
                    onClick = { onSelect(language) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = language.displayName)
            }
        }
    }
}

@Composable
private fun AppLanguageSelector(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Column {
        AppLanguage.entries.forEach { language ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == language,
                    onClick = { onSelect(language) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = language.displayName)
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    isActive: Boolean = false,
    activeLabel: String = "active",
    configuredLabel: String = "configured",
    showLabel: String = "Show",
    hideLabel: String = "Hide"
) {
    var visible by remember { mutableStateOf(false) }
    val isConfigured = value.isNotBlank()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                if (isActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (isConfigured) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = configuredLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        placeholder = { Text(hint) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) hideLabel else showLabel
                )
            }
        }
    )
}

@Composable
private fun ChatFontSizeSelector(
    currentSize: Float,
    onSizeChange: (Float) -> Unit,
    previewLabel: String = "Preview:",
    userMessage: String = "Hello, how are you?",
    assistantMessage: String = "I'm doing great, thanks!"
) {
    Column {
        // Slider with value display
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = currentSize,
                onValueChange = { onSizeChange(kotlin.math.round(it)) },
                valueRange = ApiConfig.MIN_CHAT_FONT_SIZE..ApiConfig.MAX_CHAT_FONT_SIZE,
                steps = 9,  // 10 values (10-20) = 9 intermediate steps for 1sp increments
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${currentSize.toInt()} sp",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.widthIn(min = 48.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preview
        Text(
            text = previewLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Preview bubbles
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // User message preview
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = userMessage,
                        fontSize = currentSize.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Assistant message preview
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = assistantMessage,
                        fontSize = currentSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
