package com.music.sttnotes.ui.screens.notes

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.data.i18n.Strings
import com.music.sttnotes.ui.screens.knowledgebase.UiPreferencesEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onNoteClick: (String) -> Unit,
    onAddNote: () -> Unit,
    onNavigateBack: () -> Unit,
    onManageTags: (String) -> Unit = {},
    onManageTagsGlobal: () -> Unit = {},
    viewModel: NotesListViewModel = hiltViewModel()
) {
    val allNotes by viewModel.filteredNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val selectedTagFilters by viewModel.selectedTagFilters.collectAsState()
    val showTagFilter by viewModel.showTagFilter.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val summaryInProgress by viewModel.summaryInProgress.collectAsState()
    val generatedSummary by viewModel.generatedSummary.collectAsState()
    val strings = rememberStrings()

    // Volume scroll support
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val uiPreferences = LocalContext.current.let { context ->
        remember { dagger.hilt.android.EntryPointAccessors.fromApplication<UiPreferencesEntryPoint>(context.applicationContext).uiPreferences() }
    }
    val volumeScrollEnabled by uiPreferences.volumeButtonScrollEnabled.collectAsState(initial = false)
    val volumeScrollDistance by uiPreferences.volumeButtonScrollDistance.collectAsState(initial = 0.8f)

    // Undo deletion state
    var pendingDeletion by remember { mutableStateOf<PendingDeletion<Note>?>(null) }

    // View mode state (true = list, false = grid)
    var isListView by remember { mutableStateOf(true) }

    val volumeHandler = remember(listState, gridState, isListView, coroutineScope, volumeScrollDistance) {
        if (isListView) {
            com.music.sttnotes.ui.components.createLazyListVolumeHandler(
                state = listState,
                scope = coroutineScope,
                scrollDistanceProvider = { volumeScrollDistance }
            )
        } else {
            com.music.sttnotes.ui.components.createLazyGridVolumeHandler(
                state = gridState,
                scope = coroutineScope,
                scrollDistanceProvider = { volumeScrollDistance }
            )
        }
    }

    // Register volume scroll handler with Activity (only if enabled in settings)
    val activity = LocalContext.current as? com.music.sttnotes.MainActivity
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

    // Filter out pending deletion items from display
    val notes = allNotes.filter { note -> pendingDeletion?.item?.id != note.id }
    val archivedNotesFiltered = archivedNotes.filter { note -> pendingDeletion?.item?.id != note.id }

    // Handle system back button - commit pending deletion before leaving, or go back from archive view
    BackHandler {
        if (showArchived) {
            viewModel.toggleShowArchived()
        } else {
            pendingDeletion?.let { deletion ->
                viewModel.deleteNote(deletion.item.id)
            }
            pendingDeletion = null
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showArchived) strings.archive else strings.notes,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    EInkIconButton(
                        onClick = {
                            if (showArchived) {
                                viewModel.toggleShowArchived()
                            } else {
                                // Commit any pending deletion before navigating back
                                pendingDeletion?.let { deletion ->
                                    viewModel.deleteNote(deletion.item.id)
                                }
                                pendingDeletion = null
                                onNavigateBack()
                            }
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
                                viewModel.deleteNote(deletion.item.id)
                                pendingDeletion = null
                            },
                            itemKey = deletion.item.id // Restart countdown when different item deleted
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (showArchived) {
                        // Delete all archived button
                        if (archivedNotesFiltered.isNotEmpty()) {
                            EInkIconButton(
                                onClick = { viewModel.deleteAllArchived() },
                                icon = Icons.Default.DeleteForever,
                                contentDescription = strings.deleteAllArchived
                            )
                        }
                    } else {
                        // Archive button to view archives
                        EInkIconButton(
                            onClick = { viewModel.toggleShowArchived() },
                            icon = Icons.Default.Inventory2,
                            contentDescription = strings.viewArchives
                        )
                        EInkIconButton(
                            onClick = { isListView = !isListView },
                            icon = if (isListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = if (isListView) strings.gridView else strings.listView
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        bottomBar = {
            if (!showArchived) {
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
                        Text(strings.newNote)
                    }
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
            if (showArchived) {
                // Archived notes view
                EInkDivider(modifier = Modifier.padding(horizontal = 16.dp))

                if (archivedNotesFiltered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = EInkBlack.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = strings.noArchivedNotes,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.archivedNotesAppearHere,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(archivedNotesFiltered, key = { it.id }) { note ->
                            ArchivedNoteItem(
                                note = note,
                                onRestore = { viewModel.unarchiveNote(note.id) },
                                onDelete = {
                                    // Commit previous pending deletion first
                                    pendingDeletion?.let { previousDeletion ->
                                        viewModel.deleteNote(previousDeletion.item.id)
                                    }
                                    // Set new pending deletion
                                    pendingDeletion = PendingDeletion(
                                        item = note,
                                        message = strings.notePermanentlyDeleted
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                // Regular notes view
                // Search bar with tag toggle button
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
                    // Tag filter toggle button (only show if there are tags)
                    if (allTags.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        EInkIconButton(
                            onClick = { viewModel.toggleShowTagFilter() },
                            onLongClick = onManageTagsGlobal,
                            icon = Icons.Default.LocalOffer,
                            contentDescription = strings.filterByTags
                        )
                    }
                }

                // Tags filter chips (show when toggled and tags exist)
                if (showTagFilter && allTags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allTags.toList()) { tag ->
                            EInkChip(
                                label = tag,
                                selected = tag in selectedTagFilters,
                                onClick = { viewModel.toggleTagFilter(tag) }
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
                                text = if (searchQuery.isNotEmpty() || selectedTagFilters.isNotEmpty())
                                    strings.noResults else strings.noNotes,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isEmpty() && selectedTagFilters.isEmpty())
                                    strings.createFirstNote else strings.tryAnotherSearch,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    if (isListView) {
                        // List view with delete button visible
                        LazyColumn(
                            state = listState,
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
                                    onArchive = { viewModel.archiveNote(note.id) },
                                    onDelete = {
                                        // Commit previous pending deletion first
                                        pendingDeletion?.let { previousDeletion ->
                                            viewModel.deleteNote(previousDeletion.item.id)
                                        }
                                        // Set new pending deletion
                                        pendingDeletion = PendingDeletion(
                                            item = note,
                                            message = strings.noteDeleted
                                        )
                                    },
                                    onManageTags = { onManageTags(note.id) },
                                    onToggleFavorite = { viewModel.toggleNoteFavorite(note.id) },
                                    onSummarize = { viewModel.generateSummary(note.id) },
                                    isSummarizing = summaryInProgress == note.id
                                )
                            }
                        }
                    } else {
                        // Grid view (2 columns)
                        LazyVerticalGrid(
                            state = gridState,
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
                                    onArchive = { viewModel.archiveNote(note.id) },
                                    onDelete = {
                                        // Commit previous pending deletion first
                                        pendingDeletion?.let { previousDeletion ->
                                            viewModel.deleteNote(previousDeletion.item.id)
                                        }
                                        // Set new pending deletion
                                        pendingDeletion = PendingDeletion(
                                            item = note,
                                            message = strings.noteDeleted
                                        )
                                    },
                                    onManageTags = { onManageTags(note.id) },
                                    onToggleFavorite = { viewModel.toggleNoteFavorite(note.id) },
                                    onSummarize = { viewModel.generateSummary(note.id) },
                                    isSummarizing = summaryInProgress == note.id
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * List view item - shows title, preview, date and archive/delete buttons on the right
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onManageTags: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onSummarize: () -> Unit = {},
    isSummarizing: Boolean = false
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
                        text = note.title.ifEmpty { strings.untitled },
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

                    // Tags display
                    if (note.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Gray separator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(EInkGrayMedium.copy(alpha = 0.3f))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tags in styled chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            note.tags.forEach { tag ->
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

                Spacer(modifier = Modifier.width(8.dp))

                // Favorite star icon
                if (note.isFavorite) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = strings.favorites,
                        modifier = Modifier.size(18.dp),
                        tint = EInkBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Delete button
                EInkIconButton(
                    onClick = onDelete,
                    icon = Icons.Default.Delete,
                    contentDescription = strings.delete
                )
            }
        }

        // Context menu (long press) for archive
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (note.isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                onClick = {
                    showContextMenu = false
                    onToggleFavorite()
                },
                leadingIcon = {
                    Icon(
                        if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(strings.manageTags) },
                onClick = {
                    showContextMenu = false
                    onManageTags()
                },
                leadingIcon = {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(20.dp))
                }
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
                leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(20.dp)) },
                enabled = !isSummarizing && note.content.isNotBlank()
            )
            DropdownMenuItem(
                text = { Text(strings.archive) },
                onClick = {
                    showContextMenu = false
                    onArchive()
                },
                leadingIcon = {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

/**
 * Grid view card - fixed height card for 2-column layout with long press for archive/delete
 * Shows title and content preview in small font for overview
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteGridCard(
    note: Note,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onManageTags: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onSummarize: () -> Unit = {},
    isSummarizing: Boolean = false
) {
    val strings = rememberStrings()
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
                    text = note.title.ifEmpty { strings.untitled },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Content preview - small font for overview
                Text(
                    text = note.content.take(400).replace("\n", " "),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                        lineHeight = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 8,
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

                // Tags display (compact version for grid)
                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        note.tags.take(2).forEach { tag ->  // Limit to 2 tags for grid view
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EInkWhite,
                                border = BorderStroke(1.dp, EInkGrayMedium.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = androidx.compose.ui.unit.TextUnit(7f, androidx.compose.ui.unit.TextUnitType.Sp)
                                    ),
                                    color = EInkGrayMedium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (note.tags.size > 2) {
                            Text(
                                text = "+${note.tags.size - 2}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = androidx.compose.ui.unit.TextUnit(7f, androidx.compose.ui.unit.TextUnitType.Sp)
                                ),
                                color = EInkGrayMedium
                            )
                        }
                    }
                }
            }
        }

        // Context menu (long press)
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (note.isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                onClick = {
                    showContextMenu = false
                    onToggleFavorite()
                },
                leadingIcon = {
                    Icon(
                        if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(strings.manageTags) },
                onClick = {
                    showContextMenu = false
                    onManageTags()
                },
                leadingIcon = {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(20.dp))
                }
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
                leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(20.dp)) },
                enabled = !isSummarizing && note.content.isNotBlank()
            )
            DropdownMenuItem(
                text = { Text(strings.archive) },
                onClick = {
                    showContextMenu = false
                    onArchive()
                },
                leadingIcon = {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text(strings.export) },
                onClick = {
                    showContextMenu = false
                    // Export note as text
                    val shareText = buildString {
                        appendLine(note.title.ifEmpty { strings.untitled })
                        appendLine()
                        appendLine(note.content)
                        if (note.tags.isNotEmpty()) {
                            appendLine()
                            appendLine("Tags: ${note.tags.joinToString(", ")}")
                        }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, note.title.ifEmpty { strings.note })
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, strings.exportNote))
                },
                leadingIcon = {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text(strings.delete) },
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

/**
 * Archived note item - shows title, date, and restore/delete actions on long press
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArchivedNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = rememberStrings()
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        EInkCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showContextMenu = true }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Archive icon
                Icon(
                    Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = EInkBlack.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title
                    Text(
                        text = note.title.ifEmpty { strings.untitled },
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
            }
        }

        // Context menu (long press) for restore/delete
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.restore) },
                onClick = {
                    showContextMenu = false
                    onRestore()
                },
                leadingIcon = {
                    Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text(strings.deletePermantently) },
                onClick = {
                    showContextMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
