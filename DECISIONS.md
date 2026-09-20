# Legend Racer — Architecture Decision Records (ADR)

## ADR 001: 60Hz Fixed Timestep Physics Loop
- **Context**: In dynamic framerate environments (e.g. 60Hz vs 90Hz vs 120Hz displays), physics calculated with variable delta-time cause collision tunneling and inconsistent top speed trajectories.
- **Decision**: Run `GameEngine` physics updates with a fixed 16.6ms timestep (60Hz) accumulator.
- **Consequences**: Deterministic physics across all device models; smooth and reproducible replay recordings.

## ADR 002: Reactive StateFlow for GameEngine Lifecycle
- **Context**: Passing a nullable raw reference for `activeEngine` between ViewModel and NavHost composables resulted in transient race conditions (black screen bug) during race initialization and restart.
- **Decision**: Wrap `activeEngine` in `StateFlow<GameEngine?>` and collect with lifecycle in `MainActivity`. Add a dark loading fallback when transitioning between race states.
- **Consequences**: Eliminates black screen bugs; guarantees deterministic screen rendering.

## ADR 003: 30-Second Rolling Replay Buffer
- **Context**: Players want to view their race highlights from cinematic angles post-finish line. Storing whole races causes unbounded memory growth.
- **Decision**: Maintain a synchronized `ArrayDeque<ReplayFrame>` capped at 30,000 milliseconds of telemetry data (`timestampMs`, `posX`, `speedKmh`, `steer`, `nitro`, `leanAngleRad`).
- **Consequences**: Bounded memory footprint (~1,800 frames maximum), instant playback initiation without serialization delays.

## ADR 004: Display-Orientation Sensor Remapping
- **Context**: Android orientation sensors report roll in device coordinate space rather than screen coordinates. Tilting the device in Landscape vs Portrait would yield inverted or orthogonal steering.
- **Decision**: Use `SensorManager.remapCoordinateSystem` querying `Display.rotation` (`ROTATION_0`, `ROTATION_90`, `ROTATION_180`, `ROTATION_270`).
- **Consequences**: Right side down consistently produces positive right steering (`+1.0f`), left side down produces negative left steering (`-1.0f`) across all device orientations.
