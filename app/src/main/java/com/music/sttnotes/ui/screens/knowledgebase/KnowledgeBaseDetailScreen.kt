package com.music.sttnotes.ui.screens.knowledgebase

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.m3.Markdown
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.UndoSnackbar
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkWhite

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

    LaunchedEffect(folder, filename) {
        viewModel.loadFileContent(folder, filename)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = filename.removeSuffix(".md"),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    EInkIconButton(
                        onClick = {
                            // Commit pending deletion before navigating back
                            if (showUndoSnackbar) {
                                viewModel.deleteFile(folder, filename)
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
                    viewModel.deleteFile(folder, filename)
                    showUndoSnackbar = false
                    onNavigateBack()
                }
            )
        }
    }
}
