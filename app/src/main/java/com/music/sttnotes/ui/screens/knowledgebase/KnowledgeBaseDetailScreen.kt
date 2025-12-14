package com.music.sttnotes.ui.screens.knowledgebase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.m3.Markdown
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.UndoSnackbar
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseDetailScreen(
    folder: String,
    filename: String,
    onNavigateBack: () -> Unit,
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val fileContent by viewModel.fileContent.collectAsState()
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentFilename by remember { mutableStateOf(filename) }
    var renameError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(folder, filename) {
        viewModel.loadFileContent(folder, filename)
        currentFilename = filename
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
        when {
            fileContent == null -> {
                EInkLoadingIndicator(
                    text = "Chargement...",
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            else -> {
                // Render markdown content with proper formatting and text selection
                SelectionContainer {
                    Markdown(
                        content = fileContent ?: "",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    )
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
