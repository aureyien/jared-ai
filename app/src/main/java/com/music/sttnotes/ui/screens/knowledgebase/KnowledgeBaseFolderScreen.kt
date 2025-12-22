package com.music.sttnotes.ui.screens.knowledgebase

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.mikepenz.markdown.m3.Markdown
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.ui.components.EInkChip
import com.music.sttnotes.ui.components.EInkConfirmationModal
import com.music.sttnotes.ui.components.EInkDivider
import com.music.sttnotes.ui.components.EInkFormModal
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkLoadingIndicator
import com.music.sttnotes.ui.components.EInkModal
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.PendingDeletion
import com.music.sttnotes.ui.components.ShareResultModal
import com.music.sttnotes.ui.components.UndoButton
import com.music.sttnotes.ui.components.einkMarkdownColors
import com.music.sttnotes.ui.components.einkMarkdownComponents
import com.music.sttnotes.ui.components.einkMarkdownTypography
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
    onManageTags: (folder: String, filename: String) -> Unit = { _, _ -> },
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val strings = rememberStrings()

    // Collect state from ViewModel - EXACT same pattern as ChatListScreen
    val allFolders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTagFilters by viewModel.selectedTagFilters.collectAsState()
    val showTagFilter by viewModel.showTagFilter.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val summaryInProgress by viewModel.summaryInProgress.collectAsState()
    val generatedSummary by viewModel.generatedSummary.collectAsState()
    val shareInProgress by viewModel.shareInProgress.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()
    val shareEnabled by viewModel.shareEnabled.collectAsState(initial = false)

    // Undo deletion state
    var pendingFileDeletion by remember { mutableStateOf<PendingDeletion<String>?>(null) }

    // Tag deletion state
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    // Merge dialog state
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeFilename by remember { mutableStateOf("") }

    // Find the current folder's files - NOT a State, just a regular value
    val files = remember(allFolders, folderName) {
        allFolders.find { it.name == folderName }?.files ?: emptyList()
    }

    // Get tags specific to this folder's files
    val folderTags = remember(files) {
        files.flatMap { it.tags }.distinct().sorted()
    }

    // Local search query - mutableStateOf
    var searchQuery by remember { mutableStateOf("") }

    // FILTERING - EXACT same pattern as ChatListScreen line 126
    val filteredFiles = remember(files, searchQuery, pendingFileDeletion, selectedTagFilters) {
        val baseList = if (searchQuery.isBlank()) {
            files
        } else {
            files.filter { file ->
                file.file.nameWithoutExtension.contains(searchQuery, ignoreCase = true) ||
                file.preview.contains(searchQuery, ignoreCase = true) ||
                file.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
        // Filter by selected tags
        val tagFiltered = if (selectedTagFilters.isEmpty()) {
            baseList
        } else {
            baseList.filter { file -> file.tags.any { it in selectedTagFilters } }
        }
        // Filter out pending deletion items from display
        tagFiltered.filter { file -> pendingFileDeletion?.item != file.file.name }
    }

    val displayFiles = filteredFiles

    // Load folders on entry
    LaunchedEffect(Unit) {
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
            displayFiles.isEmpty() && searchQuery.isEmpty() && selectedTagFilters.isEmpty() -> {
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
                    // Search bar with tag filter button - EXACT ChatListScreen pattern
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    contentDescription = strings.search,
                                    tint = EInkGrayMedium
                                )
                            }
                        )
                        // Tag filter button (only show if there are tags) - EXACT ChatListScreen pattern
                        if (folderTags.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            EInkIconButton(
                                onClick = { viewModel.toggleShowTagFilter() },
                                icon = Icons.Default.LocalOffer,
                                contentDescription = strings.filterByTags
                            )
                        }
                    }

                    // Tag filter chips (show when toggled and tags exist) - EXACT ChatListScreen pattern
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
                                    onClick = { viewModel.toggleTagFilter(tag) }
                                )
                            }
                        }
                    }

                    EInkDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    if (displayFiles.isEmpty() && (searchQuery.isNotEmpty() || selectedTagFilters.isNotEmpty())) {
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
                                    isFavorite = viewModel.isFileFavorite(folderName, filePreview.file.name),
                                    onToggleFavorite = {
                                        viewModel.toggleFileFavorite(folderName, filePreview.file.name)
                                    },
                                    onManageTags = {
                                        onManageTags(folderName, filePreview.file.name)
                                    },
                                    onSummarize = {
                                        viewModel.generateSummary(folderName, filePreview.file.name)
                                    },
                                    isSummarizing = summaryInProgress == "$folderName/${filePreview.file.name}",
                                    showTags = showTagFilter,
                                    onShare = {
                                        viewModel.shareArticle(folderName, filePreview.file.name)
                                    },
                                    isShareEnabled = shareEnabled
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete tag confirmation dialog
        tagToDelete?.let { tag ->
            EInkConfirmationModal(
                onDismiss = { tagToDelete = null },
                onConfirm = { viewModel.deleteTag(tag) },
                title = strings.deleteTag,
                message = "${strings.deleteTagConfirmation} \"$tag\"?\n\n${strings.deleteTagWarning}",
                confirmText = strings.delete,
                dismissText = strings.cancel
            )
        }

        // Merge files dialog
        if (showMergeDialog) {
            EInkFormModal(
                onDismiss = { showMergeDialog = false },
                onConfirm = {
                    viewModel.mergeSelectedFiles(folderName, mergeFilename)
                    showMergeDialog = false
                },
                title = strings.mergeFiles,
                confirmText = strings.merge,
                confirmEnabled = mergeFilename.isNotBlank()
            ) {
                Text("${strings.mergeFilesConfirmation} ${selectedFiles.size} ${strings.files}")
                Spacer(Modifier.height(16.dp))
                EInkTextField(
                    value = mergeFilename,
                    onValueChange = { mergeFilename = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = strings.newFilename
                )
            }
        }
    }

    // Share result modal
    shareResult?.let { (fileId, response) ->
        ShareResultModal(
            shareResponse = response,
            onDismiss = { viewModel.clearShareResult() }
        )
    }

    // Share loading indicator
    shareInProgress?.let { fileId ->
        val filename = fileId.substringAfterLast("/")
        EInkModal(
            onDismiss = {},
            dismissOnBackgroundClick = false
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EInkLoadingIndicator(text = "${strings.sharing} $filename...")
            }
        }
    }

    // Full-screen summary view
    generatedSummary?.let { (fileId, summary) ->
        val filename = fileId.substringAfterLast("/")
        val isLoading = summaryInProgress == fileId
        SummaryFullScreenView(
            title = filename.removeSuffix(".md"),
            summary = summary,
            isLoading = isLoading,
            onClose = { viewModel.clearSummary() },
            onSaveToKb = { folderName, filename ->
                viewModel.saveSummaryToKb(fileId, summary, folderName, filename)
                viewModel.clearSummary()
            },
            existingFolders = allFolders.map { it.name }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SummaryFullScreenView(
    title: String,
    summary: String,
    isLoading: Boolean,
    onClose: () -> Unit,
    onSaveToKb: (folderName: String, filename: String) -> Unit,
    existingFolders: List<String> = emptyList()
) {
    val strings = rememberStrings()
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    EInkIconButton(
                        onClick = onClose,
                        icon = Icons.Default.Close,
                        contentDescription = strings.close
                    )
                },
                actions = {
                    // Only show save button when summary is ready
                    if (!isLoading) {
                        EInkIconButton(
                            onClick = { showSaveDialog = true },
                            icon = Icons.Default.Save,
                            contentDescription = strings.saveToKb
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
        if (isLoading) {
            // Show loading indicator while summary is being generated
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EInkLoadingIndicator(text = strings.loading)
            }
        } else {
            // Show summary content when ready
            SelectionContainer {
                Markdown(
                    content = summary,
                    colors = einkMarkdownColors(),
                    typography = einkMarkdownTypography(),
                    components = einkMarkdownComponents(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }

    // Save to KB dialog with folder dropdown
    if (showSaveDialog) {
        SaveToKbDialog(
            suggestedTitle = title,
            existingFolders = existingFolders,
            onDismiss = { showSaveDialog = false },
            onConfirm = { folderName, filename ->
                showSaveDialog = false
                onSaveToKb(folderName, filename)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveToKbDialog(
    suggestedTitle: String,
    existingFolders: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (folderName: String, filename: String) -> Unit
) {
    val strings = rememberStrings()
    var folderName by remember { mutableStateOf(existingFolders.firstOrNull() ?: "Summaries") }
    var filename by remember { mutableStateOf(suggestedTitle.take(50)) }
    var showFolderDropdown by remember { mutableStateOf(false) }
    var isNewFolder by remember { mutableStateOf(existingFolders.isEmpty()) }

    EInkFormModal(
        onDismiss = onDismiss,
        onConfirm = { onConfirm(folderName, filename) },
        title = strings.saveToKb,
        confirmText = strings.save,
        confirmEnabled = folderName.isNotBlank() && filename.isNotBlank()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Folder selection with dropdown
            Box {
                EInkTextField(
                    value = folderName,
                    onValueChange = {
                        folderName = it
                        isNewFolder = true
                    },
                    placeholder = strings.folderName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { showFolderDropdown = !showFolderDropdown }
                        ),
                    readOnly = false,
                    trailingIcon = if (existingFolders.isNotEmpty()) {
                        {
                            EInkIconButton(
                                onClick = { showFolderDropdown = !showFolderDropdown },
                                icon = if (showFolderDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = strings.selectFile
                            )
                        }
                    } else null
                )

                DropdownMenu(
                    expanded = showFolderDropdown,
                    onDismissRequest = { showFolderDropdown = false }
                ) {
                    // Existing folders
                    existingFolders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder) },
                            onClick = {
                                folderName = folder
                                isNewFolder = false
                                showFolderDropdown = false
                            }
                        )
                    }
                    if (existingFolders.isNotEmpty()) {
                        EInkDivider()
                    }
                    // New folder option
                    DropdownMenuItem(
                        text = { Text("+ ${strings.newFolder}") },
                        onClick = {
                            folderName = ""
                            isNewFolder = true
                            showFolderDropdown = false
                        }
                    )
                }
            }

            // Filename input
            EInkTextField(
                value = filename,
                onValueChange = { filename = it },
                placeholder = strings.fileName,
                modifier = Modifier.fillMaxWidth()
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
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onManageTags: () -> Unit = {},
    onSummarize: () -> Unit = {},
    isSummarizing: Boolean = false,
    showTags: Boolean = false,
    onShare: () -> Unit = {},
    isShareEnabled: Boolean = false
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
                // Hide menu button in selection mode
                if (!selectionMode) {
                    EInkIconButton(
                        onClick = { showMenu = true },
                        icon = Icons.Default.MoreVert,
                        contentDescription = strings.settings
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
            // Show tags with styled chips (same as chat list) when showTags is true
            if (showTags && file.tags.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                // Gray separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(EInkGrayMedium.copy(alpha = 0.3f))
                )
                Spacer(Modifier.height(14.dp))
                // Tags in styled chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    file.tags.forEach { tag ->
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

        // Context menu (same structure as chat list)
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                onClick = {
                    showMenu = false
                    onToggleFavorite()
                },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(strings.manageTags) },
                onClick = {
                    showMenu = false
                    onManageTags()
                },
                leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.summarize)
                        if (isSummarizing) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = EInkBlack
                            )
                        }
                    }
                },
                onClick = {
                    showMenu = false
                    onSummarize()
                },
                leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(20.dp)) },
                enabled = !isSummarizing
            )
            // Share article (conditionally visible)
            if (isShareEnabled) {
                DropdownMenuItem(
                    text = { Text(strings.shareArticle) },
                    onClick = {
                        showMenu = false
                        onShare()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                )
            }
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
