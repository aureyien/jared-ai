package com.music.sttnotes.ui.screens.knowledgebase

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.ui.components.EInkChip
import com.music.sttnotes.ui.components.EInkDivider
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.PendingDeletion
import com.music.sttnotes.ui.components.UndoSnackbar
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeBaseScreen(
    onFileClick: (folder: String, filename: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val allFolders by viewModel.filteredFolders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagFilters by viewModel.selectedTagFilters.collectAsState()

    // Tag filter visibility
    var showTagFilter by remember { mutableStateOf(false) }

    // Undo deletion state for folders
    var pendingFolderDeletion by remember { mutableStateOf<PendingDeletion<String>?>(null) }

    // Filter out pending deletion folders from display
    val folders = remember(allFolders, pendingFolderDeletion) {
        allFolders.filter { folder -> pendingFolderDeletion?.item != folder.name }
    }

    // Handle system back button - commit pending deletion before leaving
    BackHandler {
        pendingFolderDeletion?.let { deletion ->
            viewModel.deleteFolder(deletion.item)
        }
        pendingFolderDeletion = null
        onNavigateBack()
    }

    // Refresh list when screen becomes visible
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadFolders()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Base", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    EInkIconButton(
                        onClick = {
                            // Commit pending deletion before navigating back
                            pendingFolderDeletion?.let { deletion ->
                                viewModel.deleteFolder(deletion.item)
                                pendingFolderDeletion = null
                            }
                            onNavigateBack()
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour"
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
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EInkLoadingIndicator(text = "Chargement...")
                }
            }
            folders.isEmpty() && searchQuery.isEmpty() -> {
                EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    // Search bar with tag filter button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EInkTextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            placeholder = "Rechercher...",
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = EInkBlack
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    EInkIconButton(
                                        onClick = { viewModel.onSearchQueryChange("") },
                                        icon = Icons.Default.Close,
                                        contentDescription = "Effacer"
                                    )
                                }
                            }
                        )
                        // Tag filter button (only show if there are tags)
                        if (allTags.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            EInkIconButton(
                                onClick = { showTagFilter = !showTagFilter },
                                icon = Icons.Default.LocalOffer,
                                contentDescription = "Filtrer par tags"
                            )
                        }
                    }

                    // Tag filter chips (show when toggled and tags exist)
                    if (showTagFilter && allTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allTags.forEach { tag ->
                                EInkChip(
                                    label = tag,
                                    selected = tag in selectedTagFilters,
                                    onClick = { viewModel.toggleTagFilter(tag) }
                                )
                            }
                        }
                    }

                    EInkDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    if (folders.isEmpty()) {
                        // No results
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Aucun resultat",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Essayez une autre recherche",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            folders.forEach { folder ->
                                item(key = "folder_${folder.name}") {
                                    FolderHeader(
                                        name = folder.name,
                                        fileCount = folder.files.size,
                                        isExpanded = folder.isExpanded,
                                        onClick = { viewModel.toggleFolder(folder.name) },
                                        onDelete = {
                                            // Commit previous pending deletion first
                                            pendingFolderDeletion?.let { previousDeletion ->
                                                viewModel.deleteFolder(previousDeletion.item)
                                            }
                                            // Set new pending deletion
                                            pendingFolderDeletion = PendingDeletion(
                                                item = folder.name,
                                                message = "Dossier supprimé"
                                            )
                                        }
                                    )
                                }
                                if (folder.isExpanded) {
                                    items(folder.files, key = { "file_${folder.name}_${it.file.name}" }) { filePreview ->
                                        FileItem(
                                            file = filePreview,
                                            folderName = folder.name,
                                            onClick = {
                                                // Commit any pending deletion before navigating
                                                pendingFolderDeletion?.let { deletion ->
                                                    viewModel.deleteFolder(deletion.item)
                                                }
                                                pendingFolderDeletion = null
                                                onFileClick(folder.name, filePreview.file.name)
                                            },
                                            onCopyContent = {
                                                viewModel.getFileContent(folder.name, filePreview.file.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Undo Snackbar for folder deletion
    pendingFolderDeletion?.let { deletion ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            UndoSnackbar(
                message = deletion.message,
                onUndo = {
                    pendingFolderDeletion = null
                },
                onTimeout = {
                    viewModel.deleteFolder(deletion.item)
                    pendingFolderDeletion = null
                }
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.LibraryBooks,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = EInkGrayMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Aucun fichier sauvegarde",
            style = MaterialTheme.typography.titleMedium,
            color = EInkBlack
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sauvegardez des reponses IA depuis le Chat",
            style = MaterialTheme.typography.bodyMedium,
            color = EInkGrayMedium
        )
    }
}

@Composable
private fun FolderHeader(
    name: String,
    fileCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    EInkCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = EInkBlack,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = EInkBlack
                )
                Text(
                    text = "$fileCount fichier${if (fileCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EInkGrayMedium
                )
            }

            // Context menu
            EInkIconButton(
                onClick = { showMenu = true },
                icon = Icons.Default.Delete,
                contentDescription = "Supprimer"
            )
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Supprimer le dossier") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }

            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Reduire" else "Etendre",
                tint = EInkBlack
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItem(
    file: FilePreview,
    folderName: String,
    onClick: () -> Unit,
    onCopyContent: () -> String?
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    EInkCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = EInkGrayMedium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = file.file.nameWithoutExtension,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EInkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatRelativeTime(file.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = EInkGrayMedium
                )
            }
            if (file.preview.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = file.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = EInkGrayMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Context menu for copy
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copier le contenu") },
                onClick = {
                    showMenu = false
                    val content = onCopyContent()
                    if (content != null) {
                        clipboardManager.setText(AnnotatedString(content))
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "maintenant"
        minutes < 60 -> "il y a ${minutes}min"
        hours < 24 -> "il y a ${hours}h"
        days < 2 -> "hier"
        days < 7 -> "il y a ${days}j"
        else -> {
            val format = SimpleDateFormat("d MMM", Locale.FRENCH)
            format.format(Date(timestamp))
        }
    }
}
