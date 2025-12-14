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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.einkMarkdownColors
import com.music.sttnotes.ui.components.einkMarkdownTypography
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.MarkdownToolbar
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
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val fileContent by viewModel.fileContent.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val fileTags by viewModel.fileTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val tagInput by viewModel.tagInput.collectAsState()
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showTagsSection by remember { mutableStateOf(false) }
    var currentFilename by remember { mutableStateOf(filename) }
    var renameError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Rich text editor state
    val richTextState = rememberRichTextState()

    // Keyboard detection
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible = imeInsets.getBottom(density) > 0

    LaunchedEffect(folder, filename) {
        viewModel.loadFileContent(folder, filename)
        currentFilename = filename
    }

    // Load content into RichTextState when fileContent changes
    LaunchedEffect(fileContent) {
        fileContent?.let { content ->
            richTextState.setMarkdown(content)
        }
    }

    // Auto-save on back navigation
    DisposableEffect(Unit) {
        onDispose {
            if (isEditMode) {
                viewModel.saveFileContent(folder, currentFilename, richTextState.toMarkdown())
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
                    EInkIconButton(
                        onClick = {
                            // Save content if in edit mode
                            if (isEditMode) {
                                viewModel.saveFileContent(folder, currentFilename, richTextState.toMarkdown())
                            }
                            // Commit pending deletion before navigating back
                            if (showUndoSnackbar) {
                                viewModel.deleteFile(folder, currentFilename)
                                showUndoSnackbar = false
                            }
                            viewModel.clearFileContent()
                            onNavigateBack()
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour"
                    )
                },
                actions = {
                    // Tags button
                    EInkIconButton(
                        onClick = { showTagsSection = !showTagsSection },
                        icon = Icons.Default.LocalOffer,
                        contentDescription = "Tags"
                    )
                    // Toggle Edit/Preview
                    EInkIconButton(
                        onClick = {
                            if (isEditMode) {
                                // Save before switching to preview
                                viewModel.saveFileContent(folder, currentFilename, richTextState.toMarkdown())
                            }
                            viewModel.toggleEditMode()
                        },
                        icon = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                        contentDescription = if (isEditMode) "Aperçu" else "Modifier"
                    )
                    // Delete button
                    EInkIconButton(
                        onClick = { showUndoSnackbar = true },
                        icon = Icons.Default.Delete,
                        contentDescription = "Supprimer"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                )
            )
        },
        containerColor = EInkWhite
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tags section (collapsible)
            if (showTagsSection) {
                TagsSection(
                    tags = fileTags,
                    allTags = allTags,
                    tagInput = tagInput,
                    onTagInputChange = viewModel::updateTagInput,
                    onAddTag = viewModel::addTag,
                    onRemoveTag = viewModel::removeTag,
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
                                // Edit mode: RichTextEditor
                                RichTextEditor(
                                    state = richTextState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    colors = RichTextEditorDefaults.richTextEditorColors(
                                        containerColor = EInkWhite,
                                        textColor = EInkBlack,
                                        cursorColor = EInkBlack
                                    ),
                                    placeholder = { Text("Commencez à écrire...", color = EInkGrayMedium) }
                                )
                            } else {
                                // Preview mode: Markdown renderer with text selection
                                SelectionContainer {
                                    Markdown(
                                        content = fileContent ?: "",
                                        colors = einkMarkdownColors(),
                                        typography = einkMarkdownTypography(),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp, vertical = 16.dp)
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }

                            // Toolbar above keyboard (only in edit mode when keyboard is visible)
                            if (isEditMode && isKeyboardVisible) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .imePadding()
                                ) {
                                    MarkdownToolbar(richTextState = richTextState)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Undo Snackbar for file deletion
    if (showUndoSnackbar) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
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
}

@Composable
private fun RenameDialog(
    currentName: String,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer le fichier") },
        text = {
            androidx.compose.foundation.layout.Column {
                EInkTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "Nouveau nom",
                    modifier = Modifier.fillMaxWidth()
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
        },
        confirmButton = {
            EInkButton(
                onClick = { onConfirm(newName) },
                filled = true,
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Renommer")
            }
        },
        dismissButton = {
            EInkButton(
                onClick = onDismiss,
                filled = false
            ) {
                Text("Annuler")
            }
        },
        containerColor = EInkWhite
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<String>,
    allTags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Current tags
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.forEach { tag ->
                    TagChip(
                        tag = tag,
                        onRemove = { onRemoveTag(tag) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

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
            it.lowercase().contains(tagInput.lowercase()) && !tags.contains(it)
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

@Composable
private fun TagChip(
    tag: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EInkBlack,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.bodySmall,
                color = EInkWhite
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                onClick = onRemove,
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
}
