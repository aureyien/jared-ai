package com.music.sttnotes.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.music.sttnotes.ui.components.EInkBottomActionBar
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.ui.screens.knowledgebase.UiPreferencesEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNotesClick: () -> Unit,
    onNewNote: () -> Unit,
    onNewNoteWithRecording: () -> Unit,
    onConversationsClick: () -> Unit,
    onNewConversation: () -> Unit,
    onNewConversationWithRecording: () -> Unit,
    onKnowledgeBaseClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettings: () -> Unit,
    onNoteClick: (String) -> Unit = {},
    onConversationClick: (String) -> Unit = {},
    onKbFileClick: (String, String) -> Unit = { _, _ -> },
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val strings = rememberStrings()
    val isSearching = state.searchQuery.isNotEmpty()
    var showSearchField by rememberSaveable { mutableStateOf(false) }

    // Keep search field visible if there's a search query
    LaunchedEffect(isSearching) {
        if (isSearching) {
            showSearchField = true
        }
    }

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

    // Refresh on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jared AI", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    EInkIconButton(
                        onClick = { showSearchField = !showSearchField },
                        icon = Icons.Default.Search,
                        contentDescription = strings.search
                    )
                    EInkIconButton(
                        onClick = onSettings,
                        icon = Icons.Default.Settings,
                        contentDescription = strings.settings
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        bottomBar = {
            EInkBottomActionBar(
                onAddNote = onNewNote,
                onAddNoteWithRecording = onNewNoteWithRecording,
                onChat = onNewConversation,  // Opens new conversation directly (ChatList accessible from card)
                onChatLongPress = onNewConversationWithRecording,
                onKnowledgeBase = onKnowledgeBaseClick,
                showChat = state.isLlmConfigured
            )
        },
        containerColor = EInkWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Global Search Bar - only show when search icon is clicked or there's a query
            if (showSearchField || isSearching) {
                EInkTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    placeholder = strings.searchPlaceholder,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = EInkBlack
                        )
                    },
                    trailingIcon = {
                        EInkIconButton(
                            onClick = {
                                viewModel.onSearchQueryChange("")
                                showSearchField = false
                            },
                            icon = Icons.Default.Close,
                            contentDescription = strings.clear
                        )
                    }
                )
            }

            if (isSearching) {
                // Search Results
                if (state.searchResults.isEmpty() && !state.isSearching) {
                    Text(
                        text = strings.noResults,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EInkGrayMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.searchResults) { result ->
                            SearchResultItem(
                                result = result,
                                onNoteClick = { onNoteClick(it.id) },
                                onConversationClick = { onConversationClick(it.id) },
                                onKbClick = { folder, file -> onKbFileClick(folder, file) }
                            )
                        }
                    }
                }
            } else {
                // Normal Dashboard Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Favorites Section - clickable card
                    if (state.favoriteItems.isNotEmpty()) {
                        DashboardSection(
                            title = strings.favorites,
                            count = state.favoriteItems.size,
                            icon = Icons.Filled.Star,
                            onClick = onFavoritesClick,
                            onNew = null,
                            isEmpty = false,
                            emptyText = "",
                            newButtonText = null
                        ) {
                            // Show preview of favorite items types
                            val noteCount = state.favoriteItems.count { it is FavoriteItem.NoteItem }
                            val chatCount = state.favoriteItems.count { it is FavoriteItem.ConversationItem }
                            val kbCount = state.favoriteItems.count { it is FavoriteItem.KbItem }

                            Text(
                                text = buildString {
                                    val parts = mutableListOf<String>()
                                    if (noteCount > 0) parts.add("$noteCount ${strings.filterNotes}")
                                    if (chatCount > 0) parts.add("$chatCount ${strings.filterChat}")
                                    if (kbCount > 0) parts.add("$kbCount ${strings.filterKb}")
                                    append(parts.joinToString(" • "))
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = EInkGrayMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

            // Notes Section - always clickable (to access archives even when no active notes)
            DashboardSection(
                title = strings.notes,
                count = state.notesCount,
                icon = Icons.Default.Description,
                onClick = onNotesClick,  // Always enabled - can access archives
                onNew = null,
                isEmpty = state.notesCount == 0,
                emptyText = strings.noNotes,
                newButtonText = null,
                alwaysClickable = true  // Enable click to access archives
            ) {
                state.lastNote?.let { note ->
                    Text(
                        text = note.title.ifEmpty { strings.untitled },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.content.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = note.content.take(80).replace("\n", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = EInkGrayMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Conversations Section - only show if LLM is configured
            if (state.isLlmConfigured) {
                DashboardSection(
                    title = strings.chat,
                    count = state.conversationsCount,
                    icon = Icons.Default.SmartToy,
                    onClick = onConversationsClick,
                    onNew = null,
                    isEmpty = state.conversationsCount == 0,
                    emptyText = strings.noConversations,
                    newButtonText = null
                ) {
                    state.lastConversation?.let { conv ->
                        Text(
                            text = conv.title.ifEmpty { strings.untitled },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val preview = conv.getLastResponsePreview()
                        if (preview.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodySmall,
                                color = EInkGrayMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            // If LLM not configured, section is hidden and Chat button in bottom bar is also hidden

            // Knowledge Base Section - Custom inverted design
            KnowledgeBaseSection(
                count = state.kbItemsCount,
                items = state.lastKbItems,
                isEmpty = state.kbItemsCount == 0,
                onSectionClick = onKnowledgeBaseClick,
                onItemClick = onKbFileClick,
                emptyText = strings.noSavedFiles,
                modifier = Modifier.padding(bottom = 24.dp)
            )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: GlobalSearchResult,
    onNoteClick: (com.music.sttnotes.data.notes.Note) -> Unit,
    onConversationClick: (com.music.sttnotes.data.chat.ChatConversation) -> Unit,
    onKbClick: (String, String) -> Unit
) {
    val strings = rememberStrings()
    EInkCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            when (result) {
                is GlobalSearchResult.NoteResult -> onNoteClick(result.note)
                is GlobalSearchResult.ConversationResult -> onConversationClick(result.conversation)
                is GlobalSearchResult.KbResult -> onKbClick(result.folder, result.filename)
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            Icon(
                imageVector = when (result) {
                    is GlobalSearchResult.NoteResult -> Icons.Default.Description
                    is GlobalSearchResult.ConversationResult -> Icons.Default.SmartToy
                    is GlobalSearchResult.KbResult -> Icons.AutoMirrored.Filled.LibraryBooks
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = EInkBlack
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (result) {
                        is GlobalSearchResult.NoteResult -> result.note.title.ifEmpty { strings.untitled }
                        is GlobalSearchResult.ConversationResult -> result.conversation.title.ifEmpty { strings.untitled }
                        is GlobalSearchResult.KbResult -> result.filename.removeSuffix(".md")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (result) {
                        is GlobalSearchResult.NoteResult -> result.note.content.take(60).replace("\n", " ")
                        is GlobalSearchResult.ConversationResult -> result.conversation.getLastResponsePreview()
                        is GlobalSearchResult.KbResult -> "${result.folder} • ${result.preview}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = EInkGrayMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    onNew: (() -> Unit)?,
    isEmpty: Boolean,
    emptyText: String,
    newButtonText: String?,
    alwaysClickable: Boolean = false,  // Allow click even when empty (e.g., to access archives)
    content: @Composable () -> Unit
) {
    EInkCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (!isEmpty || alwaysClickable) onClick else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = EInkBlack
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = EInkBlack
                    )
                }
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.titleMedium,
                    color = EInkGrayMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            if (isEmpty) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EInkGrayMedium
                    )
                    if (onNew != null && newButtonText != null) {
                        Spacer(Modifier.height(12.dp))
                        EInkButton(onClick = onNew, filled = true) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(newButtonText)
                        }
                    }
                }
            } else {
                content()
            }
        }
    }
}

@Composable
private fun KnowledgeBaseSection(
    count: Int,
    items: List<KbPreviewItem>,
    isEmpty: Boolean,
    onSectionClick: () -> Unit,
    onItemClick: (String, String) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier
) {
    val strings = rememberStrings()

    EInkCard(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isEmpty) 120.dp else 300.dp),
        onClick = if (isEmpty) onSectionClick else null,
        backgroundColor = EInkBlack
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = EInkWhite
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = strings.knowledgeBase,
                        style = MaterialTheme.typography.titleMedium,
                        color = EInkWhite
                    )
                }
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.titleMedium,
                    color = EInkWhite.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(12.dp))

            if (isEmpty) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EInkWhite.copy(alpha = 0.7f)
                    )
                }
            } else {
                // List of last 3 items
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        EInkCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onItemClick(item.folder, item.filename) },
                            backgroundColor = EInkWhite
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                // Folder - Title on same line
                                Text(
                                    text = "${item.folder} - ${item.filename.removeSuffix(".md")}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = EInkBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Preview
                                if (item.preview.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = item.preview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EInkGrayMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // "View All" button at the bottom
                    if (count > 3) {
                        EInkButton(
                            onClick = onSectionClick,
                            modifier = Modifier.fillMaxWidth(),
                            filled = false
                        ) {
                            Text("View All ($count)", color = EInkWhite)
                        }
                    }
                }
            }
        }
    }
}
