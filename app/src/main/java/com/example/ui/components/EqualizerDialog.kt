package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun EqualizerDialog(
    currentPreset: Int,
    bassBoost: Int,
    virtualizer: Int,
    onSelectPreset: (Short) -> Unit,
    onChangeBassBoost: (Short) -> Unit,
    onChangeVirtualizer: (Short) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember(currentPreset) { mutableIntStateOf(currentPreset) }
    var bassVal by remember(bassBoost) { mutableFloatStateOf(bassBoost.toFloat()) }
    var virtVal by remember(virtualizer) { mutableFloatStateOf(virtualizer.toFloat()) }

    val presets = listOf(
        "Flat", "Bass Boost", "Rock", "Pop", "Vocal", "Classical", "Electronic", "Jazz"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .testTag("equalizer_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Equalizer & Sound",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_eq_dialog_button")
                        .semantics { contentDescription = "Close equalizer" }
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Acoustic Profiles",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Presets wrap
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0..3) {
                            FilterChip(
                                selected = selectedPreset == i,
                                onClick = {
                                    selectedPreset = i
                                    onSelectPreset(i.toShort())
                                },
                                label = { Text(presets[i], style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_preset_$i")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 4..7) {
                            FilterChip(
                                selected = selectedPreset == i,
                                onClick = {
                                    selectedPreset = i
                                    onSelectPreset(i.toShort())
                                },
                                label = { Text(presets[i], style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_preset_$i")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bass Boost Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bass Boost",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${(bassVal / 10).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = bassVal,
                    onValueChange = { bassVal = it },
                    onValueChangeFinished = { onChangeBassBoost(bassVal.toInt().toShort()) },
                    valueRange = 0f..1000f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bass_boost_slider")
                        .semantics {
                            contentDescription = "Bass boost intensity"
                            stateDescription = "${(bassVal / 10).toInt()} percent"
                        }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Virtualizer / Spatial 3D Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SpatialAudio,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Virtualizer (3D Audio)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${(virtVal / 10).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = virtVal,
                    onValueChange = { virtVal = it },
                    onValueChangeFinished = { onChangeVirtualizer(virtVal.toInt().toShort()) },
                    valueRange = 0f..1000f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("virtualizer_slider")
                        .semantics {
                            contentDescription = "Virtualizer 3D audio depth"
                            stateDescription = "${(virtVal / 10).toInt()} percent"
                        }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .testTag("apply_eq_button")
                    .semantics { contentDescription = "Apply equalizer and close" }
            ) {
                Text("Done")
            }
        }
    )
}
