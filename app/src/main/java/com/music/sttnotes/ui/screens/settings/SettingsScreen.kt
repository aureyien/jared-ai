package com.music.sttnotes.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.sttnotes.data.api.ApiConfig
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.SttProvider
import com.music.sttnotes.data.stt.SttLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
            // STT Provider Section
            SettingsSection(title = "Transcription (STT)") {
                SttProviderSelector(
                    selected = uiState.sttProvider,
                    onSelect = viewModel::setSttProvider
                )
            }

            // STT Language Section
            SettingsSection(title = "Langue de transcription") {
                SttLanguageSelector(
                    selected = uiState.sttLanguage,
                    onSelect = viewModel::setSttLanguage
                )
            }

            // Chat Font Size Section
            SettingsSection(title = "Taille de police du chat") {
                ChatFontSizeSelector(
                    currentSize = uiState.chatFontSize,
                    onSizeChange = viewModel::setChatFontSize
                )
            }

            // LLM Provider Section
            SettingsSection(title = "Traitement LLM") {
                LlmProviderSelector(
                    selected = uiState.llmProvider,
                    onSelect = viewModel::setLlmProvider
                )
            }

            // API Keys Section - Always visible to configure all keys
            SettingsSection(title = "Clés API") {
                // Groq - used for STT and LLM
                ApiKeyField(
                    label = "Clé API Groq",
                    value = uiState.groqApiKey,
                    onValueChange = viewModel::setGroqApiKey,
                    hint = "Gratuit: console.groq.com",
                    isActive = uiState.sttProvider == SttProvider.GROQ || uiState.llmProvider == LlmProvider.GROQ
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OpenAI - used for STT and LLM
                ApiKeyField(
                    label = "Clé API OpenAI",
                    value = uiState.openaiApiKey,
                    onValueChange = viewModel::setOpenaiApiKey,
                    hint = "platform.openai.com",
                    isActive = uiState.sttProvider == SttProvider.OPENAI || uiState.llmProvider == LlmProvider.OPENAI
                )

                Spacer(modifier = Modifier.height(12.dp))

                // xAI - LLM only
                ApiKeyField(
                    label = "Clé API xAI",
                    value = uiState.xaiApiKey,
                    onValueChange = viewModel::setXaiApiKey,
                    hint = "console.x.ai",
                    isActive = uiState.llmProvider == LlmProvider.XAI
                )
            }

            // LLM System Prompt
            if (uiState.llmProvider != LlmProvider.NONE) {
                SettingsSection(title = "Prompt système LLM") {
                    OutlinedTextField(
                        value = uiState.llmSystemPrompt,
                        onValueChange = viewModel::setLlmSystemPrompt,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        label = { Text("Instructions pour le LLM") }
                    )
                }
            }

            // Info Section
            SettingsSection(title = "À propos") {
                Text(
                    text = """
                        • Local: Whisper.cpp (hors-ligne, modèle inclus)
                        • Groq: Whisper v3 Turbo (gratuit 8h/jour)
                        • LLM: Formate et améliore les transcriptions
                    """.trimIndent(),
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
                            SttProvider.GROQ -> "En ligne, ~1s, gratuit 8h/jour"
                            SttProvider.OPENAI -> "En ligne, \$0.006/min"
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
                            LlmProvider.NONE -> "Désactivé"
                            LlmProvider.GROQ -> "Groq (Llama 3.3 70B)"
                            LlmProvider.OPENAI -> "OpenAI (GPT-5-mini)"
                            LlmProvider.XAI -> "xAI (Grok 4.1 Fast)"
                        }
                    )
                    Text(
                        text = when (provider) {
                            LlmProvider.NONE -> "Transcription brute"
                            LlmProvider.GROQ -> "Gratuit, très rapide"
                            LlmProvider.OPENAI -> "\$0.25/1M input, \$2/1M output"
                            LlmProvider.XAI -> "\$0.20/1M input, \$0.50/1M output"
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
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    isActive: Boolean = false
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
                        text = "actif",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (isConfigured) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "configure",
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
                    contentDescription = if (visible) "Masquer" else "Afficher"
                )
            }
        }
    )
}

@Composable
private fun ChatFontSizeSelector(
    currentSize: Float,
    onSizeChange: (Float) -> Unit
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
            text = "Aperçu:",
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
                        text = "Bonjour, comment ça va ?",
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
                        text = "Je vais très bien, merci !",
                        fontSize = currentSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
