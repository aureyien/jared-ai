package com.music.sttnotes.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import com.mikepenz.markdown.m3.Markdown
import com.music.sttnotes.ui.components.einkMarkdownColors
import com.music.sttnotes.ui.components.einkMarkdownTypography
import com.music.sttnotes.ui.components.einkMarkdownComponents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.data.chat.ChatConversation
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.ui.components.EInkChip
import com.music.sttnotes.ui.components.EInkDivider
import com.music.sttnotes.ui.components.EInkFormModal
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.PendingDeletion
import com.music.sttnotes.ui.components.UndoButton
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.data.i18n.Strings
import com.music.sttnotes.ui.screens.knowledgebase.UiPreferencesEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatListScreen(
    onConversationClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    onNavigateBack: () -> Unit,
    onManageTags: (String) -> Unit = {},
    onManageTagsGlobal: () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val usedTags by viewModel.usedTags.collectAsState()
    val selectedTagFilters by viewModel.selectedTagFilters.collectAsState()
    val showTagFilter by viewModel.showTagFilter.collectAsState()
    val summaryInProgress by viewModel.summaryInProgress.collectAsState()
    val generatedSummary by viewModel.generatedSummary.collectAsState()
    val existingFolders by viewModel.existingFolders.collectAsState()
    val strings = rememberStrings()

    // Volume scroll support
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val uiPreferences = androidx.compose.ui.platform.LocalContext.current.let { context ->
        remember { dagger.hilt.android.EntryPointAccessors.fromApplication<UiPreferencesEntryPoint>(context.applicationContext).uiPreferences() }
    }
    val volumeScrollEnabled by uiPreferences.volumeButtonScrollEnabled.collectAsState(initial = false)
    val volumeScrollDistance by uiPreferences.volumeButtonScrollDistance.collectAsState(initial = 0.8f)

    val volumeHandler = remember(listState, coroutineScope, volumeScrollDistance) {
        com.music.sttnotes.ui.components.createLazyListVolumeHandler(
            state = listState,
            scope = coroutineScope,
            scrollDistanceProvider = { volumeScrollDistance }
        )
    }

    // Register volume scroll handler with Activity (only if enabled in settings)
    val activity = androidx.compose.ui.platform.LocalContext.current as? com.music.sttnotes.MainActivity
    androidx.compose.runtime.LaunchedEffect(volumeHandler, volumeScrollEnabled) {
        if (volumeScrollEnabled) {
            activity?.setVolumeScrollHandler(volumeHandler)
        } else {
            activity?.setVolumeScrollHandler(null)
        }
    }

    // Clean up handler when screen is disposed
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            activity?.setVolumeScrollHandler(null)
        }
    }

    // Refresh list when screen becomes visible (returning from conversation)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // View state - showing archived or active conversations
    var showingArchived by remember { mutableStateOf(false) }
    val archivedConversations by viewModel.archivedConversations.collectAsState()

    // Undo deletion state
    var pendingDeletion by remember { mutableStateOf<PendingDeletion<ChatConversation>?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val filteredConversations = remember(conversations, archivedConversations, showingArchived, searchQuery, pendingDeletion, selectedTagFilters) {
        val sourceList = if (showingArchived) archivedConversations else conversations
        val baseList = if (searchQuery.isBlank()) {
            sourceList
        } else {
            sourceList.filter { conv ->
                conv.title.contains(searchQuery, ignoreCase = true) ||
                conv.messages.any { it.content.contains(searchQuery, ignoreCase = true) } ||
                conv.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
        // Filter by selected tags
        val tagFiltered = if (selectedTagFilters.isEmpty()) {
            baseList
        } else {
            baseList.filter { conv -> conv.tags.any { it in selectedTagFilters } }
        }
        // Filter out pending deletion items from display
        tagFiltered.filter { conv -> pendingDeletion?.item?.id != conv.id }
    }

    // Dialog states
    var showRenameDialog by remember { mutableStateOf<ChatConversation?>(null) }

    // Handle system back button - commit pending deletion before leaving
    BackHandler {
        pendingDeletion?.let { deletion ->
            viewModel.deleteConversation(deletion.item.id)
        }
        pendingDeletion = null
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showingArchived) strings.archivedChats else strings.chat,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    EInkIconButton(
                        onClick = {
                            // Commit pending deletion before navigating back
                            pendingDeletion?.let { deletion ->
                                viewModel.deleteConversation(deletion.item.id)
                                pendingDeletion = null
                            }
                            onNavigateBack()
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back
                    )
                },
                actions = {
                    // Undo button in TopAppBar
                    pendingDeletion?.let { deletion ->
                        UndoButton(
                            onUndo = { pendingDeletion = null },
                            onTimeout = {
                                viewModel.deleteConversation(deletion.item.id)
                                pendingDeletion = null
                            },
                            itemKey = deletion.item.id // Restart countdown when different item deleted
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    EInkIconButton(
                        onClick = { showingArchived = !showingArchived },
                        icon = Icons.Default.Archive,
                        contentDescription = if (showingArchived) strings.chat else strings.archivedChats
                    )
                    Spacer(Modifier.width(8.dp))
                    EInkIconButton(
                        onClick = onNewConversation,
                        icon = Icons.Default.Add,
                        contentDescription = strings.newConversation
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EInkWhite,
                border = BorderStroke(1.dp, EInkBlack)
            ) {
                Button(
                    onClick = onNewConversation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EInkBlack,
                        contentColor = EInkWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.newConversation)
                }
            }
        },
        containerColor = EInkWhite
    ) { padding ->
        if (conversations.isEmpty()) {
            EmptyConversationsState(
                onNewConversation = onNewConversation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search bar with tag filter button
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
                    // Tag filter button (only show if there are tags)
                    if (usedTags.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        EInkIconButton(
                            onClick = { viewModel.toggleShowTagFilter() },
                            onLongClick = onManageTagsGlobal,
                            icon = Icons.Default.LocalOffer,
                            contentDescription = strings.filterByTags
                        )
                    }
                }

                // Tag filter chips (show when toggled and tags exist)
                if (showTagFilter && usedTags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        usedTags.forEach { tag ->
                            EInkChip(
                                label = tag,
                                selected = tag in selectedTagFilters,
                                onClick = { viewModel.toggleTagFilter(tag) }
                            )
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = {
                                // Commit any pending deletion before navigating
                                pendingDeletion?.let { deletion ->
                                    viewModel.deleteConversation(deletion.item.id)
                                }
                                pendingDeletion = null
                                onConversationClick(conversation.id)
                            },
                            onRename = { showRenameDialog = conversation },
                            onDelete = {
                                // Commit previous pending deletion first
                                pendingDeletion?.let { previousDeletion ->
                                    viewModel.deleteConversation(previousDeletion.item.id)
                                }
                                // Set new pending deletion
                                pendingDeletion = PendingDeletion(
                                    item = conversation,
                                    message = strings.conversationDeleted
                                )
                            },
                            onArchive = { viewModel.archiveConversation(conversation.id) },
                            onUnarchive = { viewModel.unarchiveConversation(conversation.id) },
                            onManageTags = { onManageTags(conversation.id) },
                            onSummarize = { viewModel.generateSummary(conversation.id) },
                            showTags = showTagFilter,
                            onToggleFavorite = { viewModel.toggleConversationFavorite(conversation.id) },
                            isSummarizing = summaryInProgress == conversation.id,
                            isArchived = showingArchived
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    showRenameDialog?.let { conv ->
        RenameDialog(
            currentTitle = conv.title,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newTitle ->
                viewModel.renameConversation(conv.id, newTitle)
                showRenameDialog = null
            }
        )
    }

    // Full-screen loading view while generating summary
    summaryInProgress?.let { convId ->
        val conversation = conversations.find { it.id == convId }
        SummaryLoadingView(
            title = conversation?.title ?: strings.conversationSummary,
            onCancel = { viewModel.clearSummaryProgress() }
        )
    }

    // Full-screen summary view
    generatedSummary?.let { (convId, summary) ->
        val conversation = conversations.find { it.id == convId }
        SummaryFullScreenView(
            title = conversation?.title ?: strings.conversationSummary,
            summary = summary,
            existingFolders = existingFolders,
            onClose = { viewModel.clearSummary() },
            onSaveToKb = { folderName, filename ->
                viewModel.saveSummaryToKb(convId, summary, folderName, filename)
                viewModel.clearSummary()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationCard(
    conversation: ChatConversation,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onManageTags: () -> Unit,
    onSummarize: () -> Unit,
    showTags: Boolean,
    onToggleFavorite: () -> Unit,
    isSummarizing: Boolean = false,
    isArchived: Boolean = false
) {
    val strings = rememberStrings()
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        EInkCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.title.ifEmpty { strings.untitled },
                        style = MaterialTheme.typography.titleMedium,
                        color = EInkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (conversation.isFavorite) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = strings.favorites,
                            modifier = Modifier.size(18.dp),
                            tint = EInkBlack
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = formatRelativeTime(conversation.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = EInkGrayMedium
                    )
                }

                val preview = conversation.getLastResponsePreview()
                if (preview.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EInkGrayMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Message count indicator
                val messageCount = conversation.messages.size
                if (messageCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$messageCount ${if (messageCount > 1) strings.messages else strings.message}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EInkGrayMedium
                    )
                }

                // Tags display (only when showTags is true)
                if (showTags && conversation.tags.isNotEmpty()) {
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
                        conversation.tags.forEach { tag ->
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
        }

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (conversation.isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                onClick = {
                    showContextMenu = false
                    onToggleFavorite()
                },
                leadingIcon = {
                    Icon(
                        if (conversation.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(strings.rename) },
                onClick = {
                    showContextMenu = false
                    onRename()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(strings.manageTags) },
                onClick = {
                    showContextMenu = false
                    onManageTags()
                },
                leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) }
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
                    showContextMenu = false
                    onSummarize()
                },
                leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null) },
                enabled = !isSummarizing && conversation.messages.isNotEmpty()
            )
            if (isArchived) {
                DropdownMenuItem(
                    text = { Text(strings.unarchiveChat) },
                    onClick = {
                        showContextMenu = false
                        onUnarchive()
                    },
                    leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null) }
                )
            } else {
                DropdownMenuItem(
                    text = { Text(strings.archiveChat) },
                    onClick = {
                        showContextMenu = false
                        onArchive()
                    },
                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                )
            }
            DropdownMenuItem(
                text = { Text(strings.delete) },
                onClick = {
                    showContextMenu = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun EmptyConversationsState(
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = rememberStrings()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = EInkGrayMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.noConversations,
            style = MaterialTheme.typography.titleMedium,
            color = EInkBlack
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.startNewConversation,
            style = MaterialTheme.typography.bodyMedium,
            color = EInkGrayMedium
        )
        Spacer(Modifier.height(24.dp))
        EInkButton(
            onClick = onNewConversation,
            filled = true
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.newConversation)
        }
    }
}

@Composable
private fun RenameDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = rememberStrings()
    var newTitle by remember { mutableStateOf(currentTitle) }

    EInkFormModal(
        onDismiss = onDismiss,
        onConfirm = { onConfirm(newTitle) },
        title = strings.renameConversation,
        confirmText = strings.rename,
        dismissText = strings.cancel,
        confirmEnabled = newTitle.isNotBlank()
    ) {
        EInkTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            placeholder = strings.newTitle,
            modifier = Modifier.fillMaxWidth(),
            showClearButton = true
        )
    }
}

/**
 * Format timestamp as relative time (e.g., "2h ago", "yesterday", "3d ago")
 */
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryFullScreenView(
    title: String,
    summary: String,
    existingFolders: List<String>,
    onClose: () -> Unit,
    onSaveToKb: (folderName: String, filename: String) -> Unit
) {
    val strings = rememberStrings()
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        strings.conversationSummary,
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
                    EInkIconButton(
                        onClick = { showSaveDialog = true },
                        icon = Icons.Default.Save,
                        contentDescription = strings.saveToKb
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

    // Save to KB dialog
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
private fun SummaryLoadingView(
    title: String,
    onCancel: () -> Unit
) {
    val strings = rememberStrings()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        strings.conversationSummary,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    EInkIconButton(
                        onClick = onCancel,
                        icon = Icons.Default.Close,
                        contentDescription = strings.cancel
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = EInkBlack,
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = strings.thinking,
                style = MaterialTheme.typography.titleMedium,
                color = EInkBlack
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = EInkGrayMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaveToKbDialog(
    suggestedTitle: String,
    existingFolders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (folderName: String, filename: String) -> Unit
) {
    val strings = rememberStrings()
    var filename by remember { mutableStateOf(suggestedTitle) }
    var selectedFolder by remember { mutableStateOf(existingFolders.firstOrNull() ?: "") }
    var showNewFolderInput by remember { mutableStateOf(existingFolders.isEmpty()) }
    var newFolderName by remember { mutableStateOf("Summaries") }

    val finalFolder = if (showNewFolderInput) newFolderName else selectedFolder

    EInkFormModal(
        onDismiss = onDismiss,
        onConfirm = { onConfirm(finalFolder, filename) },
        title = strings.saveToKb,
        confirmText = strings.save,
        dismissText = strings.cancel,
        confirmEnabled = filename.isNotBlank() && finalFolder.isNotBlank()
    ) {
        Column {
            // Filename input
            Text(
                text = "${strings.filename}:",
                style = MaterialTheme.typography.labelLarge,
                color = EInkBlack
            )
            Spacer(Modifier.height(4.dp))
            EInkTextField(
                value = filename,
                onValueChange = { filename = it },
                placeholder = strings.filename,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "${strings.folder}:",
                style = MaterialTheme.typography.labelLarge,
                color = EInkBlack
            )
            Spacer(Modifier.height(8.dp))

            // Folder selection with chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // New folder chip
                EInkChip(
                    label = strings.newFolder,
                    selected = showNewFolderInput,
                    onClick = {
                        showNewFolderInput = true
                        selectedFolder = ""
                    }
                )

                // Existing folders
                existingFolders.forEach { folder ->
                    EInkChip(
                        label = folder,
                        selected = selectedFolder == folder && !showNewFolderInput,
                        onClick = {
                            selectedFolder = folder
                            showNewFolderInput = false
                        }
                    )
                }
            }

            // New folder input
            if (showNewFolderInput) {
                Spacer(Modifier.height(8.dp))
                EInkTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = strings.newFolderName,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

