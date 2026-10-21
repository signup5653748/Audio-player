package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.VisualEffectType

@Composable
fun VisualEffectsDialog(
    visualEffectsEnabled: Boolean,
    currentEffect: VisualEffectType,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectEffect: (VisualEffectType) -> Unit,
    onDismiss: () -> Unit
) {
    var previewEffect by remember(currentEffect) { mutableStateOf(currentEffect) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.90f)
            .testTag("visual_effects_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visual Effects & Animations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_effects_dialog_button")
                        .semantics { contentDescription = "Close visual effects dialog" }
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Master Toggle Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (visualEffectsEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleEnabled(!visualEffectsEnabled) }
                        .semantics {
                            role = Role.Switch
                            contentDescription = "Visual effects: " + if (visualEffectsEnabled) "Enabled. Double tap to disable." else "Disabled. Double tap to enable."
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Visual Effects",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (visualEffectsEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (visualEffectsEnabled) "Live procedural animations active on player" else "Showing static album art",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (visualEffectsEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = visualEffectsEnabled,
                            onCheckedChange = { onToggleEnabled(it) },
                            modifier = Modifier.testTag("fx_master_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Featured Live Animated Preview Viewport
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (visualEffectsEnabled) {
                            LiveVisualEffectCanvas(
                                effect = previewEffect,
                                isPlaying = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            StaticAlbumArtwork(modifier = Modifier.fillMaxSize())
                        }

                        // Preview overlay tag
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                            color = Color(0xCC000000),
                            modifier = Modifier.align(Alignment.BottomStart)
                        ) {
                            Text(
                                text = if (visualEffectsEnabled) "Live Preview: ${previewEffect.title}" else "Preview: Static Artwork Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose from 20 Live Dynamic Variations",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2-Column Grid of 20 Effects with Live Mini-Previews
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(VisualEffectType.entries, key = { it.id }) { item ->
                        val isSelected = currentEffect == item

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    previewEffect = item
                                    onSelectEffect(item)
                                    if (!visualEffectsEnabled) {
                                        onToggleEnabled(true)
                                    }
                                }
                                .testTag("fx_item_${item.id}")
                                .semantics {
                                    role = Role.RadioButton
                                    contentDescription = "${item.title}, tag ${item.tag}. " +
                                            if (isSelected) "Currently selected." else "Double tap to apply."
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Live mini animation canvas for this effect
                                LiveVisualEffectCanvas(
                                    effect = item,
                                    isPlaying = true,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = item.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .testTag("apply_effects_button")
                    .semantics { contentDescription = "Apply visual effects and close" }
            ) {
                Text("Done")
            }
        }
    )
}
