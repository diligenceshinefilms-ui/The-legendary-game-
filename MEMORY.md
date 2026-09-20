# Legend Racer — Active Engineering Memory

## Current Game State
- **Project**: Legend Racer (Kotlin / Jetpack Compose Android Racing Game)
- **Active Phase**: Phase 4 (Iterative Modification Loop)
- **Completed in this turn**:
  - **Task T1.5**: Dynamic slipstream vacuum drafting force when positioned behind lead vehicles.
  - **Task T1.6**: Multi-tiered audio & haptic feedback triggers (high-frequency near-miss tone with dual-pulse lane-split haptic waveform, rev-limiter RPM handlebar flutter, slipstream lock-on tone/tap, and scrape vs. catastrophic crash rumble).
  - **Task T2.4**: Road surface rain/wet asphalt reflection shaders and dynamic neon underglow rendering under player and AI bikes.
  - **Task T2.5**: High-speed camera FOV warping (anamorphic perspective widening) and radial chromatic aberration during nitro boost with prism split light streaks.
- **Current Core Systems**:
  - Multi-Lane Highway (6 lanes) with traffic spawner & near-miss scoring.
  - Dynamic Slipstream Vacuum Drafting physics with real-time UI/Canvas telemetry.
  - Wet Asphalt Mirror Sheen, glossy reflection light columns, and water surface micro-ripples.
  - Dynamic pulsating Neon Underglow on chassis & ground contact for player superbike and competitor bikes.
  - High-Speed Camera FOV Warping & Radial Chromatic Aberration Lens Dispersion with tunnel vignette.
  - Procedural Audio Synthesizer & Multi-Tier Haptic Feedback Engine.
  - Hardware-accelerated `GameCanvasRenderer` with particle systems.
  - Orientation-aware `MotionTiltSensorManager` with touch-drag fallback.
  - 30-second rolling replay recording & `CinematicReplayScreen` with 5 camera presets.
  - `GameViewModel` reactive `activeEngine` StateFlow integration.

## Key Performance / FPS Risks
- Particle emission spikes during multi-vehicle near-miss chains: bounded particle pool prevents GC churning.
- Replay frame synchronization: `ArrayDeque` thread-safe read/write prevents ConcurrentModificationException during rendering.
- Sensor event listener frequency: `SENSOR_DELAY_GAME` clamped to prevent UI thread starvation.
