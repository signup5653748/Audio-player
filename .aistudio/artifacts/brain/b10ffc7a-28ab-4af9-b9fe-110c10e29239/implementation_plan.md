# Implementation Plan (Revised): Dynamic Live Visual Animations Engine (20 Variations)

Implement a real-time, procedural 60-FPS animated visual effects engine for Smart Music Player with 20 distinct dynamic styles (no static images or flat picture placeholders), live motion gallery selection, master on/off toggling, and complete accessibility.

## User Review & Critical Clarifications

> [!IMPORTANT]
> **Key Revision Confirmed by User**:
> - **Pure Live Motion Animations (No Static Images)**: The visual effects are **not** static images or flat JPEG/PNG backgrounds. They are **100% real-time procedural animations** drawn continuously via Jetpack Compose `Canvas` and `DrawScope` with dynamic math, oscillating frequency waves, jumping equalizer bars, and glowing neon halos.
> - **Toggle Behavior**:
>   - **When Visual Effects are ON**: Replaces the center artwork with the full live procedural animation (dynamic waveforms, radial rays, fire embers, reactive sound bars).
>   - **When Visual Effects are OFF**: Displays the clean static album cover artwork.
> - **Gallery Selection**: Interactive visual gallery where every one of the 20 cards renders an actual **mini live moving animation loop**, giving the user instant visual confirmation of the effect's motion before applying.
> - **Default Effect**: **Crimson Neon Pulse** with fiery red glowing waveforms and an ambient ruby breathing aura.

---

## 1. Overview & Core Concept

- **Live Animation Engine**: Pure Jetpack Compose `Canvas` rendering system calculating real-time geometric transforms, gradient shaders, wave harmonics, and particle positions on every frame.
- **Access Point**: Dedicated entry in Player Mode 3-dot menu named **"Visual Effects & Animations"**, plus a quick shortcut pill `"FX: Crimson Pulse"` directly on the Player screen.

---

## 2. 20 Dynamic Live Animation Variations

Every effect is procedurally coded with unique mathematical motion algorithms (sine harmonics, radial trigonometry, particle physics, Fourier bar simulations):

| # | Effect Name | Motion Algorithm & Visual Dynamics | Color Palette |
|---|-------------|------------------------------------|---------------|
| 1 | **Crimson Neon Pulse** *(Default)* | Multi-harmonic red waveform with pulsating radial bloom and breathing red aura. | Electric Crimson (`#FF1744`), Ruby (`#D50000`), Dark Red (`#880000`) |
| 2 | **Radial Sunburst Ring** | 48 circular rays expanding and contracting radially around a center vinyl disc. | Neon Cyan (`#00E5FF`) & Hot Magenta (`#FF007F`) |
| 3 | **Harmonic Ribbon Waves** | 5 intertwined sinusoidal ribbon waves undulating with phase-shifted frequencies. | Electric Emerald, Violet, Amber, and Cyan (`image_2eb5e772.png`) |
| 4 | **Cyber Symmetrical Wave** | Dual-ended vertical spectrum bars oscillating symmetrically across a glowing horizontal center axis. | Golden Amber, Sunset Rose, Sky Cyan (`image_951afbdd.png`) |
| 5 | **Concentric Sonic Rings** | Continuous concentric wave rings radiating outward from the center like water ripples. | Electric Indigo & Deep Violet (`image_334dc0d1.png`) |
| 6 | **Segmented Rainbow EQ** | 24 LED multi-segment vertical spectrum columns with bottom mirror reflection. | Rainbow Gradient (Green, Yellow, Orange, Red, Blue) |
| 7 | **Nebula Particle Flow** | 40 orbiting light particles with trailing alpha tails drifting in elliptical paths. | Cosmic Purple, Starlight Cyan, Neon Blue |
| 8 | **Minimalist Voice Pill** | Google-style morphing rounded capsule bars and oscillating voice dots. | Google Multi-Color (Coral Red, Amber Yellow, Emerald Green, Cobalt Blue) |
| 9 | **Neon Headphone Pulse** | Glowing neon headphone vector arch with rhythmic equalizer bars dancing between earcups. | Neon Lime (`#00E676`) to Cyber Pink (`#FF4081`) |
| 10 | **Lava Lamp Fluid** | 6 metaball fluid blobs slowly merging, stretching, and floating in continuous motion. | Volcanic Flame (`#FF3D00`) & Deep Gold (`#FFD600`) |
| 11 | **Electric Vortex** | High-speed spinning spiral vortex with crackling lightning arc filaments. | Arc Blue (`#00B0FF`) & Plasma Purple (`#D500F9`) |
| 12 | **Retro Synthwave Grid** | 3D wireframe perspective horizon scrolling forward with animated sunset peaks. | Retro Neon Pink (`#FF007F`) & Twilight Violet (`#4A148C`) |
| 13 | **Audio Heartbeat (ECG)** | Real-time moving cardiogram trace line with rhythmic P-Q-R-S-T medical heartbeat spikes. | Vivid Crimson Heartbeat (`#FF1744`) on Dark Grid |
| 14 | **Prism Matrix** | 3D angled diamond pillars bouncing vertically with shimmering prismatic highlights. | Prismatic Diamond & Holographic Shimmer |
| 15 | **Starlight Twinkle** | Rotating constellation nodes connected by pulsating laser web filaments. | Astral White (`#FFFFFF`) & Celestial Cyan (`#80D8FF`) |
| 16 | **Aurora Borealis** | Smooth layered translucent curtain sheets waving horizontally with fluid turbulence. | Polar Emerald Green (`#00E676`) & Mystic Violet (`#AA00FF`) |
| 17 | **Hyperdrive Warp** | 50 warp-speed star streaks accelerating outwards towards the edges with bass kicks. | Warp White & Ion Blue (`#40C4FF`) |
| 18 | **Circular Laser Scope** | High-tech sci-fi targeting radar sweep with a 360° rotating sweep line and frequency blips. | Tactical Lime Green (`#76FF03`) on Dark Matrix |
| 19 | **Molten Fire Embers** | 35 glowing ember sparks rising upward with realistic heat turbulence and fade. | Ember Orange (`#FF6D00`), Fire Red (`#D50000`), Yellow (`#FFEA00`) |
| 20 | **Zen Water Ripple** | Slow, elegant concentric ripple rings expanding softly with delicate fading alpha. | Aqua Mint (`#64FFDA`) & Morning Mist Blue (`#82B1FF`) |

---

## 3. User Experience & Screen Interaction

### Player Mode Screen

- **When Visual Effects are ON**:
  - The central 240dp card renders the selected **live procedural animation**.
  - Bars bounce, ribbons flow, ripples expand, and halos pulse in real-time.
  - A quick action pill `"FX: [Effect Name] (ON)"` appears below the track info; tapping it opens the Visual Effects Gallery.
- **When Visual Effects are OFF**:
  - The central card displays the standard **static album cover artwork / music note disc**.
  - The quick action pill displays `"FX: OFF"`.

### Visual Effects Gallery Dialog

- **Master Toggle Card**: "Enable Visual Effects" switch with status indicator ("Live animations active on player" vs "Showing static album art").
- **Live Motion Featured Preview**: Large viewport at top showing the currently selected effect animating continuously.
- **2-Column Interactive Grid**:
  - Every card displays an actual **live mini animated canvas preview** of that specific effect in real-time.
  - Card title, descriptive genre/mood tag, and checkmark radio state.
  - Double-tap or click instantly activates the effect.
- **Accessibility**: TalkBack announces the active effect, motion style, and selection state.

---

## 4. Technical Architecture

```
┌────────────────────────────────────────────────────────┐
│                   WavePulseViewModel                   │
│ - visualEffectsEnabled: StateFlow<Boolean> (def: true) │
│ - selectedVisualEffect: StateFlow<VisualEffectType>    │
│ - showVisualEffectsDialog: StateFlow<Boolean>          │
└───────────────────────────┬────────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
┌───────────────────────────┐ ┌───────────────────────────┐
│     PlayerModeScreen      │ │    VisualEffectsDialog    │
│                           │ │                           │
│ if (visualEffectsEnabled) │ │ - Master Toggle Switch    │
│   DynamicEffectCanvas(fx) │ │ - Top Live Preview View   │
│ else                      │ │ - 20 Live Mini Canvases   │
│   StaticAlbumArtwork()    │ │   in 2-column LazyGrid    │
└───────────────────────────┘ └───────────────────────────┘
```

- **Persistence**: Saved via `PreferencesManager.kt` (`KEY_VISUAL_EFFECTS_ENABLED`, `KEY_SELECTED_VISUAL_EFFECT`).

---

## 5. Verification & Delivery Plan

1. **Implement `VisualEffectModels.kt`**: 20 enum entries with names, tags, and theme color palettes.
2. **Implement `VisualEffectRenderer.kt`**: 20 distinct `Canvas` drawing functions utilizing phase/time animations.
3. **Implement `VisualEffectsDialog.kt`**: M3 dialog with master switch, featured live preview, and 2-column live animation card grid.
4. **Integrate into `PlayerModeScreen.kt` & `WavePulseApp.kt`**:
   - Central artwork dynamically switches between live canvas animation and static artwork based on master toggle.
   - 3-dot menu and quick pill integration.
5. **Compile & Build**: Run `compile_applet`, verify 0 build errors, delete old build, and compile new `SmartMusicPlayer.apk`.
