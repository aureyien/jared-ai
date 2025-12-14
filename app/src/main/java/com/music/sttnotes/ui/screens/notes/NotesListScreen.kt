package com.music.sttnotes.ui.screens.notes

import android.content.Intent
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ViewList
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.data.notes.Note
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.ui.components.EInkChip
import com.music.sttnotes.ui.components.EInkDivider
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.components.EInkTextField
import com.music.sttnotes.ui.components.PendingDeletion
import com.music.sttnotes.ui.components.UndoButton
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onNoteClick: (String) -> Unit,
    onAddNote: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: NotesListViewModel = hiltViewModel()
) {
    val allNotes by viewModel.filteredNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    // Undo deletion state
    var pendingDeletion by remember { mutableStateOf<PendingDeletion<Note>?>(null) }

    // View mode state (true = list, false = grid)
    var isListView by remember { mutableStateOf(true) }

    // Filter out pending deletion items from display
    val notes = allNotes.filter { note -> pendingDeletion?.item?.id != note.id }

    // Handle system back button - commit pending deletion before leaving
    BackHandler {
        pendingDeletion?.let { deletion ->
            viewModel.deleteNote(deletion.item.id)
        }
        pendingDeletion = null
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notes",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    EInkIconButton(
                        onClick = {
                            // Commit any pending deletion before navigating back
                            pendingDeletion?.let { deletion ->
                                viewModel.deleteNote(deletion.item.id)
                            }
                            pendingDeletion = null
                            onNavigateBack()
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    // Undo button in TopAppBar
                    pendingDeletion?.let { deletion ->
                        UndoButton(
                            onUndo = { pendingDeletion = null },
                            onTimeout = {
                                viewModel.deleteNote(deletion.item.id)
                                pendingDeletion = null
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    EInkIconButton(
                        onClick = { isListView = !isListView },
                        icon = if (isListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = if (isListView) "Grid view" else "List view"
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EInkWhite,
                border = BorderStroke(1.dp, EInkBlack)
            ) {
                Button(
                    onClick = onAddNote,
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
                    Text("New Note")
                }
            }
        },
        containerColor = EInkWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            EInkTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = "Search notes...",
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
                            contentDescription = "Clear"
                        )
                    }
                }
            )

            // Tags filter
            if (allTags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTags.toList()) { tag ->
                        EInkChip(
                            label = tag,
                            selected = selectedTag == tag,
                            onClick = {
                                viewModel.onTagSelected(if (selectedTag == tag) null else tag)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            EInkDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Notes list
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedTag != null)
                                "No notes found" else "No notes yet",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty() && selectedTag == null)
                                "Tap 'New Note' to create one" else "Try a different search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (isListView) {
                    // List view with delete button visible
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteListItem(
                                note = note,
                                onClick = {
                                    // Commit any pending deletion before navigating
                                    pendingDeletion?.let { deletion ->
                                        viewModel.deleteNote(deletion.item.id)
                                    }
                                    pendingDeletion = null
                                    onNoteClick(note.id)
                                },
                                onDelete = {
                                    // Commit previous pending deletion first
                                    pendingDeletion?.let { previousDeletion ->
                                        viewModel.deleteNote(previousDeletion.item.id)
                                    }
                                    // Set new pending deletion
                                    pendingDeletion = PendingDeletion(
                                        item = note,
                                        message = "Note supprimée"
                                    )
                                }
                            )
                        }
                    }
                } else {
                    // Grid view (2 columns)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteGridCard(
                                note = note,
                                onClick = {
                                    // Commit any pending deletion before navigating
                                    pendingDeletion?.let { deletion ->
                                        viewModel.deleteNote(deletion.item.id)
                                    }
                                    pendingDeletion = null
                                    onNoteClick(note.id)
                                },
                                onDelete = {
                                    // Commit previous pending deletion first
                                    pendingDeletion?.let { previousDeletion ->
                                        viewModel.deleteNote(previousDeletion.item.id)
                                    }
                                    // Set new pending deletion
                                    pendingDeletion = PendingDeletion(
                                        item = note,
                                        message = "Note supprimée"
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

/**
 * List view item - shows title, preview, date and delete button on the right
 */
@Composable
private fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    EInkCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Content preview
                if (note.content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.content.take(80).replace("\n", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Date
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            EInkIconButton(
                onClick = onDelete,
                icon = Icons.Default.Delete,
                contentDescription = "Delete"
            )
        }
    }
}

/**
 * Grid view card - fixed height card for 2-column layout with long press for delete
 * Shows title and content preview in small font for overview
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteGridCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box {
        EInkCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp) // Fixed height for uniform grid
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Title
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Content preview - small font for overview
                Text(
                    text = note.content.take(200).replace("\n", " "),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                        lineHeight = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Date at bottom
                Text(
                    text = formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Context menu (long press)
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export / Share") },
                onClick = {
                    showContextMenu = false
                    // Export note as text
                    val shareText = buildString {
                        appendLine(note.title.ifEmpty { "Untitled" })
                        appendLine()
                        appendLine(note.content)
                        if (note.tags.isNotEmpty()) {
                            appendLine()
                            appendLine("Tags: ${note.tags.joinToString(", ")}")
                        }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, note.title.ifEmpty { "Note" })
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Note"))
                },
                leadingIcon = {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showContextMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
