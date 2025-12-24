package com.music.sttnotes.ui.screens.notes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.m3.Markdown
import com.music.sttnotes.ui.components.convertCheckboxesToUnicode
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.einkMarkdownColors
import com.music.sttnotes.ui.components.einkMarkdownComponents
import com.music.sttnotes.ui.components.einkMarkdownTypography
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.PlainTextMarkdownToolbar
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayLight
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.data.i18n.rememberStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    autoRecord: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val strings = rememberStrings()
    val note by viewModel.note.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    val tagInput by viewModel.tagInput.collectAsState()
    val isArchived by viewModel.isArchived.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val markdownContent by viewModel.markdownContent.collectAsState()

    // Local state for TextField that syncs with viewModel (using TextFieldValue for cursor position)
    var localContent by remember { mutableStateOf(TextFieldValue("")) }

    // Sync local content with viewModel content when it changes externally (e.g., from transcription)
    LaunchedEffect(markdownContent) {
        if (localContent.text != markdownContent) {
            // Preserve cursor position when updating from external source
            val cursorPosition = localContent.selection.start.coerceIn(0, markdownContent.length)
            localContent = TextFieldValue(
                text = markdownContent,
                selection = TextRange(cursorPosition)
            )
        }
    }

    var showPermissionDenied by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            showPermissionDenied = true
        }
    }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    // Initialize local content when note is loaded
    LaunchedEffect(noteId) {
        localContent = TextFieldValue(markdownContent)
    }

    // Auto-start recording if requested (quick dictate)
    LaunchedEffect(autoRecord) {
        if (autoRecord) {
            delay(500) // Wait for UI and whisper to be ready
            if (viewModel.hasAudioPermission()) {
                viewModel.startRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Auto-save on back
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveNote()
        }
    }

    // Detect keyboard visibility
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible = imeInsets.getBottom(density) > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNote()
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = EInkBlack
                        )
                    }
                },
                actions = {
                    // Tag button - show in edit mode
                    if (!isPreviewMode) {
                        IconButton(onClick = { showTagInput = !showTagInput }) {
                            Icon(
                                Icons.Default.LocalOffer,
                                contentDescription = strings.tags,
                                tint = if (showTagInput || note.tags.isNotEmpty()) EInkBlack else EInkGrayMedium
                            )
                        }
                    }
                    // Favorite button (only for existing notes)
                    if (noteId != null) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (note.isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                                tint = EInkBlack
                            )
                        }
                    }
                    // Archive button removed - use context menu from notes list instead
                    // Preview toggle with save icon when in edit mode
                    IconButton(onClick = { viewModel.togglePreviewMode() }) {
                        Icon(
                            if (isPreviewMode) Icons.Default.Edit else Icons.Default.Save,
                            contentDescription = if (isPreviewMode) strings.edit else strings.preview,
                            tint = EInkBlack
                        )
                    }
                    // Record button - only in edit mode (rightmost position)
                    if (!isPreviewMode && recordingState is RecordingState.Idle) {
                        IconButton(onClick = {
                            if (viewModel.hasAudioPermission()) {
                                viewModel.startRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = strings.recording,
                                tint = EInkBlack
                            )
                        }
                    }
                    // Stop recording button - show when recording (rightmost position)
                    if (recordingState is RecordingState.Recording) {
                        IconButton(onClick = { viewModel.stopRecording() }) {
                            StopRecordingIcon()
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                )
            )
        },
        containerColor = EInkWhite
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                    }
            ) {
            // Title - text in preview mode, input in edit mode
            if (isPreviewMode) {
                // Preview mode: show title as text
                Text(
                    text = note.title.ifEmpty { strings.untitled },
                    style = MaterialTheme.typography.headlineMedium,
                    color = EInkBlack,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Edit mode: show title input
                EInkTextField(
                    value = note.title,
                    onValueChange = viewModel::updateTitle,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = strings.title
                )
            }

            // Tag input - only visible when toggled in edit mode
            if (!isPreviewMode && showTagInput) {
                Spacer(modifier = Modifier.height(8.dp))

                // Input field for new tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EInkTextField(
                        value = tagInput,
                        onValueChange = viewModel::updateTagInput,
                        modifier = Modifier.weight(1f),
                        placeholder = strings.addTag
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.addTag() },
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .border(1.dp, EInkBlack, RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = strings.addTag, tint = EInkBlack)
                    }
                }

                // Show existing tags that are not already added to this note
                val availableTags = allTags.filter { it !in note.tags }
                if (availableTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.existingTags,
                        style = MaterialTheme.typography.labelSmall,
                        color = EInkGrayMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    viewModel.updateTagInput(tag)
                                    viewModel.addTag()
                                },
                                label = { Text(tag) },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = EInkWhite,
                                    labelColor = EInkBlack
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    borderColor = EInkGrayMedium,
                                    borderWidth = 1.dp,
                                    enabled = true,
                                    selected = false
                                )
                            )
                        }
                    }
                }
            }

            // Tags display - preview mode: read-only, edit mode: with remove buttons
            if (note.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    note.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { },
                            label = { Text(tag) },
                            trailingIcon = if (!isPreviewMode) {
                                {
                                    IconButton(
                                        onClick = { viewModel.removeTag(tag) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp),
                                            tint = EInkBlack
                                        )
                                    }
                                }
                            } else null,
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = EInkWhite,
                                labelColor = EInkBlack
                            ),
                            border = InputChipDefaults.inputChipBorder(
                                borderColor = EInkBlack,
                                borderWidth = 1.dp,
                                enabled = true,
                                selected = false
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content editor with toolbar overlay
            Box(
                modifier = Modifier.weight(1f)
            ) {
                // Editor or Preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(0.5.dp, EInkGrayMedium, RoundedCornerShape(6.dp))
                ) {
                    if (isPreviewMode) {
                        // Preview mode: render Markdown with checkbox support
                        SelectionContainer {
                            Markdown(
                                content = convertCheckboxesToUnicode(markdownContent),
                                colors = einkMarkdownColors(),
                                typography = einkMarkdownTypography(),
                                components = einkMarkdownComponents(),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    } else {
                        // Edit mode: Plain TextField for markdown editing
                        CompositionLocalProvider(
                            LocalTextSelectionColors provides TextSelectionColors(
                                handleColor = Color(0xFF64B5F6),
                                backgroundColor = Color(0xFF64B5F6).copy(alpha = 0.4f)
                            )
                        ) {
                            TextField(
                                value = localContent,
                                onValueChange = { newValue ->
                                    localContent = newValue
                                    viewModel.updateContent(newValue.text)
                                    viewModel.updateCursorPosition(newValue.selection.start)
                                },
                                modifier = Modifier.fillMaxSize(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = EInkWhite,
                                    unfocusedContainerColor = EInkWhite,
                                    focusedTextColor = EInkBlack,
                                    unfocusedTextColor = EInkBlack,
                                    cursorColor = EInkBlack,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                placeholder = { Text(strings.startWritingOrRecord, color = EInkGrayMedium) }
                            )
                        }
                    }
                }
            }

            // Recording status indicator - show when processing or error
            if (!isPreviewMode) {
                when (val state = recordingState) {
                    is RecordingState.Initializing -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        EInkLoadingIndicator(text = strings.initializing)
                    }
                    is RecordingState.Recording -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            formatDuration(state.duration),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    is RecordingState.Processing -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        EInkLoadingIndicator(text = strings.transcribing)
                    }
                    is RecordingState.Error -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = EInkBlack,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                state.message,
                                color = EInkBlack,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            TextButton(onClick = { viewModel.dismissError() }) {
                                Text(strings.dismiss, color = EInkBlack)
                            }
                        }
                    }
                    else -> {
                        // Idle state - show nothing or permission denied
                        if (showPermissionDenied) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                strings.micPermissionRequired,
                                color = EInkBlack,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
            }

            // Markdown toolbar - edge to edge, above keyboard
            if (!isPreviewMode && isKeyboardVisible) {
                PlainTextMarkdownToolbar(
                    textFieldValue = localContent,
                    onTextChange = { newValue ->
                        localContent = newValue
                        viewModel.updateContent(newValue.text)
                        viewModel.updateCursorPosition(newValue.selection.start)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

/**
 * Custom stop recording icon - circle with square inside (like media players)
 */
@Composable
private fun StopRecordingIcon() {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Draw outer circle
            drawCircle(
                color = androidx.compose.ui.graphics.Color.Black,
                radius = size.minDimension / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            // Draw inner square (stop symbol)
            val squareSize = size.minDimension * 0.45f
            drawRect(
                color = androidx.compose.ui.graphics.Color.Black,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = (size.width - squareSize) / 2,
                    y = (size.height - squareSize) / 2
                ),
                size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
            )
        }
    }
}
