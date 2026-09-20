# Legend Racer — Technical Architecture Document

## 1. Architectural Overview
Legend Racer is built as a high-performance native Android application adhering strictly to MVVM (Model-View-ViewModel), reactive state streaming with Kotlin Coroutines/Flow, and hardware-accelerated rendering using Jetpack Compose Canvas.

```
[ Android Activity / Navigation ]
               │
               ▼
   [ Jetpack Compose UI Screens ]
   (RaceScreen, CinematicReplayScreen, GarageScreen, etc.)
               │
               ▼
       [ GameViewModel ] ─── (StateFlow<GameEngine?>)
               │
               ├───────► [ Room Local Database ]
               │         (PlayerEntity, BikeEntity, TrackEntity, LeaderboardEntity)
               │
               └───────► [ GameEngine ]
                           ├── Coroutine Physics Loop (60Hz Fixed Timestep)
                           ├── BikePhysics (Acceleration, Lean, Friction, Nitro)
                           ├── Traffic & Obstacle Spawner (6 Lanes)
                           ├── Collision & Near-Miss Detector
                           ├── MotionTiltSensorManager (Orientation-Aware Remapping)
                           └── 30-Second Rolling Replay Buffer (ArrayDeque<ReplayFrame>)
                                       │
                                       ▼
                       [ GameCanvasRenderer (Compose Canvas) ]
                         ├── Perspective Road Projection
                         ├── Multi-Lane Asphalt & Striping
                         ├── Procedural Horizon & Skyline
                         ├── Superbike Sprites / 3D Vector Geometry
                         └── Particle Systems (Draft, Near-Miss, Sparks)
```

---

## 2. Core Subsystems

### 2.1 Physics Engine (`com.example.game.physics.BikePhysics`)
- Fixed-timestep numerical integration (60Hz / 16.6ms).
- Sub-stepping for collision detection to prevent tunneling through high-speed traffic vehicles.
- Speed, acceleration curves, aerodynamic drag, rolling resistance, and lean-damping equations.

### 2.2 Game Engine Lifecycle (`com.example.game.engine.GameEngine`)
- Manages race state: `READY`, `COUNTDOWN`, `RACING`, `PAUSED`, `FINISHED`, `CRASHED`.
- Thread-safe rolling buffer of `ReplayFrame` instances retaining the last 30,000 milliseconds of race data.
- Prunes stale frames automatically per physics tick without allocating new collections.

### 2.3 Motion Tilt Sensor System (`com.example.game.sensor.MotionTiltSensorManager`)
- Employs `Sensor.TYPE_GAME_ROTATION_VECTOR` and `Sensor.TYPE_ROTATION_VECTOR` with fallback to `TYPE_GRAVITY` and `TYPE_ACCELEROMETER`.
- Automatically calls `SensorManager.remapCoordinateSystem` using dynamic display rotation (`ROTATION_0`, `ROTATION_90`, `ROTATION_180`, `ROTATION_270`).
- Guarantees right side dip = steer right (`+1.0f`), left side dip = steer left (`-1.0f`).
- Includes deadzone, sensitivity scaler, center calibration, and touch-drag fallback for emulators.

### 2.4 Canvas Render Pipeline (`com.example.game.renderer.GameCanvasRenderer`)
- Zero-garbage-collection rendering in Jetpack Compose `Canvas`.
- Reuses Paint and Path instances where applicable; avoids object instantiation inside `onDraw`.
- Perspective projection simulates true 3D road depth, vanishing point scaling, and camera shake.

### 2.5 Local Persistence (`com.example.data`)
- Android Room Database storing player stats, unlocked bikes, upgrades, records, and settings.
- Kotlin Coroutines & Flow provide instant, reactive updates to the UI layer.
