package com.music.sttnotes.ui.screens.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.ui.components.EInkButton
import com.music.sttnotes.ui.components.EInkCard
import com.music.sttnotes.ui.components.EInkIconButton
import com.music.sttnotes.ui.screens.home.DashboardViewModel
import com.music.sttnotes.ui.screens.home.FavoriteFilter
import com.music.sttnotes.ui.screens.home.FavoriteItem
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit,
    onConversationClick: (String) -> Unit,
    onKbFileClick: (String, String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val strings = rememberStrings()
    val filteredFavorites = remember(state.favoriteItems, state.favoriteFilter) {
        viewModel.getFilteredFavorites()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.favorites,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    EInkIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EInkWhite,
                    titleContentColor = EInkBlack
                ),
                windowInsets = WindowInsets(0.dp)
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
            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EInkButton(
                    onClick = { viewModel.setFavoriteFilter(FavoriteFilter.ALL) },
                    modifier = Modifier.weight(1f),
                    filled = state.favoriteFilter == FavoriteFilter.ALL
                ) {
                    Text(strings.allFavorites)
                }
                EInkButton(
                    onClick = { viewModel.setFavoriteFilter(FavoriteFilter.NOTES) },
                    modifier = Modifier.weight(1f),
                    filled = state.favoriteFilter == FavoriteFilter.NOTES
                ) {
                    Text(strings.filterNotes)
                }
                EInkButton(
                    onClick = { viewModel.setFavoriteFilter(FavoriteFilter.KB) },
                    modifier = Modifier.weight(1f),
                    filled = state.favoriteFilter == FavoriteFilter.KB
                ) {
                    Text(strings.filterKb)
                }
                EInkButton(
                    onClick = { viewModel.setFavoriteFilter(FavoriteFilter.CHAT) },
                    modifier = Modifier.weight(1f),
                    filled = state.favoriteFilter == FavoriteFilter.CHAT
                ) {
                    Text(strings.filterChat)
                }
            }

            // Favorites List
            if (filteredFavorites.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = strings.noFavoritesYet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = EInkGrayMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFavorites, key = { item ->
                        when (item) {
                            is FavoriteItem.NoteItem -> "note_${item.note.id}"
                            is FavoriteItem.ConversationItem -> "conv_${item.conversation.id}"
                            is FavoriteItem.KbItem -> "kb_${item.folder}_${item.filename}"
                        }
                    }) { item ->
                        when (item) {
                            is FavoriteItem.NoteItem -> {
                                EInkCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { onNoteClick(item.note.id) },
                                            onLongClick = { viewModel.toggleNoteFavorite(item.note.id) }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = EInkGrayMedium
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.note.title.ifEmpty { strings.untitled },
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = EInkBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (item.note.tags.isNotEmpty()) {
                                                    Text(
                                                        text = item.note.tags.take(3).joinToString(", "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EInkGrayMedium,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = strings.favorites,
                                            modifier = Modifier.size(18.dp),
                                            tint = EInkBlack
                                        )
                                    }
                                }
                            }
                            is FavoriteItem.ConversationItem -> {
                                EInkCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { onConversationClick(item.conversation.id) },
                                            onLongClick = { viewModel.toggleConversationFavorite(item.conversation.id) }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.SmartToy,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = EInkGrayMedium
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.conversation.title,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = EInkBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val preview = item.conversation.getLastResponsePreview(40)
                                                if (preview.isNotEmpty()) {
                                                    Text(
                                                        text = preview,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EInkGrayMedium,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = strings.favorites,
                                            modifier = Modifier.size(18.dp),
                                            tint = EInkBlack
                                        )
                                    }
                                }
                            }
                            is FavoriteItem.KbItem -> {
                                EInkCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { onKbFileClick(item.folder, item.filename) },
                                            onLongClick = { viewModel.toggleFileFavorite(item.folder, item.filename) }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.LibraryBooks,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = EInkGrayMedium
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.filename.removeSuffix(".md"),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = EInkBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.folder,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EInkGrayMedium,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = strings.favorites,
                                            modifier = Modifier.size(18.dp),
                                            tint = EInkBlack
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
