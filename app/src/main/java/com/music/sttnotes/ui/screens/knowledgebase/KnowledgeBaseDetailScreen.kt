package com.music.sttnotes.ui.screens.knowledgebase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import com.music.sttnotes.data.i18n.rememberStrings
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.m3.Markdown
import com.music.sttnotes.ui.components.convertCheckboxesToUnicode
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkChip
import com.music.sttnotes.ui.components.einkMarkdownColors
import com.music.sttnotes.ui.components.einkMarkdownComponents
import com.music.sttnotes.ui.components.einkMarkdownTypography
import com.music.sttnotes.ui.components.EInkFormModal
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.PlainTextMarkdownToolbar
import com.music.sttnotes.ui.components.ShareResultModal
import com.music.sttnotes.ui.components.UndoSnackbar
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeBaseDetailScreen(
    folder: String,
    filename: String,
    onNavigateBack: () -> Unit,
    onOpenParentFolder: (() -> Unit)? = null,
    onNavigateToHome: (() -> Unit)? = null,
    onManageTags: () -> Unit = {},
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val fileContent by viewModel.fileContent.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val fileTags by viewModel.fileTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val tagInput by viewModel.tagInput.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val shareEnabled by viewModel.shareEnabled.collectAsState(initial = false)
    val shareResult by viewModel.shareResult.collectAsState()
    val kbPreviewFontSize by viewModel.kbPreviewFontSize.collectAsState()
    val allFolders by viewModel.folders.collectAsState()
    val strings = rememberStrings()
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showTagsSection by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var currentFilename by remember { mutableStateOf(filename) }
    var renameError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Volume scroll support for detail view
    val scrollState = rememberScrollState()
    val uiPreferences = androidx.compose.ui.platform.LocalContext.current.let { context ->
        remember { dagger.hilt.android.EntryPointAccessors.fromApplication<UiPreferencesEntryPoint>(context.applicationContext).uiPreferences() }
    }
    val volumeScrollEnabled by uiPreferences.volumeButtonScrollEnabled.collectAsState(initial = false)
    val volumeScrollDistance by uiPreferences.volumeButtonScrollDistance.collectAsState(initial = 0.8f)

    // Plain text state for markdown editing (preserves all markdown syntax)
    var editText by remember { mutableStateOf(TextFieldValue("")) }

    // Keyboard detection
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible = imeInsets.getBottom(density) > 0

    // Volume handler setup (using density for viewport height)
    val volumeHandler = remember(scrollState, coroutineScope, volumeScrollDistance) {
        com.music.sttnotes.ui.components.createScrollStateVolumeHandler(
            state = scrollState,
            scope = coroutineScope,
            viewportHeightProvider = { with(density) { 800.dp.toPx().toInt() } }, // approximate viewport height
            scrollDistanceProvider = { volumeScrollDistance }
        )
    }

    // Register volume scroll handler with Activity
    val activity = androidx.compose.ui.platform.LocalContext.current as? com.music.sttnotes.MainActivity
    LaunchedEffect(volumeHandler, volumeScrollEnabled) {
        if (volumeScrollEnabled) {
            activity?.setVolumeScrollHandler(volumeHandler)
        } else {
            activity?.setVolumeScrollHandler(null)
        }
    }

    // Clean up handler when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            activity?.setVolumeScrollHandler(null)
        }
    }

    LaunchedEffect(folder, filename) {
        viewModel.loadFileContent(folder, filename)
        currentFilename = filename
    }

    // Load content into edit text when fileContent changes
    LaunchedEffect(fileContent) {
        fileContent?.let { content ->
            editText = TextFieldValue(content)
        }
    }

    // Auto-save on back navigation
    DisposableEffect(Unit) {
        onDispose {
            if (isEditMode) {
                viewModel.saveFileContent(folder, currentFilename, editText.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentFilename.removeSuffix(".md"),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    )
                },
                navigationIcon = {
                    // Always show single back button in top bar
                    EInkIconButton(
                        onClick = {
                            // Save content if in edit mode
                            if (isEditMode) {
                                viewModel.saveFileContent(folder, currentFilename, editText.text)
                            }
                            // Cancel pending deletion when navigating back manually
                            showUndoSnackbar = false
                            viewModel.clearFileContent()
                            onNavigateBack()
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour"
                    )
                },
                actions = {
                    // Save/Preview button when in edit mode
                    if (isEditMode) {
                        EInkIconButton(
                            onClick = {
                                viewModel.saveFileContent(folder, currentFilename, editText.text)
                                viewModel.toggleEditMode()
                            },
                            icon = Icons.Default.Save,
                            contentDescription = strings.preview
                        )
                    }
                    // Favorite button - standalone
                    EInkIconButton(
                        onClick = { viewModel.toggleFileFavorite(folder, filename) },
                        icon = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites
                    )
                    // Share button (conditionally visible)
                    if (shareEnabled && !isEditMode) {
                        EInkIconButton(
                            onClick = { viewModel.shareArticle(folder, filename) },
                            icon = Icons.Default.Share,
                            contentDescription = strings.shareArticle
                        )
                    }
                    // 3-dot menu for other actions
                    Box {
                        EInkIconButton(
                            onClick = { showActionMenu = true },
                            icon = Icons.Default.MoreVert,
                            contentDescription = strings.settings
                        )
                        DropdownMenu(
                            expanded = showActionMenu,
                            onDismissRequest = { showActionMenu = false }
                        ) {
                            // Tags option
                            DropdownMenuItem(
                                text = { Text(strings.tags) },
                                onClick = {
                                    showActionMenu = false
                                    onManageTags()
                                },
                                leadingIcon = { Icon(Icons.Default.LocalOffer, null) }
                            )
                            // Copy option
                            DropdownMenuItem(
                                text = { Text(strings.copyContent) },
                                onClick = {
                                    showActionMenu = false
                                    fileContent?.let { content ->
                                        clipboardManager.setText(AnnotatedString(content))
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                            )
                            // Edit/Preview toggle
                            DropdownMenuItem(
                                text = { Text(if (isEditMode) strings.preview else strings.edit) },
                                onClick = {
                                    showActionMenu = false
                                    if (isEditMode) {
                                        viewModel.saveFileContent(folder, currentFilename, editText.text)
                                    }
                                    viewModel.toggleEditMode()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                        null
                                    )
                                }
                            )
                            // Font size option for grid preview
                            DropdownMenuItem(
                                text = { Text("Preview Font Size") },
                                onClick = {
                                    showActionMenu = false
                                    showFontSizeDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.FormatSize, null) }
                            )
                            // Move to folder option
                            DropdownMenuItem(
                                text = { Text("Move to folder") },
                                onClick = {
                                    showActionMenu = false
                                    showMoveDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.DriveFileMove, null) }
                            )
                            // Delete option
                            DropdownMenuItem(
                                text = { Text(strings.delete) },
                                onClick = {
                                    showActionMenu = false
                                    showUndoSnackbar = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                )
            )
        },
        bottomBar = {
            // Show two-button navigation when opened from home
            if (onOpenParentFolder != null && onNavigateToHome != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = EInkWhite,
                    border = BorderStroke(1.dp, EInkBlack)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val leftButtonShape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = 24.dp,
                            bottomEnd = 0.dp
                        )
                        val rightButtonShape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 24.dp
                        )

                        // Open Folder button - LEFT
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable {
                                    if (isEditMode) {
                                        viewModel.saveFileContent(folder, currentFilename, editText.text)
                                    }
                                    // Cancel pending deletion when navigating to parent folder
                                    showUndoSnackbar = false
                                    viewModel.clearFileContent()
                                    onOpenParentFolder()
                                },
                            shape = leftButtonShape,
                            color = EInkBlack
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Open Folder",
                                    color = EInkWhite,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Close button - RIGHT
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable {
                                    if (isEditMode) {
                                        viewModel.saveFileContent(folder, currentFilename, editText.text)
                                    }
                                    // Cancel pending deletion when navigating to home
                                    showUndoSnackbar = false
                                    viewModel.clearFileContent()
                                    onNavigateToHome()
                                },
                            shape = rightButtonShape,
                            color = EInkWhite,
                            border = BorderStroke(1.dp, EInkBlack)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Close",
                                    color = EInkBlack,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = EInkWhite
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tags display (always visible if tags exist)
            if (fileTags.isNotEmpty()) {
                TagsDisplay(
                    tags = fileTags,
                    isEditMode = showTagsSection,
                    onRemoveTag = { tag ->
                        viewModel.removeTag(tag)
                        viewModel.saveFileContent(folder, currentFilename, editText.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Tag input section (only when tag icon clicked)
            if (showTagsSection) {
                TagInputSection(
                    tagInput = tagInput,
                    allTags = allTags,
                    currentTags = fileTags,
                    onTagInputChange = viewModel::updateTagInput,
                    onAddTag = { tag ->
                        viewModel.addTag(tag)
                        viewModel.saveFileContent(folder, currentFilename, editText.text)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when {
                    fileContent == null -> {
                        EInkLoadingIndicator(
                            text = "Chargement...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        // Editor or Preview based on mode
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isEditMode) {
                                // Edit mode: Plain TextField for markdown editing
                                CompositionLocalProvider(
                                    LocalTextSelectionColors provides TextSelectionColors(
                                        handleColor = Color(0xFF64B5F6),
                                        backgroundColor = Color(0xFF64B5F6).copy(alpha = 0.4f)
                                    )
                                ) {
                                    TextField(
                                        value = editText,
                                        onValueChange = { editText = it },
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
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        placeholder = { Text("Start writing...", color = EInkGrayMedium) }
                                    )
                                }
                            } else {
                                // Preview mode: Markdown renderer with text selection and checkbox support
                                // Convert grid preview font size (7-12sp) to content multiplier (0.778-1.333)
                                val fontMultiplier = kbPreviewFontSize / 9f // 9sp is baseline (1.0x)
                                SelectionContainer {
                                    Markdown(
                                        content = convertCheckboxesToUnicode(fileContent ?: ""),
                                        colors = einkMarkdownColors(),
                                        typography = einkMarkdownTypography(fontMultiplier),
                                        components = einkMarkdownComponents(),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp, vertical = 16.dp)
                                            .verticalScroll(scrollState)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            // Markdown toolbar - edge to edge, above keyboard
            if (isEditMode && isKeyboardVisible) {
                PlainTextMarkdownToolbar(
                    textFieldValue = editText,
                    onTextChange = { editText = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                )
            }
        }
    }

    // Undo Snackbar for file deletion
    if (showUndoSnackbar) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            UndoSnackbar(
                message = "Fichier supprimé",
                onUndo = {
                    showUndoSnackbar = false
                },
                onTimeout = {
                    viewModel.deleteFile(folder, currentFilename)
                    showUndoSnackbar = false
                    onNavigateBack()
                }
            )
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        RenameDialog(
            currentName = currentFilename.removeSuffix(".md"),
            error = renameError,
            onDismiss = {
                showRenameDialog = false
                renameError = null
            },
            onConfirm = { newName ->
                coroutineScope.launch {
                    val result = viewModel.renameFile(folder, currentFilename, newName)
                    result.fold(
                        onSuccess = { newFilename ->
                            currentFilename = newFilename
                            showRenameDialog = false
                            renameError = null
                        },
                        onFailure = { error ->
                            renameError = error.message
                        }
                    )
                }
            }
        )
    }

    // Font size dialog
    // Move to folder dialog
    if (showMoveDialog) {
        val folderNames = allFolders.map { it.name }.filter { it != folder }
        var selectedFolder by remember { mutableStateOf(folderNames.firstOrNull() ?: "") }
        var isMoving by remember { mutableStateOf(false) }

        EInkFormModal(
            onDismiss = { if (!isMoving) showMoveDialog = false },
            onConfirm = {
                if (selectedFolder.isNotBlank()) {
                    isMoving = true
                    coroutineScope.launch {
                        viewModel.moveFile(folder, currentFilename, selectedFolder).onSuccess {
                            showMoveDialog = false
                            onNavigateBack()
                        }.onFailure {
                            isMoving = false
                        }
                    }
                }
            },
            title = "Move to folder",
            confirmText = strings.confirm,
            dismissText = strings.cancel,
            confirmEnabled = selectedFolder.isNotBlank() && !isMoving
        ) {
            Column {
                Text(
                    text = "Select destination folder:",
                    style = MaterialTheme.typography.labelLarge,
                    color = EInkBlack
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    folderNames.forEach { folderName ->
                        EInkChip(
                            label = folderName,
                            selected = selectedFolder == folderName,
                            onClick = { selectedFolder = folderName }
                        )
                    }
                }
            }
        }
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = kbPreviewFontSize,
            onDismiss = { showFontSizeDialog = false },
            onSizeChange = { viewModel.setKbPreviewFontSize(it) }
        )
    }

    // Share result modal
    shareResult?.let { (_, response) ->
        ShareResultModal(
            shareResponse = response,
            onDismiss = { viewModel.clearShareResult() }
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    EInkFormModal(
        onDismiss = onDismiss,
        onConfirm = { onConfirm(newName) },
        title = "Renommer le fichier",
        confirmText = "Renommer",
        dismissText = "Annuler",
        confirmEnabled = newName.isNotBlank() && newName != currentName
    ) {
        EInkTextField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = "Nouveau nom",
            modifier = Modifier.fillMaxWidth(),
            showClearButton = true
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun FontSizeDialog(
    currentSize: Float,
    onDismiss: () -> Unit,
    onSizeChange: (Float) -> Unit
) {
    EInkFormModal(
        onDismiss = onDismiss,
        onConfirm = onDismiss,
        title = "Preview Font Size",
        confirmText = "Done",
        dismissText = "Cancel"
    ) {
        Text(
            "Adjust the font size for KB grid previews",
            style = MaterialTheme.typography.bodySmall,
            color = EInkGrayMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("7", style = MaterialTheme.typography.labelSmall, color = EInkBlack)
            Spacer(Modifier.width(8.dp))
            Slider(
                value = currentSize,
                onValueChange = onSizeChange,
                valueRange = 7f..12f,
                steps = 4, // 7, 8, 9, 10, 11, 12
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text("12", style = MaterialTheme.typography.labelSmall, color = EInkBlack)
        }

        Text(
            "Current: ${currentSize.toInt()}sp",
            style = MaterialTheme.typography.labelLarge,
            color = EInkBlack,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Displays tags as chips - always visible if tags exist
 * Shows delete button only in edit mode
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsDisplay(
    tags: List<String>,
    isEditMode: Boolean,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        tags.forEach { tag ->
            if (isEditMode) {
                // Editable tag with delete button
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EInkBlack
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = EInkWhite
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            onClick = { onRemoveTag(tag) },
                            shape = RoundedCornerShape(12.dp),
                            color = EInkWhite.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Supprimer",
                                tint = EInkWhite,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Read-only tag chip (harmonized with Chat styling)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EInkWhite,
                    border = BorderStroke(1.dp, EInkGrayMedium.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = EInkGrayMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tag input section - shown when tag icon is clicked
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagInputSection(
    tagInput: String,
    allTags: List<String>,
    currentTags: List<String>,
    onTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Add tag input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = onTagInputChange,
                placeholder = { Text("Ajouter un tag...", color = EInkGrayMedium) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (tagInput.isNotBlank()) onAddTag(tagInput) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EInkBlack,
                    unfocusedBorderColor = EInkGrayMedium,
                    focusedTextColor = EInkBlack,
                    unfocusedTextColor = EInkBlack,
                    cursorColor = EInkBlack
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            EInkIconButton(
                onClick = { if (tagInput.isNotBlank()) onAddTag(tagInput) },
                icon = Icons.Default.Add,
                contentDescription = "Ajouter"
            )
        }

        // Suggestions from existing tags
        val suggestions = allTags.filter {
            it.lowercase().contains(tagInput.lowercase()) && !currentTags.contains(it)
        }.take(5)

        if (suggestions.isNotEmpty() && tagInput.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                suggestions.forEach { suggestion ->
                    Surface(
                        onClick = { onAddTag(suggestion) },
                        shape = RoundedCornerShape(16.dp),
                        color = EInkWhite,
                        border = BorderStroke(1.dp, EInkGrayMedium)
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = EInkGrayMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
