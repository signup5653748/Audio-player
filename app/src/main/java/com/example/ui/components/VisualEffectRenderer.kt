package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.VisualEffectType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural live animation canvas rendering 20 distinct dynamic visual effects in real time.
 */
@Composable
fun LiveVisualEffectCanvas(
    effect: VisualEffectType,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fx_transition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isPlaying) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.background(Color(0xFF0A0A12)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (effect) {
                VisualEffectType.CRIMSON_NEON_PULSE -> drawCrimsonNeonPulse(phase, pulse, isPlaying)
                VisualEffectType.RADIAL_SUNBURST_RING -> drawRadialSunburstRing(phase, pulse, isPlaying)
                VisualEffectType.HARMONIC_RIBBON_WAVES -> drawHarmonicRibbonWaves(phase, isPlaying)
                VisualEffectType.CYBER_SYMMETRICAL_WAVE -> drawCyberSymmetricalWave(phase, isPlaying)
                VisualEffectType.CONCENTRIC_SONIC_RINGS -> drawConcentricSonicRings(phase, isPlaying)
                VisualEffectType.SEGMENTED_RAINBOW_EQ -> drawSegmentedRainbowEq(phase, isPlaying)
                VisualEffectType.NEBULA_PARTICLE_FLOW -> drawNebulaParticleFlow(phase, isPlaying)
                VisualEffectType.MINIMALIST_VOICE_PILL -> drawMinimalistVoicePill(phase, isPlaying)
                VisualEffectType.NEON_HEADPHONE_PULSE -> drawNeonHeadphonePulse(phase, pulse, isPlaying)
                VisualEffectType.LAVA_LAMP_FLUID -> drawLavaLampFluid(phase, isPlaying)
                VisualEffectType.ELECTRIC_VORTEX -> drawElectricVortex(phase, isPlaying)
                VisualEffectType.RETRO_SYNTHWAVE_GRID -> drawRetroSynthwaveGrid(phase, isPlaying)
                VisualEffectType.AUDIO_HEARTBEAT_ECG -> drawAudioHeartbeatEcg(phase, isPlaying)
                VisualEffectType.PRISM_MATRIX -> drawPrismMatrix(phase, isPlaying)
                VisualEffectType.STARLIGHT_TWINKLE -> drawStarlightTwinkle(phase, isPlaying)
                VisualEffectType.AURORA_BOREALIS -> drawAuroraBorealis(phase, isPlaying)
                VisualEffectType.HYPERDRIVE_WARP -> drawHyperdriveWarp(phase, isPlaying)
                VisualEffectType.CIRCULAR_LASER_SCOPE -> drawCircularLaserScope(phase, isPlaying)
                VisualEffectType.MOLTEN_FIRE_EMBERS -> drawMoltenFireEmbers(phase, isPlaying)
                VisualEffectType.ZEN_WATER_RIPPLE -> drawZenWaterRipple(phase, isPlaying)
            }
        }

        // Center vinyl disc badge (minimalist & clean)
        Surface(
            shape = CircleShape,
            color = Color(0xCC12121E),
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = effect.primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Static album artwork displayed when Visual Effects are toggled OFF.
 */
@Composable
fun StaticAlbumArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.surfaceVariant
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 20 Procedural Drawing Algorithms
// -------------------------------------------------------------

// 1. Crimson Neon Pulse (Red Ambient Glow & Waveform)
private fun DrawScope.drawCrimsonNeonPulse(phase: Float, pulse: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.minDimension / 2f

    // Ambient Ruby Red Glow Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x99FF1744),
                Color(0x44D50000),
                Color.Transparent
            ),
            center = center,
            radius = maxRadius * pulse
        ),
        radius = maxRadius * pulse,
        center = center
    )

    // Vibrant Red Equalizer Bars
    val barCount = 22
    val barWidth = size.width / (barCount * 1.8f)
    val spacing = barWidth * 0.8f
    val startX = (size.width - (barCount * (barWidth + spacing))) / 2f
    val centerY = size.height / 2f

    for (i in 0 until barCount) {
        val x = startX + i * (barWidth + spacing)
        val factor = if (isPlaying) (sin(phase + i * 0.35) * 0.45 + 0.55).toFloat() else 0.25f
        val barHeight = (size.height * 0.5f) * factor
        val topY = centerY - barHeight / 2f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF5252), Color(0xFFFF1744), Color(0xFFB71C1C)),
                startY = topY,
                endY = topY + barHeight
            ),
            topLeft = Offset(x, topY),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

// 2. Radial Sunburst Ring (360° Neon Rays - image_76d20c27.png)
private fun DrawScope.drawRadialSunburstRing(phase: Float, pulse: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val baseRadius = size.minDimension * 0.24f
    val rayCount = 42

    for (i in 0 until rayCount) {
        val angle = (i.toFloat() / rayCount) * (2f * Math.PI.toFloat())
        val wave = if (isPlaying) (sin(phase * 2f + i * 0.5f) * 0.45f + 0.55f) else 0.25f
        val rayLength = (size.minDimension * 0.22f) * wave * pulse

        val startX = (center.x + cos(angle) * baseRadius).toFloat()
        val startY = (center.y + sin(angle) * baseRadius).toFloat()
        val endX = (center.x + cos(angle) * (baseRadius + rayLength)).toFloat()
        val endY = (center.y + sin(angle) * (baseRadius + rayLength)).toFloat()

        val color = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFF007F)

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }

    // Outer Neon Ring
    drawCircle(
        color = Color(0x6600E5FF),
        radius = baseRadius * 1.05f,
        center = center,
        style = Stroke(width = 3f)
    )
}

// 3. Harmonic Ribbon Waves (image_2eb5e772.png)
private fun DrawScope.drawHarmonicRibbonWaves(phase: Float, isPlaying: Boolean) {
    val ribbonColors = listOf(
        Color(0xFF00E676),
        Color(0xFF00E5FF),
        Color(0xFFFF007F),
        Color(0xFFFFD600)
    )

    ribbonColors.forEachIndexed { index, color ->
        val path = Path()
        val yOffset = size.height * (0.35f + index * 0.1f)
        val amplitude = if (isPlaying) (size.height * 0.14f) else 10f
        val speed = phase + index * 0.7f

        path.moveTo(0f, yOffset)
        var x = 0f
        val step = 12f
        while (x <= size.width) {
            val y = yOffset + sin(speed + (x / size.width) * (2 * Math.PI).toFloat() * 1.5f) * amplitude
            path.lineTo(x, y.toFloat())
            x += step
        }

        drawPath(
            path = path,
            color = color.copy(alpha = 0.85f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

// 4. Cyber Symmetrical Wave (image_951afbdd.png)
private fun DrawScope.drawCyberSymmetricalWave(phase: Float, isPlaying: Boolean) {
    val barCount = 36
    val barWidth = size.width / (barCount * 1.5f)
    val spacing = barWidth * 0.5f
    val startX = (size.width - (barCount * (barWidth + spacing))) / 2f
    val centerY = size.height / 2f

    // Center horizontal beam
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color(0xFFFF9100), Color(0xFFFF1744), Color(0xFF00E5FF))
        ),
        start = Offset(0f, centerY),
        end = Offset(size.width, centerY),
        strokeWidth = 3f
    )

    for (i in 0 until barCount) {
        val x = startX + i * (barWidth + spacing)
        val factor = if (isPlaying) (sin(phase + i * 0.3) * 0.45 + 0.55).toFloat() else 0.2f
        val halfHeight = (size.height * 0.35f) * factor

        val color = when {
            i < barCount / 3 -> Color(0xFFFFAB00)
            i < (barCount * 2) / 3 -> Color(0xFFFF1744)
            else -> Color(0xFF00E5FF)
        }

        drawLine(
            color = color,
            start = Offset(x, centerY - halfHeight),
            end = Offset(x, centerY + halfHeight),
            strokeWidth = barWidth * 0.75f,
            cap = StrokeCap.Round
        )
    }
}

// 5. Concentric Sonic Rings (image_334dc0d1.png)
private fun DrawScope.drawConcentricSonicRings(phase: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.minDimension * 0.45f
    val ringCount = 6

    for (i in 0 until ringCount) {
        val progress = if (isPlaying) ((phase / (2 * Math.PI).toFloat() + (i.toFloat() / ringCount)) % 1f) else (i.toFloat() / ringCount)
        val radius = maxRadius * progress
        val alpha = (1f - progress).coerceIn(0f, 1f) * 0.85f

        drawCircle(
            color = if (i % 2 == 0) Color(0xFF651FFF).copy(alpha = alpha) else Color(0xFF00B0FF).copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = 4f)
        )
    }
}

// 6. Segmented Rainbow EQ (images_c787875d.mp4)
private fun DrawScope.drawSegmentedRainbowEq(phase: Float, isPlaying: Boolean) {
    val colCount = 18
    val blocksPerCol = 14
    val blockWidth = size.width / (colCount * 1.6f)
    val blockHeight = size.height * 0.022f
    val startX = (size.width - (colCount * (blockWidth + 4f))) / 2f
    val baselineY = size.height * 0.65f

    for (c in 0 until colCount) {
        val x = startX + c * (blockWidth + 4f)
        val factor = if (isPlaying) (sin(phase + c * 0.4) * 0.45 + 0.55).toFloat() else 0.3f
        val activeBlocks = (blocksPerCol * factor).toInt()

        for (b in 0 until activeBlocks) {
            val y = baselineY - b * (blockHeight + 3f)
            val color = when {
                b < 6 -> Color(0xFF76FF03)
                b < 10 -> Color(0xFFFFD600)
                else -> Color(0xFFFF1744)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(blockWidth, blockHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Reflection beneath baseline
            if (b < 4) {
                val refY = baselineY + 6f + b * (blockHeight + 3f)
                drawRoundRect(
                    color = color.copy(alpha = 0.3f - b * 0.07f),
                    topLeft = Offset(x, refY),
                    size = Size(blockWidth, blockHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

// 7. Nebula Particle Flow (image_334dc0d1.png)
private fun DrawScope.drawNebulaParticleFlow(phase: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val particleCount = 36

    for (i in 0 until particleCount) {
        val baseAngle = (i.toFloat() / particleCount) * (2f * Math.PI.toFloat())
        val speedFactor = 1.0f + (i % 3) * 0.4f
        val angle = if (isPlaying) baseAngle + phase * speedFactor else baseAngle
        val radius = size.minDimension * (0.15f + ((i * 7) % 30) * 0.01f)

        val x = (center.x + cos(angle) * radius * 1.2f).toFloat()
        val y = (center.y + sin(angle) * radius * 0.8f).toFloat()

        val color = when (i % 3) {
            0 -> Color(0xFFD500F9)
            1 -> Color(0xFF00E5FF)
            else -> Color(0xFF651FFF)
        }
        drawCircle(
            color = color.copy(alpha = 0.85f),
            radius = 4f + (i % 4) * 2f,
            center = Offset(x, y)
        )
    }
}

// 8. Minimalist Voice Pill (images__1__8c2c33cf.mp4)
private fun DrawScope.drawMinimalistVoicePill(phase: Float, isPlaying: Boolean) {
    val colors = listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))
    val pillCount = 4
    val pillWidth = 14f
    val spacing = 22f
    val startX = (size.width - (pillCount * (pillWidth + spacing))) / 2f
    val centerY = size.height / 2f

    colors.forEachIndexed { i, color ->
        val x = startX + i * (pillWidth + spacing)
        val factor = if (isPlaying) (sin(phase * 2.5 + i * 0.8) * 0.45 + 0.55).toFloat() else 0.3f
        val pillHeight = (size.height * 0.4f) * factor
        val topY = centerY - pillHeight / 2f

        drawRoundRect(
            color = color,
            topLeft = Offset(x, topY),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillWidth / 2f, pillWidth / 2f)
        )
    }
}

// 9. Neon Headphone Pulse (images__2__6c900e9b.mp4)
private fun DrawScope.drawNeonHeadphonePulse(phase: Float, pulse: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val headRadius = size.minDimension * 0.3f

    // Headphone Arch
    drawCircle(
        brush = Brush.sweepGradient(listOf(Color(0xFF00E676), Color(0xFFFF4081), Color(0xFF00E676))),
        radius = headRadius,
        center = center,
        style = Stroke(width = 6f)
    )

    // Inner Equalizer Bars
    val barCount = 10
    val barWidth = 8f
    val startX = center.x - (barCount * 14f) / 2f
    for (i in 0 until barCount) {
        val x = startX + i * 14f
        val factor = if (isPlaying) (sin(phase + i * 0.6) * 0.4 + 0.6).toFloat() else 0.25f
        val h = (size.height * 0.2f) * factor * pulse
        drawRoundRect(
            color = if (i % 2 == 0) Color(0xFF00E676) else Color(0xFFFF4081),
            topLeft = Offset(x, center.y - h / 2f),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

// 10. Lava Lamp Fluid
private fun DrawScope.drawLavaLampFluid(phase: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val blobCount = 6

    for (i in 0 until blobCount) {
        val angle = phase + (i.toFloat() / blobCount) * (2f * Math.PI.toFloat())
        val distance = size.minDimension * (0.15f + 0.1f * sin(phase * 1.5f + i).toFloat())
        val x = (center.x + cos(angle) * distance).toFloat()
        val y = (center.y + sin(angle * 1.2f) * distance).toFloat()
        val radius = size.minDimension * (0.14f + 0.04f * cos(phase + i).toFloat())

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD600), Color(0xFFFF3D00), Color.Transparent),
                center = Offset(x, y),
                radius = radius
            ),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

// 11. Electric Vortex
private fun DrawScope.drawElectricVortex(phase: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val spiralTurns = 4
    val steps = 90

    for (turn in 0 until spiralTurns) {
        val path = Path()
        val turnOffset = (turn.toFloat() / spiralTurns) * (2f * Math.PI.toFloat())

        for (s in 0 until steps) {
            val t = (s.toFloat() / steps)
            val angle = phase * 2f + turnOffset + t * 4f * Math.PI.toFloat()
            val r = (size.minDimension * 0.45f) * t
            val x = (center.x + cos(angle) * r).toFloat()
            val y = (center.y + sin(angle) * r).toFloat()

            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val color = if (turn % 2 == 0) Color(0xFF00B0FF) else Color(0xFFD500F9)
        drawPath(path, color = color.copy(alpha = 0.8f), style = Stroke(width = 3.5f))
    }
}

// 12. Retro Synthwave Grid
private fun DrawScope.drawRetroSynthwaveGrid(phase: Float, isPlaying: Boolean) {
    val horizonY = size.height * 0.55f

    // Sun / Sunset Disc
    drawCircle(
        brush = Brush.verticalGradient(listOf(Color(0xFFFF007F), Color(0xFFFFD600))),
        radius = size.minDimension * 0.22f,
        center = Offset(size.width / 2f, horizonY * 0.75f)
    )

    // Perspective floor lines
    val floorLines = 8
    for (i in 0..floorLines) {
        val x = (size.width / floorLines) * i
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.7f),
            start = Offset(size.width / 2f, horizonY),
            end = Offset(x, size.height),
            strokeWidth = 2f
        )
    }

    // Horizontal moving grid lines
    val gridCount = 5
    for (i in 0 until gridCount) {
        val prog = ((phase / (2 * Math.PI).toFloat() + i.toFloat() / gridCount) % 1f)
        val y = horizonY + (size.height - horizonY) * (prog * prog)
        drawLine(
            color = Color(0xFFFF007F).copy(alpha = prog * 0.9f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2f
        )
    }
}

// 13. Audio Heartbeat (ECG)
private fun DrawScope.drawAudioHeartbeatEcg(phase: Float, isPlaying: Boolean) {
    val centerY = size.height / 2f
    val path = Path()
    path.moveTo(0f, centerY)

    val step = 10f
    var x = 0f
    val waveOffset = phase * 80f

    while (x <= size.width) {
        val relX = (x + waveOffset) % size.width
        val spike = if (relX in 120f..180f && isPlaying) {
            when {
                relX < 140f -> -((relX - 120f) * 4f)
                relX < 160f -> ((relX - 140f) * 8f) - 80f
                else -> -((180f - relX) * 4f)
            }
        } else 0f

        path.lineTo(x, centerY + spike)
        x += step
    }

    drawPath(
        path = path,
        color = Color(0xFFFF1744),
        style = Stroke(width = 4f, cap = StrokeCap.Round)
    )
}

// 14. Prism Matrix
private fun DrawScope.drawPrismMatrix(phase: Float, isPlaying: Boolean) {
    val colCount = 14
    val width = size.width / (colCount * 1.5f)
    val startX = (size.width - (colCount * (width + 6f))) / 2f
    val centerY = size.height / 2f

    for (i in 0 until colCount) {
        val x = startX + i * (width + 6f)
        val factor = if (isPlaying) (sin(phase + i * 0.45) * 0.45 + 0.55).toFloat() else 0.25f
        val h = (size.height * 0.45f) * factor

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00E5FF), Color(0xFFFF4081), Color(0xFFE040FB))
            ),
            topLeft = Offset(x, centerY - h / 2f),
            size = Size(width, h),
            cornerRadius = CornerRadius(width / 3f, width / 3f)
        )
    }
}

// 15. Starlight Twinkle
private fun DrawScope.drawStarlightTwinkle(phase: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val nodeCount = 16
    val points = mutableListOf<Offset>()

    for (i in 0 until nodeCount) {
        val angle = (i.toFloat() / nodeCount) * (2f * Math.PI.toFloat()) + phase * 0.4f
        val radius = size.minDimension * (0.2f + 0.15f * sin(phase + i).toFloat())
        val pt = Offset((center.x + cos(angle) * radius).toFloat(), (center.y + sin(angle) * radius).toFloat())
        points.add(pt)

        drawCircle(color = Color.White, radius = 4f, center = pt)
    }

    // Connect constellation lines
    for (i in points.indices) {
        val next = points[(i + 1) % points.size]
        drawLine(color = Color(0x7780D8FF), start = points[i], end = next, strokeWidth = 2f)
    }
}

// 16. Aurora Borealis
private fun DrawScope.drawAuroraBorealis(phase: Float, isPlaying: Boolean) {
    val colors = listOf(Color(0xFF00E676), Color(0xFFAA00FF), Color(0xFF00B0FF))

    colors.forEachIndexed { idx, color ->
        val path = Path()
        val yBase = size.height * (0.35f + idx * 0.12f)
        val amp = if (isPlaying) 35f else 10f
        path.moveTo(0f, yBase)

        var x = 0f
        while (x <= size.width) {
            val y = yBase + sin(phase + idx * 0.8f + (x / size.width) * 4f) * amp
            path.lineTo(x, y.toFloat())
            x += 16f
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()

        drawPath(
            path = path,
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent))
        )
    }
}

// 17. Hyperdrive Warp
private fun DrawScope.drawHyperdriveWarp(phase: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val streakCount = 38

    for (i in 0 until streakCount) {
        val angle = (i.toFloat() / streakCount) * (2f * Math.PI.toFloat())
        val prog = if (isPlaying) ((phase / (2f * Math.PI.toFloat()) + i * 0.1f) % 1f) else (i.toFloat() / streakCount)
        val startR = size.minDimension * 0.1f + (size.minDimension * 0.35f) * (prog * prog)
        val endR = startR + 24f * prog

        drawLine(
            color = Color(0xFF40C4FF).copy(alpha = prog),
            start = Offset((center.x + cos(angle) * startR).toFloat(), (center.y + sin(angle) * startR).toFloat()),
            end = Offset((center.x + cos(angle) * endR).toFloat(), (center.y + sin(angle) * endR).toFloat()),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

// 18. Circular Laser Scope
private fun DrawScope.drawCircularLaserScope(phase: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.4f

    // Scope boundary ring
    drawCircle(color = Color(0x8876FF03), radius = radius, center = center, style = Stroke(width = 3f))
    drawCircle(color = Color(0x4476FF03), radius = radius * 0.6f, center = center, style = Stroke(width = 2f))

    // Rotating scanner laser line
    val angle = phase * 2f
    val endX = (center.x + cos(angle) * radius).toFloat()
    val endY = (center.y + sin(angle) * radius).toFloat()

    drawLine(
        color = Color(0xFF76FF03),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
}

// 19. Molten Fire Embers
private fun DrawScope.drawMoltenFireEmbers(phase: Float, isPlaying: Boolean) {
    val sparkCount = 30
    for (i in 0 until sparkCount) {
        val prog = if (isPlaying) ((phase / (2 * Math.PI).toFloat() + i * 0.08f) % 1f) else (i.toFloat() / sparkCount)
        val y = size.height - size.height * prog
        val x = (size.width * 0.2f) + ((i * 37) % (size.width * 0.6f).toInt()) + sin(phase * 2 + i) * 15f
        val alpha = (1f - prog).coerceIn(0f, 1f)

        drawCircle(
            color = if (i % 2 == 0) Color(0xFFFF6D00).copy(alpha = alpha) else Color(0xFFFF1744).copy(alpha = alpha),
            radius = 3.5f + (i % 3) * 2f,
            center = Offset(x.toFloat(), y)
        )
    }
}

// 20. Zen Water Ripple
private fun DrawScope.drawZenWaterRipple(phase: Float, isPlaying: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.minDimension * 0.42f
    val rippleCount = 5

    for (i in 0 until rippleCount) {
        val prog = if (isPlaying) ((phase / (2 * Math.PI).toFloat() + i.toFloat() / rippleCount) % 1f) else (i.toFloat() / rippleCount)
        val radius = maxRadius * prog
        val alpha = (1f - prog) * 0.75f

        drawCircle(
            color = Color(0xFF64FFDA).copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = 3f)
        )
    }
}
