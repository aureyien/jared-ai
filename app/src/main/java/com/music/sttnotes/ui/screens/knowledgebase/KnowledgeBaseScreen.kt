package com.music.sttnotes.ui.screens.knowledgebase

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.ui.components.EInkChip
import com.music.sttnotes.ui.components.EInkDivider
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkKBBottomActionBar
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.PendingDeletion
import com.music.sttnotes.ui.components.UndoButton
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.data.i18n.rememberStrings

private enum class TagAction {
    REMOVE_FROM_ALL,
    DELETE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeBaseScreen(
    onFolderClick: (folderName: String) -> Unit,
    onNavigateBack: () -> Unit,
    onNewNote: () -> Unit,
    onNewNoteWithRecording: () -> Unit,
    onNewChat: () -> Unit,
    onNewChatWithRecording: () -> Unit,
    onManageTags: () -> Unit,
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val strings = rememberStrings()
    val allFolders by viewModel.filteredFolders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagFilters by viewModel.selectedTagFilters.collectAsState()
    val isLlmConfigured by viewModel.isLlmConfigured.collectAsState()

    // Tag filter visibility
    var showTagFilter by remember { mutableStateOf(false) }

    // Undo deletion state for folders
    var pendingFolderDeletion by remember { mutableStateOf<PendingDeletion<String>?>(null) }

    // Rename dialog state
    var showRenameFolderDialog by remember { mutableStateOf<String?>(null) }

    // Tag deletion state
    var tagToDelete by remember { mutableStateOf<String?>(null) }

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
                title = { Text(strings.kbTitle, style = MaterialTheme.typography.headlineSmall) },
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
                        contentDescription = strings.back
                    )
                },
                actions = {
                    // Undo button in TopAppBar
                    pendingFolderDeletion?.let { deletion ->
                        UndoButton(
                            onUndo = { pendingFolderDeletion = null },
                            onTimeout = {
                                viewModel.deleteFolder(deletion.item)
                                pendingFolderDeletion = null
                            },
                            itemKey = deletion.item // Restart countdown when different folder deleted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                )
            )
        },
        bottomBar = {
            EInkKBBottomActionBar(
                onHome = {
                    // Commit pending deletion before navigating back
                    pendingFolderDeletion?.let { deletion ->
                        viewModel.deleteFolder(deletion.item)
                    }
                    pendingFolderDeletion = null
                    onNavigateBack()
                },
                onChat = onNewChat,
                onChatLongPress = onNewChatWithRecording,
                onAddNote = onNewNote,
                onAddNoteWithRecording = onNewNoteWithRecording,
                showChat = isLlmConfigured
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
                    EInkLoadingIndicator(text = strings.loading)
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
                            placeholder = strings.searchPlaceholder,
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
                                        contentDescription = strings.clear
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
                                contentDescription = strings.filterByTags,
                                onLongClick = onManageTags
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
                                    onClick = { viewModel.toggleTagFilter(tag) },
                                    onLongClick = { tagToDelete = tag }
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
                                text = strings.noResults,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = strings.tryAnotherSearch,
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
                            items(folders, key = { "folder_${it.name}" }) { folder ->
                                FolderCard(
                                    name = folder.name,
                                    fileCount = folder.files.size,
                                    onClick = {
                                        // Commit any pending deletion before navigating
                                        pendingFolderDeletion?.let { deletion ->
                                            viewModel.deleteFolder(deletion.item)
                                        }
                                        pendingFolderDeletion = null
                                        onFolderClick(folder.name)
                                    },
                                    onRename = { showRenameFolderDialog = folder.name },
                                    onDelete = {
                                        // Commit previous pending deletion first
                                        pendingFolderDeletion?.let { previousDeletion ->
                                            viewModel.deleteFolder(previousDeletion.item)
                                        }
                                        // Set new pending deletion
                                        pendingFolderDeletion = PendingDeletion(
                                            item = folder.name,
                                            message = strings.folderDeleted
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Folder Dialog
    showRenameFolderDialog?.let { folderName ->
        var newName by remember { mutableStateOf(folderName) }
        var isRenaming by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { if (!isRenaming) showRenameFolderDialog = null },
            title = { Text(strings.renameFolder) },
            text = {
                EInkTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = strings.newFolderName,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                EInkButton(
                    onClick = {
                        if (newName.isNotBlank() && newName != folderName) {
                            isRenaming = true
                            coroutineScope.launch {
                                viewModel.renameFolder(folderName, newName)
                                showRenameFolderDialog = null
                            }
                        }
                    },
                    filled = true,
                    enabled = newName.isNotBlank() && newName != folderName && !isRenaming
                ) {
                    Text(strings.rename)
                }
            },
            dismissButton = {
                EInkButton(
                    onClick = { showRenameFolderDialog = null },
                    filled = false,
                    enabled = !isRenaming
                ) {
                    Text(strings.cancel)
                }
            },
            containerColor = EInkWhite
        )
    }

    // Delete tag confirmation dialog
    // Tag action menu state
    var showTagActionConfirmation by remember { mutableStateOf<TagAction?>(null) }

    // Tag action menu dialog
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Tag: \"$tag\"") },
            text = {
                Column {
                    Text("Choose an action:")
                }
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Remove from all files option
                    EInkButton(
                        onClick = {
                            showTagActionConfirmation = TagAction.REMOVE_FROM_ALL
                        },
                        filled = false
                    ) {
                        Text(strings.removeTagFromAll)
                    }
                    // Delete tag option
                    EInkButton(
                        onClick = {
                            showTagActionConfirmation = TagAction.DELETE
                        },
                        filled = true
                    ) {
                        Text(strings.deleteTag)
                    }
                }
            },
            dismissButton = {
                EInkButton(
                    onClick = { tagToDelete = null },
                    filled = false
                ) {
                    Text(strings.cancel)
                }
            },
            containerColor = EInkWhite
        )
    }

    // Confirmation dialog for tag actions
    showTagActionConfirmation?.let { action ->
        val tag = tagToDelete ?: return@let
        AlertDialog(
            onDismissRequest = { showTagActionConfirmation = null },
            title = {
                Text(
                    when (action) {
                        TagAction.REMOVE_FROM_ALL -> strings.removeTagFromAll
                        TagAction.DELETE -> strings.deleteTag
                    }
                )
            },
            text = {
                Text(
                    when (action) {
                        TagAction.REMOVE_FROM_ALL -> "${strings.removeTagFromAllConfirmation} \"$tag\"?"
                        TagAction.DELETE -> "${strings.deleteTagConfirmation} \"$tag\"?\n\n${strings.deleteTagWarning}"
                    }
                )
            },
            confirmButton = {
                EInkButton(
                    onClick = {
                        viewModel.deleteTag(tag)
                        tagToDelete = null
                        showTagActionConfirmation = null
                    },
                    filled = true
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                EInkButton(
                    onClick = { showTagActionConfirmation = null },
                    filled = false
                ) {
                    Text(strings.cancel)
                }
            },
            containerColor = EInkWhite
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val strings = rememberStrings()
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
            text = strings.noSavedFiles,
            style = MaterialTheme.typography.titleMedium,
            color = EInkBlack
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.saveFromChat,
            style = MaterialTheme.typography.bodyMedium,
            color = EInkGrayMedium
        )
    }
}

@Composable
private fun FolderCard(
    name: String,
    fileCount: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = rememberStrings()
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
                    text = "$fileCount ${if (fileCount > 1) strings.files else strings.file}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EInkGrayMedium
                )
            }

            // More options menu button
            EInkIconButton(
                onClick = { showMenu = true },
                icon = Icons.Default.MoreVert,
                contentDescription = strings.settings
            )
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(strings.renameFolder) },
                    onClick = {
                        showMenu = false
                        onRename()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text(strings.deleteFolder) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }
        }
    }
}
