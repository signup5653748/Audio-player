package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FolderItem
import com.example.model.MediaItem
import com.example.player.PlayerState

@Composable
fun FoldersScreen(
    folders: List<FolderItem>,
    selectedFolder: FolderItem?,
    allTracks: List<MediaItem>,
    playerState: PlayerState,
    onSelectFolder: (FolderItem) -> Unit,
    onCloseFolder: () -> Unit,
    onPlayTrack: (MediaItem) -> Unit,
    onPlayAllInFolder: (FolderItem) -> Unit,
    onExcludeFolder: (String) -> Unit,
    onOpenBlacklistDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedFolder != null) {
        // Folder Detail View
        BackHandler { onCloseFolder() }

        val folderTracks = allTracks.filter { it.folderPath == selectedFolder.path }

        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("folder_detail_screen")
        ) {
            // Folder Detail Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCloseFolder,
                        modifier = Modifier
                            .testTag("folder_back_button")
                            .semantics { contentDescription = "Back to folders list" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedFolder.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folderTracks.size} tracks (${selectedFolder.durationFormatted})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onPlayAllInFolder(selectedFolder) },
                        modifier = Modifier
                            .testTag("play_all_in_folder_button")
                            .semantics { contentDescription = "Play all tracks in ${selectedFolder.name}" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All")
                    }
                }
            }

            if (folderTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks in this folder match your current filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(folderTracks, key = { it.id }) { track ->
                        val isCurrent = playerState.currentTrack?.id == track.id
                        val isPlayingCurrent = isCurrent && playerState.isPlaying

                        TrackListItem(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isPlayingCurrent,
                            onClick = { onPlayTrack(track) }
                        )
                        HorizontalDivider(
                            color = DividerDefaults.color.copy(alpha = 0.35f),
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Folders List View
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("folders_screen")
        ) {
            // Folders summary & Blacklist manage button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${folders.size} active folders",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onOpenBlacklistDialog,
                    modifier = Modifier
                        .testTag("manage_blacklist_button")
                        .semantics { contentDescription = "Manage folder blacklist settings" }
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Blacklist Settings")
                }
            }

            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No visible folders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All folders may have been unchecked in Blacklist settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(folders, key = { it.path }) { folder ->
                        FolderListItem(
                            folder = folder,
                            onClick = { onSelectFolder(folder) },
                            onPlayAll = { onPlayAllInFolder(folder) },
                            onExclude = { onExcludeFolder(folder.path) }
                        )
                        HorizontalDivider(
                            color = DividerDefaults.color.copy(alpha = 0.35f),
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderListItem(
    folder: FolderItem,
    onClick: () -> Unit,
    onPlayAll: () -> Unit,
    onExclude: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("folder_item_${folder.name}")
            .semantics {
                role = Role.Button
                contentDescription = "Folder ${folder.name}, contains ${folder.itemCount} tracks: " +
                        "${folder.audioCount} audio, ${folder.videoCount} video. Duration ${folder.durationFormatted}. Tap to open."
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${folder.itemCount} tracks (${folder.audioCount} audio, ${folder.videoCount} video) • ${folder.durationFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = folder.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Quick play all button
        IconButton(
            onClick = onPlayAll,
            modifier = Modifier
                .size(44.dp)
                .testTag("play_folder_${folder.name}")
                .semantics { contentDescription = "Play all tracks in ${folder.name}" }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
