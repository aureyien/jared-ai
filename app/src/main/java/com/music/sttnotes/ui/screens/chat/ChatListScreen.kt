package com.music.sttnotes.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
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
import com.music.sttnotes.ui.components.EInkDivider
import com.music.sttnotes.ui.components.EInkIconButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onConversationClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val strings = rememberStrings()

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

    // Undo deletion state
    var pendingDeletion by remember { mutableStateOf<PendingDeletion<ChatConversation>?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val filteredConversations = remember(conversations, searchQuery, pendingDeletion) {
        val baseList = if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conv ->
                conv.title.contains(searchQuery, ignoreCase = true) ||
                conv.messages.any { it.content.contains(searchQuery, ignoreCase = true) }
            }
        }
        // Filter out pending deletion items from display
        baseList.filter { conv -> pendingDeletion?.item?.id != conv.id }
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
                title = { Text(strings.chat, style = MaterialTheme.typography.headlineSmall) },
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
                // Search field
                EInkTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = strings.searchPlaceholder,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = strings.search,
                            tint = EInkGrayMedium
                        )
                    }
                )

                LazyColumn(
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
                            }
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationCard(
    conversation: ChatConversation,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
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
                        text = "$messageCount message${if (messageCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EInkGrayMedium
                    )
                }
            }
        }

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.rename) },
                onClick = {
                    showContextMenu = false
                    onRename()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.renameConversation) },
        text = {
            EInkTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                placeholder = strings.newTitle,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            EInkButton(
                onClick = { onConfirm(newTitle) },
                filled = true,
                enabled = newTitle.isNotBlank()
            ) {
                Text(strings.rename)
            }
        },
        dismissButton = {
            EInkButton(
                onClick = onDismiss,
                filled = false
            ) {
                Text(strings.cancel)
            }
        },
        containerColor = EInkWhite
    )
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
