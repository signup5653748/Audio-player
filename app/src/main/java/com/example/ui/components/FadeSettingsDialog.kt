package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun FadeSettingsDialog(
    fadeEffectsEnabled: Boolean,
    currentDurationSeconds: Float,
    onToggleFadeEffects: (Boolean) -> Unit,
    onChangeDuration: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderVal by remember(currentDurationSeconds) { mutableFloatStateOf(currentDurationSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .testTag("fade_settings_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fade Audio Effects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_fade_dialog_button")
                        .semantics { contentDescription = "Close fade effects dialog" }
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Smooth automatic volume transitions across Play, Pause, Next, and Previous track changes to prevent harsh audio cuts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Master Toggle Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (fadeEffectsEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFadeEffects(!fadeEffectsEnabled) }
                        .semantics {
                            role = Role.Switch
                            contentDescription = "Universal fade effects: " + if (fadeEffectsEnabled) "Enabled. Double tap to disable." else "Disabled. Double tap to enable."
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Universal Fade Transitions",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (fadeEffectsEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (fadeEffectsEnabled) "Active on Play, Pause, Next, and Prev" else "Audio cuts immediately without fading",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (fadeEffectsEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = fadeEffectsEnabled,
                            onCheckedChange = { onToggleFadeEffects(it) },
                            modifier = Modifier.testTag("fade_master_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Custom Fade Duration: ${"%.1f".format(sliderVal)} seconds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    onValueChangeFinished = {
                        onChangeDuration(sliderVal)
                    },
                    valueRange = 0.2f..3.0f,
                    steps = 27, // 0.1s steps
                    enabled = fadeEffectsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fade_duration_slider")
                        .semantics {
                            contentDescription = "Fade duration slider"
                            stateDescription = "${"%.1f".format(sliderVal)} seconds"
                        }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Preset Duration Chips
                val presets = listOf(
                    0.5f to "0.5s Snappy",
                    0.8f to "0.8s Default",
                    1.5f to "1.5s Smooth",
                    2.5f to "2.5s Cinematic"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (sec, label) ->
                        val isSelected = Math.abs(sliderVal - sec) < 0.05f
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                sliderVal = sec
                                onChangeDuration(sec)
                            },
                            enabled = fadeEffectsEnabled,
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("fade_preset_${sec}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .testTag("apply_fade_settings_button")
                    .semantics { contentDescription = "Save fade duration and close" }
            ) {
                Text("Done")
            }
        }
    )
}
