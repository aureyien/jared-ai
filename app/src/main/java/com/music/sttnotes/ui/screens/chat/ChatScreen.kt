package com.music.sttnotes.ui.screens.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.displayName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkChip
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.UndoSnackbar
import com.music.sttnotes.ui.components.chatMarkdownTypography
import com.music.sttnotes.ui.components.einkMarkdownColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayLight
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.data.i18n.rememberStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String? = null,
    startRecording: Boolean = false,
    onNavigateBack: () -> Unit,
    onManageTags: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val whisperReady by viewModel.whisperReady.collectAsState()
    val conversationTitle by viewModel.conversationTitle.collectAsState()
    val existingFolders by viewModel.existingFolders.collectAsState()
    val currentLlmProvider by viewModel.currentLlmProvider.collectAsState()
    val actualConversationId by viewModel.currentConversationId.collectAsState()
    val availableLlmProviders by viewModel.availableLlmProviders.collectAsState()
    val chatFontSize by viewModel.chatFontSize.collectAsState()
    val isEphemeral by viewModel.isEphemeral.collectAsState()

    val strings = rememberStrings()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showPermissionDenied by remember { mutableStateOf(false) }
    var autoRecordTriggered by remember { mutableStateOf(false) }
    var showLlmSelector by remember { mutableStateOf(false) }

    // Scroll FAB: show when there's content to scroll to
    val canScrollUp by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollDown by remember {
        derivedStateOf {
            messages.size > 3 && listState.firstVisibleItemIndex < messages.size - 3
        }
    }
    // Show FAB when there's somewhere to scroll
    val showScrollFab by remember {
        derivedStateOf { canScrollUp || canScrollDown }
    }
    // Direction: down arrow if can scroll down, up arrow if at bottom
    val scrollDirection by remember {
        derivedStateOf { if (canScrollDown) "down" else "up" }
    }

    // Save dialog state
    var messageToSave by remember { mutableStateOf<UiChatMessage?>(null) }

    // Rename dialog state
    var showRenameDialog by remember { mutableStateOf(false) }

    // Clear chat undo state (store messages before clear)
    var pendingClearMessages by remember { mutableStateOf<List<UiChatMessage>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            showPermissionDenied = true
        }
    }

    // Auto-start recording if requested via long press
    LaunchedEffect(startRecording) {
        if (startRecording && !autoRecordTriggered) {
            autoRecordTriggered = true
            if (viewModel.hasPermission()) {
                viewModel.startRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Auto-scroll to bottom when new message or on initial load (no animation for e-ink)
    var isInitialLoad by remember { mutableStateOf(true) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (isInitialLoad) {
                // Use a small delay to ensure list is composed
                kotlinx.coroutines.delay(100)
                isInitialLoad = false
            }
            // Instant scroll for all cases (better for e-ink displays)
            listState.scrollToItem(messages.size - 1)
        }
    }

    // Refresh folders when opening dialog
    LaunchedEffect(messageToSave) {
        if (messageToSave != null) {
            viewModel.refreshFolders()
        }
    }

    // Full-screen layout without TopAppBar for maximum real estate
    Scaffold(
        containerColor = EInkWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pinned header (stays fixed at top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = EInkBlack
                    )
                }
                Text(
                    text = conversationTitle.ifEmpty { strings.chat },
                    style = MaterialTheme.typography.titleMedium,
                    color = EInkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showRenameDialog = true }
                )
                // Ephemeral toggle (only show when messages are empty - new conversation)
                if (messages.isEmpty()) {
                    IconButton(onClick = { viewModel.toggleEphemeral() }) {
                        Icon(
                            if (isEphemeral) Icons.Default.CloudOff else Icons.Default.Cloud,
                            contentDescription = if (isEphemeral) "Mode ephemere actif" else "Mode normal",
                            tint = if (isEphemeral) EInkBlack else EInkGrayMedium,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                // LLM selector (only show if there are available providers)
                if (availableLlmProviders.isNotEmpty()) {
                    Box {
                        Text(
                            text = currentLlmProvider.displayName(),
                            style = MaterialTheme.typography.labelMedium,
                            color = EInkGrayMedium,
                            modifier = Modifier
                                .clickable { showLlmSelector = true }
                                .border(1.dp, EInkGrayLight, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        DropdownMenu(
                            expanded = showLlmSelector,
                            onDismissRequest = { showLlmSelector = false }
                        ) {
                            availableLlmProviders.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.displayName()) },
                                    onClick = {
                                        viewModel.setLlmProvider(provider)
                                        showLlmSelector = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Tag manager icon
                EInkIconButton(
                    onClick = {
                        actualConversationId?.let { onManageTags(it) }
                    },
                    icon = Icons.Default.LocalOffer,
                    contentDescription = strings.manageTags
                )
            }

            // Messages list with scroll FABs
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                if (messages.isEmpty()) {
                    item(contentType = "empty_state") {
                        EmptyState()
                    }
                }
                items(
                    items = messages,
                    key = { it.id },
                    contentType = { it.role } // Different types for user vs assistant messages
                ) { message ->
                    ChatBubble(
                        message = message,
                        fontSize = chatFontSize,
                        onSaveClick = if (message.role == "assistant" && message.content.isNotBlank()) {
                            { messageToSave = message }
                        } else null
                    )
                }
                // Loading indicator
                if (chatState is ChatState.SendingToLlm) {
                    item(contentType = "loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            EInkLoadingIndicator(text = strings.thinking)
                        }
                    }
                }
            }

                // Single scroll FAB - no animation for e-ink displays
                if (showScrollFab) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                if (scrollDirection == "down") {
                                    listState.scrollToItem(messages.size)
                                } else {
                                    listState.scrollToItem(0)
                                }
                            }
                        },
                        containerColor = EInkBlack,
                        contentColor = EInkWhite,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            if (scrollDirection == "down") Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (scrollDirection == "down") "Scroll to bottom" else "Scroll to top"
                        )
                    }
                }
            }

            // Status bar - shows all processing states for e-ink visibility
            when (val state = chatState) {
                is ChatState.Recording -> {
                    StatusBar(text = strings.recording, showMic = true)
                }
                is ChatState.Transcribing -> {
                    StatusBar(text = strings.transcribing, showMic = false)
                }
                is ChatState.SendingToLlm -> {
                    // Status bar removed - EInkLoadingIndicator "Reflexion..." already shows in chat
                }
                is ChatState.Error -> {
                    StatusBar(text = state.message, isError = true, onDismiss = { viewModel.dismissError() })
                }
                else -> {}
            }

            if (showPermissionDenied) {
                Text(
                    strings.micPermissionRequired,
                    color = EInkBlack,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Input area
            InputArea(
                inputText = inputText,
                onInputChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onMicClick = {
                    if (chatState is ChatState.Recording) {
                        viewModel.stopRecording()
                    } else {
                        if (viewModel.hasAudioPermission()) {
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                isRecording = chatState is ChatState.Recording,
                isLoading = chatState is ChatState.Transcribing || chatState is ChatState.SendingToLlm,
                whisperReady = whisperReady
            )
        }
    }

    // Save Response Dialog
    messageToSave?.let { message ->
        SaveResponseDialog(
            message = message,
            existingFolders = existingFolders,
            onSave = { filename, folder ->
                viewModel.saveResponseToFile(message.id, filename, folder)
                messageToSave = null
            },
            onDismiss = { messageToSave = null }
        )
    }

    // Undo Snackbar for clear chat
    pendingClearMessages?.let { clearedMessages ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            UndoSnackbar(
                message = strings.conversationCleared,
                onUndo = {
                    viewModel.restoreMessages(clearedMessages)
                    pendingClearMessages = null
                },
                onTimeout = {
                    pendingClearMessages = null
                }
            )
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        RenameConversationDialog(
            currentTitle = conversationTitle,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle ->
                viewModel.renameConversation(newTitle)
                showRenameDialog = false
            }
        )
    }
}

@Composable
private fun RenameConversationDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = rememberStrings()
    var newTitle by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.renameConversation) },
        text = {
            com.music.sttnotes.ui.components.EInkTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                placeholder = strings.newTitle,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newTitle) },
                enabled = newTitle.isNotBlank() && newTitle != currentTitle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EInkBlack,
                    contentColor = EInkWhite
                )
            ) {
                Text(strings.rename)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EInkWhite,
                    contentColor = EInkBlack
                ),
                border = BorderStroke(1.dp, EInkBlack)
            ) {
                Text(strings.cancel)
            }
        },
        containerColor = EInkWhite
    )
}

@Composable
private fun EmptyState() {
    val strings = rememberStrings()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings.startConversation,
            style = MaterialTheme.typography.titleMedium,
            color = EInkGrayMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.typeOrDictate,
            style = MaterialTheme.typography.bodyMedium,
            color = EInkGrayMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatBubble(
    message: UiChatMessage,
    fontSize: Float = 14f,
    onSaveClick: (() -> Unit)? = null
) {
    val strings = rememberStrings()
    val isUser = message.role == "user"
    val isSaved = message.savedToFile != null
    val cornerRadius = 8.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (!isUser) 8.dp else 0.dp),  // Extra margin above assistant responses
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (isUser) Modifier.widthIn(max = 300.dp)
                    else Modifier.fillMaxWidth()
                )
                .background(
                    if (isUser) EInkBlack else EInkWhite,
                    RoundedCornerShape(cornerRadius)
                )
                .border(
                    1.dp,
                    EInkBlack,
                    RoundedCornerShape(cornerRadius)
                )
                .padding(12.dp)
        ) {
            // SelectionContainer allows text selection within the bubble
            SelectionContainer {
                if (isUser) {
                    Text(
                        text = message.content,
                        color = EInkWhite,
                        fontSize = fontSize.sp
                    )
                } else {
                    Markdown(
                        content = message.content,
                        colors = einkMarkdownColors(),
                        typography = chatMarkdownTypography(baseFontSize = fontSize),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Bottom row with cloud indicator, copy button, and save button
        val clipboardManager = LocalClipboardManager.current

        Row(
            modifier = Modifier.padding(top = 6.dp, end = 4.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Cloud indicator with processing time
            if (message.isCloud && message.processingTimeMs != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = "Cloud",
                        tint = EInkGrayMedium,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "%.1fs".format(message.processingTimeMs / 1000f),
                        style = MaterialTheme.typography.labelSmall,
                        color = EInkGrayMedium
                    )
                }
            }

            // Copy button with thin border
            if (message.content.isNotBlank()) {
                MiniIconButton(
                    icon = Icons.Default.ContentCopy,
                    contentDescription = strings.copy,
                    onClick = { clipboardManager.setText(AnnotatedString(message.content)) }
                )
            }

            // Save button with thin border
            if (onSaveClick != null) {
                MiniIconButton(
                    icon = if (isSaved) Icons.Default.Check else Icons.Default.Save,
                    contentDescription = if (isSaved) strings.saved else strings.save,
                    tint = if (isSaved) EInkBlack else EInkGrayMedium,
                    onClick = onSaveClick
                )
            }
        }
    }
}

/**
 * Mini icon button with thin border for action icons
 */
@Composable
private fun MiniIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = EInkBlack,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 0.5.dp,
                color = EInkGrayMedium,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaveResponseDialog(
    message: UiChatMessage,
    existingFolders: List<String>,
    onSave: (filename: String, folder: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = rememberStrings()
    // Generate default filename from content
    val defaultFilename = message.content
        .take(40)
        .replace(Regex("[^a-zA-Z0-9\\s]"), "")
        .trim()
        .replace(Regex("\\s+"), "-")
        .lowercase()
        .ifEmpty { "response" }

    var filename by remember { mutableStateOf(defaultFilename) }
    var selectedFolder by remember { mutableStateOf(existingFolders.firstOrNull() ?: "") }
    var showNewFolderInput by remember { mutableStateOf(existingFolders.isEmpty()) }
    var newFolderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.saveResponse) },
        text = {
            Column {
                // Filename input
                Text(
                    text = "${strings.filename}:",
                    style = MaterialTheme.typography.labelLarge,
                    color = EInkBlack
                )
                Spacer(Modifier.height(4.dp))
                EInkTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    placeholder = strings.filename,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "${strings.folder}:",
                    style = MaterialTheme.typography.labelLarge,
                    color = EInkBlack
                )
                Spacer(Modifier.height(8.dp))

                // Folder selection with chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // New folder chip
                    EInkChip(
                        label = strings.newFolder,
                        selected = showNewFolderInput,
                        onClick = {
                            showNewFolderInput = true
                            selectedFolder = ""
                        }
                    )

                    // Existing folders
                    existingFolders.forEach { folder ->
                        EInkChip(
                            label = folder,
                            selected = selectedFolder == folder && !showNewFolderInput,
                            onClick = {
                                selectedFolder = folder
                                showNewFolderInput = false
                            }
                        )
                    }
                }

                // New folder input
                if (showNewFolderInput) {
                    Spacer(Modifier.height(8.dp))
                    EInkTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = strings.newFolderName,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val finalFolder = if (showNewFolderInput) newFolderName else selectedFolder
            EInkButton(
                onClick = { onSave(filename, finalFolder) },
                filled = true,
                enabled = filename.isNotBlank() && finalFolder.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            EInkButton(
                onClick = onDismiss,
                filled = false
            ) {
                Text(strings.cancel)
            }
        },
        containerColor = EInkWhite
    )
}

@Composable
private fun StatusBar(
    text: String,
    showMic: Boolean = false,
    isError: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isError) EInkGrayLight else EInkWhite)
            .border(1.dp, EInkBlack)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showMic) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = EInkBlack,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = EInkBlack,
            modifier = Modifier.weight(1f)
        )
        if (onDismiss != null) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Text("OK", color = EInkBlack)
            }
        }
    }
}

@Composable
private fun InputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isRecording: Boolean,
    isLoading: Boolean,
    whisperReady: Boolean
) {
    // Border radius on bottom-left corner to match Palma 2 Pro screen curve
    val textFieldShape = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 8.dp,
        bottomStart = 24.dp,  // Wider curve for Palma 2 Pro screen corner
        bottomEnd = 8.dp
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Horizontal divider above input area
        HorizontalDivider(
            thickness = 1.dp,
            color = EInkGrayMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EInkWhite)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Text input with custom border radius
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message...", color = EInkGrayMedium) },
            shape = textFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EInkBlack,
                unfocusedBorderColor = EInkGrayMedium,
                focusedTextColor = EInkBlack,
                unfocusedTextColor = EInkBlack,
                cursorColor = EInkBlack
            ),
            maxLines = 4,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Mic button
        Button(
            onClick = onMicClick,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) EInkBlack else EInkWhite,
                contentColor = if (isRecording) EInkWhite else EInkBlack
            ),
            border = BorderStroke(2.dp, EInkBlack),
            contentPadding = PaddingValues(0.dp),
            enabled = whisperReady && !isLoading
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Dicter",
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Send button
        Button(
            onClick = onSend,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = EInkBlack,
                contentColor = EInkWhite
            ),
            contentPadding = PaddingValues(0.dp),
            enabled = inputText.isNotBlank() && !isLoading
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Envoyer",
                modifier = Modifier.size(24.dp)
            )
        }
        }
    }
}
