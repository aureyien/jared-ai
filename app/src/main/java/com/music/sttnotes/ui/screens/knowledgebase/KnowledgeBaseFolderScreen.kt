package com.music.sttnotes.ui.screens.knowledgebase

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.runtime.LaunchedEffect
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
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.ui.components.EInkChip
import com.music.sttnotes.ui.components.EInkDivider
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.PendingDeletion
import com.music.sttnotes.ui.components.UndoButton
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.data.i18n.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeBaseFolderScreen(
    folderName: String,
    onFileClick: (filename: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val strings = rememberStrings()
    // Use unfiltered folders - filtering is done locally in this screen
    val allFolders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagFilters by viewModel.selectedTagFilters.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()

    // Local search query for this folder
    var searchQuery by remember { mutableStateOf("") }

    // Tag filter visibility
    var showTagFilter by remember { mutableStateOf(false) }

    // Tag deletion state
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    // Merge dialog state
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeFilename by remember { mutableStateOf("") }

    // Find the current folder's files
    val files = remember(allFolders, folderName) {
        allFolders.find { it.name == folderName }?.files ?: emptyList()
    }

    // Get tags specific to this folder's files
    val folderTags = remember(files) {
        files.flatMap { it.tags }.distinct().sorted()
    }

    // Filter files by search query AND selected tags
    val filteredFiles = remember(files, selectedTagFilters, searchQuery) {
        files.filter { file ->
            val matchesQuery = searchQuery.isEmpty() ||
                file.file.nameWithoutExtension.contains(searchQuery, ignoreCase = true) ||
                file.preview.contains(searchQuery, ignoreCase = true) ||
                file.tags.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesTags = selectedTagFilters.isEmpty() ||
                file.tags.any { it in selectedTagFilters }
            matchesQuery && matchesTags
        }
    }

    // Undo deletion state
    var pendingFileDeletion by remember { mutableStateOf<PendingDeletion<String>?>(null) }

    // Filter out pending deletion
    val displayFiles = remember(filteredFiles, pendingFileDeletion) {
        filteredFiles.filter { pendingFileDeletion?.item != it.file.name }
    }

    // Load folders on entry and clear any global tag filters
    LaunchedEffect(Unit) {
        viewModel.clearTagFilters()
        viewModel.loadFolders()
    }

    // Handle system back button
    BackHandler {
        if (selectionMode) {
            viewModel.exitSelectionMode()
        } else {
            pendingFileDeletion?.let { deletion ->
                viewModel.deleteFile(folderName, deletion.item)
            }
            pendingFileDeletion = null
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectionMode) "${selectedFiles.size} ${strings.selected}" else folderName,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    EInkIconButton(
                        onClick = {
                            if (selectionMode) {
                                viewModel.exitSelectionMode()
                            } else {
                                pendingFileDeletion?.let { deletion ->
                                    viewModel.deleteFile(folderName, deletion.item)
                                }
                                pendingFileDeletion = null
                                onNavigateBack()
                            }
                        },
                        icon = if (selectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (selectionMode) strings.cancel else strings.back
                    )
                },
                actions = {
                    if (selectionMode) {
                        // In selection mode: show merge button when 2+ selected
                        if (selectedFiles.size >= 2) {
                            EInkIconButton(
                                onClick = {
                                    mergeFilename = ""
                                    showMergeDialog = true
                                },
                                icon = Icons.AutoMirrored.Filled.CallMerge,
                                contentDescription = strings.merge
                            )
                        }
                    } else {
                        // Normal mode: show undo and selection button
                        pendingFileDeletion?.let { deletion ->
                            UndoButton(
                                onUndo = { pendingFileDeletion = null },
                                onTimeout = {
                                    viewModel.deleteFile(folderName, deletion.item)
                                    pendingFileDeletion = null
                                    // If folder becomes empty, go back
                                    if (files.size <= 1) {
                                        onNavigateBack()
                                    }
                                },
                                itemKey = deletion.item // Restart countdown when different file deleted
                            )
                        }
                        // Selection mode button (only if more than 1 file)
                        if (displayFiles.size > 1) {
                            EInkIconButton(
                                onClick = { viewModel.toggleSelectionMode() },
                                icon = Icons.Default.Checklist,
                                contentDescription = strings.selectToMerge
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
            displayFiles.isEmpty() && selectedTagFilters.isEmpty() && searchQuery.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = strings.emptyFolder,
                        style = MaterialTheme.typography.titleMedium,
                        color = EInkGrayMedium
                    )
                }
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
                            onValueChange = { searchQuery = it },
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
                                        onClick = { searchQuery = "" },
                                        icon = Icons.Default.Close,
                                        contentDescription = strings.clear
                                    )
                                }
                            }
                        )
                        // Tag filter button (only show if there are tags in this folder)
                        if (folderTags.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            EInkIconButton(
                                onClick = { showTagFilter = !showTagFilter },
                                icon = Icons.Default.LocalOffer,
                                contentDescription = strings.filterByTags
                            )
                        }
                    }

                    // Tag filter chips (show when toggled and tags exist)
                    if (showTagFilter && folderTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            folderTags.forEach { tag ->
                                EInkChip(
                                    label = tag,
                                    selected = tag in selectedTagFilters,
                                    onClick = { viewModel.toggleTagFilter(tag) },
                                    onLongClick = { tagToDelete = tag }
                                )
                            }
                        }
                        EInkDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    EInkDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    if (displayFiles.isEmpty() && (selectedTagFilters.isNotEmpty() || searchQuery.isNotEmpty())) {
                        // No results with filter or search
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = strings.noResults,
                                style = MaterialTheme.typography.titleMedium,
                                color = EInkGrayMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayFiles, key = { it.file.name }) { filePreview ->
                                FolderFileItem(
                                    file = filePreview,
                                    selectionMode = selectionMode,
                                    isSelected = filePreview.file.name in selectedFiles,
                                    onClick = {
                                        if (selectionMode) {
                                            viewModel.toggleFileSelection(filePreview.file.name)
                                        } else {
                                            pendingFileDeletion?.let { deletion ->
                                                viewModel.deleteFile(folderName, deletion.item)
                                            }
                                            pendingFileDeletion = null
                                            onFileClick(filePreview.file.name)
                                        }
                                    },
                                    onDelete = {
                                        pendingFileDeletion?.let { prev ->
                                            viewModel.deleteFile(folderName, prev.item)
                                        }
                                        pendingFileDeletion = PendingDeletion(
                                            item = filePreview.file.name,
                                            message = strings.fileDeleted
                                        )
                                    },
                                    onCopyContent = {
                                        viewModel.getFileContent(folderName, filePreview.file.name)
                                    },
                                    isFavorite = viewModel.isFileFavorite(folderName, filePreview.file.name)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete tag confirmation dialog
        tagToDelete?.let { tag -> AlertDialog(
                onDismissRequest = { tagToDelete = null },
                title = { Text(strings.deleteTag) },
                text = {
                    Text("${strings.deleteTagConfirmation} \"$tag\"?\n\n${strings.deleteTagWarning}")
                },
                confirmButton = {
                    EInkButton(
                        onClick = {
                            viewModel.deleteTag(tag)
                            tagToDelete = null
                        },
                        filled = true
                    ) {
                        Text(strings.delete)
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

        // Merge files dialog
        if (showMergeDialog) {
            AlertDialog(
                onDismissRequest = { showMergeDialog = false },
                title = { Text(strings.mergeFiles) },
                text = {
                    Column {
                        Text("${strings.mergeFilesConfirmation} ${selectedFiles.size} ${strings.files}")
                        Spacer(Modifier.height(16.dp))
                        EInkTextField(
                            value = mergeFilename,
                            onValueChange = { mergeFilename = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = strings.newFilename
                        )
                    }
                },
                confirmButton = {
                    EInkButton(
                        onClick = {
                            if (mergeFilename.isNotBlank()) {
                                viewModel.mergeSelectedFiles(folderName, mergeFilename)
                                showMergeDialog = false
                            }
                        },
                        filled = true
                    ) {
                        Text(strings.merge)
                    }
                },
                dismissButton = {
                    EInkButton(
                        onClick = { showMergeDialog = false },
                        filled = false
                    ) {
                        Text(strings.cancel)
                    }
                },
                containerColor = EInkWhite
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderFileItem(
    file: FilePreview,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCopyContent: () -> String?,
    isFavorite: Boolean = false
) {
    val strings = rememberStrings()
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    EInkCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    // Long-press shows context menu (only when not in selection mode)
                    if (!selectionMode) {
                        showMenu = true
                    }
                }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Selection checkbox or file icon
                if (selectionMode) {
                    Icon(
                        if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (isSelected) strings.selected else strings.selectFile,
                        modifier = Modifier.size(24.dp),
                        tint = EInkBlack
                    )
                } else {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = EInkBlack
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.file.nameWithoutExtension,
                        style = MaterialTheme.typography.titleMedium,
                        color = EInkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatRelativeTime(file.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = EInkGrayMedium
                    )
                }
                if (isFavorite && !selectionMode) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = strings.favorites,
                        modifier = Modifier.size(18.dp),
                        tint = EInkBlack
                    )
                    Spacer(Modifier.width(8.dp))
                }
                // Hide delete button in selection mode
                if (!selectionMode) {
                    EInkIconButton(
                        onClick = { showMenu = true },
                        icon = Icons.Default.Delete,
                        contentDescription = strings.delete
                    )
                }
            }
            if (file.preview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = file.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EInkGrayMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Show tags if present
            if (file.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = file.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = EInkGrayMedium
                )
            }
        }

        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.copyContent) },
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
            DropdownMenuItem(
                text = { Text(strings.delete) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val strings = Strings.current

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> strings.now
        minutes < 60 -> "$minutes ${strings.minutesAgo}"
        hours < 24 -> "$hours${strings.hoursAgo}"
        days < 2 -> strings.yesterday
        days < 7 -> "$days${strings.daysAgo}"
        else -> {
            val format = SimpleDateFormat("d MMM", Locale.ENGLISH)
            format.format(Date(timestamp))
        }
    }
}
