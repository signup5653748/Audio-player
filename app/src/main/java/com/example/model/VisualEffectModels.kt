package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * 20 distinct procedural live visual effect and animation styles.
 */
enum class VisualEffectType(
    val id: Int,
    val title: String,
    val tag: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    CRIMSON_NEON_PULSE(
        id = 0,
        title = "Crimson Neon Pulse",
        tag = "Red Ambient",
        primaryColor = Color(0xFFFF1744),
        secondaryColor = Color(0xFFD50000),
        accentColor = Color(0xFFFF5252)
    ),
    RADIAL_SUNBURST_RING(
        id = 1,
        title = "Radial Sunburst Ring",
        tag = "360° Neon Rays",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFFF007F),
        accentColor = Color(0xFF7C4DFF)
    ),
    HARMONIC_RIBBON_WAVES(
        id = 2,
        title = "Harmonic Ribbon Waves",
        tag = "Multi-Wave Flow",
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFF2979FF),
        accentColor = Color(0xFFFFD600)
    ),
    CYBER_SYMMETRICAL_WAVE(
        id = 3,
        title = "Cyber Symmetrical Wave",
        tag = "Dual Spectrum",
        primaryColor = Color(0xFFFF9100),
        secondaryColor = Color(0xFFFF1744),
        accentColor = Color(0xFF00E5FF)
    ),
    CONCENTRIC_SONIC_RINGS(
        id = 4,
        title = "Concentric Sonic Rings",
        tag = "Ripple Echo",
        primaryColor = Color(0xFF651FFF),
        secondaryColor = Color(0xFF00B0FF),
        accentColor = Color(0xFFE040FB)
    ),
    SEGMENTED_RAINBOW_EQ(
        id = 5,
        title = "Segmented Rainbow EQ",
        tag = "LED Mirror Block",
        primaryColor = Color(0xFF76FF03),
        secondaryColor = Color(0xFFFFD600),
        accentColor = Color(0xFFFF1744)
    ),
    NEBULA_PARTICLE_FLOW(
        id = 6,
        title = "Nebula Particle Flow",
        tag = "Cosmic Orbit",
        primaryColor = Color(0xFFD500F9),
        secondaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF651FFF)
    ),
    MINIMALIST_VOICE_PILL(
        id = 7,
        title = "Minimalist Voice Pill",
        tag = "Morphing Capsule",
        primaryColor = Color(0xFF4285F4),
        secondaryColor = Color(0xFFEA4335),
        accentColor = Color(0xFF34A853)
    ),
    NEON_HEADPHONE_PULSE(
        id = 8,
        title = "Neon Headphone Pulse",
        tag = "Acoustic Aura",
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFFFF4081),
        accentColor = Color(0xFF7C4DFF)
    ),
    LAVA_LAMP_FLUID(
        id = 9,
        title = "Lava Lamp Fluid",
        tag = "Metaball Molten",
        primaryColor = Color(0xFFFF3D00),
        secondaryColor = Color(0xFFFFD600),
        accentColor = Color(0xFFFF6D00)
    ),
    ELECTRIC_VORTEX(
        id = 10,
        title = "Electric Vortex",
        tag = "Lightning Spiral",
        primaryColor = Color(0xFF00B0FF),
        secondaryColor = Color(0xFFD500F9),
        accentColor = Color(0xFF18FFFF)
    ),
    RETRO_SYNTHWAVE_GRID(
        id = 11,
        title = "Retro Synthwave Grid",
        tag = "80s Cyberpunk",
        primaryColor = Color(0xFFFF007F),
        secondaryColor = Color(0xFF4A148C),
        accentColor = Color(0xFF00E5FF)
    ),
    AUDIO_HEARTBEAT_ECG(
        id = 12,
        title = "Audio Heartbeat (ECG)",
        tag = "Cardiac Trace",
        primaryColor = Color(0xFFFF1744),
        secondaryColor = Color(0xFF76FF03),
        accentColor = Color(0xFFFF5252)
    ),
    PRISM_MATRIX(
        id = 13,
        title = "Prism Matrix",
        tag = "3D Crystal Columns",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFFF4081),
        accentColor = Color(0xFFE040FB)
    ),
    STARLIGHT_TWINKLE(
        id = 14,
        title = "Starlight Twinkle",
        tag = "Constellation Web",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFF80D8FF),
        accentColor = Color(0xFFFFAB40)
    ),
    AURORA_BOREALIS(
        id = 15,
        title = "Aurora Borealis",
        tag = "Polar Curtains",
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFFAA00FF),
        accentColor = Color(0xFF00B0FF)
    ),
    HYPERDRIVE_WARP(
        id = 16,
        title = "Hyperdrive Warp",
        tag = "Starfield Kick",
        primaryColor = Color(0xFF40C4FF),
        secondaryColor = Color(0xFFFFFFFF),
        accentColor = Color(0xFF7C4DFF)
    ),
    CIRCULAR_LASER_SCOPE(
        id = 17,
        title = "Circular Laser Scope",
        tag = "Sci-Fi Radar",
        primaryColor = Color(0xFF76FF03),
        secondaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF64DD17)
    ),
    MOLTEN_FIRE_EMBERS(
        id = 18,
        title = "Molten Fire Embers",
        tag = "Rising Sparks",
        primaryColor = Color(0xFFFF6D00),
        secondaryColor = Color(0xFFD50000),
        accentColor = Color(0xFFFFEA00)
    ),
    ZEN_WATER_RIPPLE(
        id = 19,
        title = "Zen Water Ripple",
        tag = "Minimalist Echo",
        primaryColor = Color(0xFF64FFDA),
        secondaryColor = Color(0xFF82B1FF),
        accentColor = Color(0xFFB388FF)
    );

    companion object {
        fun fromId(id: Int): VisualEffectType {
            return entries.firstOrNull { it.id == id } ?: CRIMSON_NEON_PULSE
        }
    }
}
