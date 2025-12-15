package com.music.sttnotes.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayLight
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.EInkDivider
import com.music.sttnotes.ui.components.EInkButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagManagementScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val allTagsSet by viewModel.allTags.collectAsState()
    val conversation = conversations.find { it.id == conversationId } ?: run {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }
    val strings = rememberStrings()
    var searchQuery by remember { mutableStateOf("") }
    var newTagInput by remember { mutableStateOf("") }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    // Calculate tag counts across all conversations
    val tagCounts = remember(conversations) {
        conversations
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
    }

    // Get all unique tags sorted by usage count (0 for unused tags)
    val allTags = remember(allTagsSet, tagCounts) {
        allTagsSet.sortedByDescending { tagCounts[it] ?: 0 }
    }

    // Filter tags based on search
    val filteredTags = remember(allTags, searchQuery) {
        if (searchQuery.isBlank()) {
            allTags
        } else {
            allTags.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.manageTags) },
                navigationIcon = {
                    EInkIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                )
            )
        },
        containerColor = EInkWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            EInkTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = strings.searchTags,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Add new tag section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EInkTextField(
                    value = newTagInput,
                    onValueChange = { if (it.length <= 20) newTagInput = it },
                    placeholder = strings.addTagToConversation,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                EInkIconButton(
                    onClick = {
                        if (newTagInput.isNotBlank() && newTagInput !in conversation.tags) {
                            viewModel.addTagToConversation(conversationId, newTagInput.trim())
                            newTagInput = ""
                            searchQuery = ""
                        }
                    },
                    icon = Icons.Default.Add,
                    contentDescription = strings.addTag
                )
            }

            EInkDivider()

            Column(modifier = Modifier.fillMaxSize()) {
                // Show selected tags count (always show, even if 0)
                Text(
                    text = "${strings.selectedTags}: ${conversation.tags.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EInkGrayMedium,
                    modifier = Modifier.padding(16.dp)
                )

                // Tags grid
                if (filteredTags.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTags) { tag ->
                            val isSelected = tag in conversation.tags
                            val count = tagCounts[tag] ?: 0

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EInkWhite,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) EInkBlack else EInkGrayMedium.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelected) {
                                                viewModel.removeTagFromConversation(conversationId, tag)
                                            } else {
                                                viewModel.addTagToConversation(conversationId, tag)
                                            }
                                        },
                                        onLongClick = {
                                            tagToDelete = tag
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = EInkBlack,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "($count)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EInkGrayMedium
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = strings.selected,
                                            tint = EInkBlack,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (searchQuery.isNotBlank()) {
                    // Show message if no tags found
                    Text(
                        text = strings.noResults,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EInkGrayMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            }
        }

        // Delete tag confirmation dialog
        tagToDelete?.let { tag ->
            AlertDialog(
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
    }
}
