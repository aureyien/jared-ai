package com.music.sttnotes.ui.screens.notes

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkConfirmationModal
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkTextField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagManagementScreenForNotes(
    noteId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: NotesListViewModel = hiltViewModel()
) {
    val allNotes by viewModel.notes.collectAsState()
    val allTagsSet by viewModel.allTags.collectAsState()
    val note = if (noteId != null) {
        allNotes.find { it.id == noteId }
    } else {
        null
    }
    val strings = rememberStrings()
    var searchQuery by remember { mutableStateOf("") }
    var newTagInput by remember { mutableStateOf("") }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    // Calculate tag counts across all notes
    val tagCounts = remember(allNotes) {
        allNotes
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
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Add tag input (only in note-specific mode)
            if (note != null && noteId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EInkTextField(
                        value = newTagInput,
                        onValueChange = { if (it.length <= 20) newTagInput = it },
                        placeholder = strings.addTag,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    EInkIconButton(
                        onClick = {
                            if (newTagInput.isNotBlank()) {
                                viewModel.addTagToNote(noteId, newTagInput.trim().lowercase().take(20))
                                newTagInput = ""
                            }
                        },
                        icon = Icons.Default.Add,
                        contentDescription = strings.addTag
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            // 2-column grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredTags) { tag ->
                    val isSelected = note?.tags?.contains(tag) == true
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
                                    if (note != null && noteId != null) {
                                        if (isSelected) {
                                            viewModel.removeTagFromNote(noteId, tag)
                                        } else {
                                            viewModel.addTagToNote(noteId, tag)
                                        }
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
        }

        // Delete tag confirmation dialog
        tagToDelete?.let { tag ->
            EInkConfirmationModal(
                onDismiss = { tagToDelete = null },
                onConfirm = {
                    viewModel.deleteTag(tag)
                    tagToDelete = null
                },
                title = strings.deleteTag,
                message = "${strings.deleteTagConfirmation} \"$tag\"?\n\n${strings.deleteTagWarning}",
                confirmText = strings.delete,
                dismissText = strings.cancel
            )
        }
    }
}
