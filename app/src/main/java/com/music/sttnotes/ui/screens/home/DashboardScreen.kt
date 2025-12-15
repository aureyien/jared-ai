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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    onSettings: () -> Unit,
    onNoteClick: (String) -> Unit = {},
    onConversationClick: (String) -> Unit = {},
    onKbFileClick: (String, String) -> Unit = { _, _ -> },
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isSearching = state.searchQuery.isNotEmpty()

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
                        onClick = onSettings,
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings"
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
            // Global Search Bar
            EInkTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                placeholder = "Search notes, chats, KB...",
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = EInkBlack
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        EInkIconButton(
                            onClick = { viewModel.onSearchQueryChange("") },
                            icon = Icons.Default.Close,
                            contentDescription = "Clear"
                        )
                    }
                }
            )

            if (isSearching) {
                // Search Results
                if (state.searchResults.isEmpty() && !state.isSearching) {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EInkGrayMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
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
            // Notes Section
            DashboardSection(
                title = "Notes",
                count = state.notesCount,
                icon = Icons.Default.Description,
                onClick = onNotesClick,
                onNew = null,
                isEmpty = state.notesCount == 0,
                emptyText = "No notes yet",
                newButtonText = null
            ) {
                state.lastNote?.let { note ->
                    Text(
                        text = note.title.ifEmpty { "Untitled" },
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
                    title = "Conversations",
                    count = state.conversationsCount,
                    icon = Icons.Default.SmartToy,
                    onClick = onConversationsClick,
                    onNew = null,
                    isEmpty = state.conversationsCount == 0,
                    emptyText = "No conversations yet",
                    newButtonText = null
                ) {
                    state.lastConversation?.let { conv ->
                        Text(
                            text = conv.title.ifEmpty { "Untitled" },
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

            // Knowledge Base Section
            DashboardSection(
                title = "Knowledge Base",
                count = state.kbItemsCount,
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                onClick = onKnowledgeBaseClick,
                onNew = null, // KB doesn't have "new" - items are saved from chat
                isEmpty = state.kbItemsCount == 0,
                emptyText = "No saved items",
                newButtonText = null
            ) {
                if (state.lastKbFolder != null && state.lastKbFile != null) {
                    Text(
                        text = state.lastKbFolder!!,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.lastKbFile!!.removeSuffix(".md"),
                        style = MaterialTheme.typography.bodySmall,
                        color = EInkGrayMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
                        is GlobalSearchResult.NoteResult -> result.note.title.ifEmpty { "Untitled" }
                        is GlobalSearchResult.ConversationResult -> result.conversation.title.ifEmpty { "Untitled" }
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
    content: @Composable () -> Unit
) {
    EInkCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (!isEmpty) onClick else null
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
