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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.einkMarkdownColors
import com.music.sttnotes.ui.components.einkMarkdownTypography
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.MarkdownToolbar
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayLight
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    autoRecord: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    val tagInput by viewModel.tagInput.collectAsState()

    var showPermissionDenied by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
                title = {
                    Text(
                        if (noteId == null) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNote()
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EInkBlack
                        )
                    }
                },
                actions = {
                    // Preview toggle only (no save icon)
                    IconButton(onClick = { viewModel.togglePreviewMode() }) {
                        Icon(
                            if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isPreviewMode) "Edit" else "Preview",
                            tint = EInkBlack
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
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
                    text = note.title.ifEmpty { "Sans titre" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = EInkBlack,
                    modifier = Modifier.fillMaxWidth()
                )
                // Tags displayed below title in preview mode
                if (note.tags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(note.tags) { tag ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(tag) },
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
            } else {
                // Edit mode: show title input with tag button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EInkTextField(
                        value = note.title,
                        onValueChange = viewModel::updateTitle,
                        modifier = Modifier.weight(1f),
                        placeholder = "Title"
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { showTagInput = !showTagInput },
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .border(
                                1.dp,
                                if (showTagInput || note.tags.isNotEmpty()) EInkBlack else EInkGrayMedium,
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = "Tags",
                            tint = if (showTagInput || note.tags.isNotEmpty()) EInkBlack else EInkGrayMedium
                        )
                    }
                }

                // Tag input - only visible when toggled in edit mode
                if (showTagInput) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            placeholder = "Add tag..."
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.addTag() },
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .border(1.dp, EInkBlack, RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add tag", tint = EInkBlack)
                        }
                    }
                }

                // Tags display in edit mode (with remove buttons)
                if (note.tags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(note.tags) { tag ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(tag) },
                                trailingIcon = {
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
                                },
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content editor with toolbar overlay
            // imePadding on edit mode so content scrolls up when keyboard shows
            Box(
                modifier = Modifier.weight(1f).then(if (!isPreviewMode) Modifier.imePadding() else Modifier)
            ) {
                // Editor or Preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isPreviewMode) Modifier.border(1.dp, EInkBlack, RoundedCornerShape(4.dp))
                            else Modifier
                        )
                ) {
                    if (isPreviewMode) {
                        // Preview mode: render Markdown
                        SelectionContainer {
                            Markdown(
                                content = viewModel.richTextState.toMarkdown(),
                                colors = einkMarkdownColors(),
                                typography = einkMarkdownTypography(),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    } else {
                        // Edit mode: RichTextEditor
                        RichTextEditor(
                            state = viewModel.richTextState,
                            modifier = Modifier.fillMaxSize(),
                            colors = RichTextEditorDefaults.richTextEditorColors(
                                containerColor = EInkWhite,
                                textColor = EInkBlack,
                                cursorColor = EInkBlack
                            ),
                            placeholder = { Text("Start writing or record voice...", color = EInkGrayMedium) }
                        )
                    }
                }

                // Markdown toolbar - visible only when keyboard is shown in edit mode
                if (!isPreviewMode && isKeyboardVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        MarkdownToolbar(richTextState = viewModel.richTextState)
                    }
                }
            }

            // Recording controls - hide in preview mode and when keyboard is visible
            if (!isPreviewMode && !isKeyboardVisible) {
                Spacer(modifier = Modifier.height(16.dp))
                RecordingControls(
                    recordingState = recordingState,
                    onStartRecording = {
                        if (viewModel.hasAudioPermission()) {
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopRecording = { viewModel.stopRecording() },
                    onCancelRecording = { viewModel.cancelRecording() },
                    onDismissError = { viewModel.dismissError() }
                )

                if (showPermissionDenied) {
                    Text(
                        "Microphone permission required",
                        color = EInkBlack,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingControls(
    recordingState: RecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (recordingState) {
            is RecordingState.Idle -> {
                // E-ink styled record button
                Button(
                    onClick = onStartRecording,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EInkWhite,
                        contentColor = EInkBlack
                    ),
                    border = BorderStroke(2.dp, EInkBlack),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Start recording", modifier = Modifier.size(28.dp))
                }
                Text(
                    "Tap to record",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            is RecordingState.Initializing -> {
                EInkLoadingIndicator(text = "Initializing...")
            }
            is RecordingState.Recording -> {
                // No animation for e-ink - static visual with border
                Text(
                    formatDuration(recordingState.duration),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Stop button with static border indicator (no pulse animation)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(3.dp, EInkBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStopRecording,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EInkBlack,
                            contentColor = EInkWhite
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(28.dp))
                    }
                }
            }
            is RecordingState.Processing -> {
                EInkLoadingIndicator(text = "Transcribing...")
            }
            is RecordingState.Error -> {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = EInkBlack,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    recordingState.message,
                    color = EInkBlack,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                TextButton(onClick = onDismissError) {
                    Text("Dismiss", color = EInkBlack)
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
