# Legend Racer — Tasks Matrix

## Phase Status Summary
- [x] **Audit & Documentation (Phase 1 & Phase 2)**: Complete.
- [x] **Baseline Environment & Branch Verification (Phase 3)**: Complete.
- [ ] **Feature Iterations & Engine Polish (Phase 4)**: In progress.
- [ ] **Performance, Physics & Security Audit (Phase 5)**: Queued.
- [ ] **Production Build & QA Verification (Phase 6)**: Queued.

---

## Active Task Matrix

### Module 1: Physics & Controls (Engine)
- [x] **T1.1**: Multi-lane highway road geometry (6-lane traffic layout `[-0.72f, ... 0.72f]`).
- [x] **T1.2**: Near-miss overtake particle effects and traffic vehicle dimensional scaling.
- [x] **T1.3**: 30-Second Rolling Replay Buffer in `GameEngine`.
- [x] **T1.4**: Orientation-aware Motion Tilt Sensor remapping (right side dip = turn right, left side dip = turn left).
- [x] **T1.5**: Dynamic slipstream vacuum drafting force when positioned behind lead vehicles.
- [x] **T1.6**: Audio / Haptic feedback triggers on close calls, redline RPM, and collisions.

### Module 2: Visuals & UI / HUD
- [x] **T2.1**: Black screen fix via `StateFlow<GameEngine?>` lifecycle migration.
- [x] **T2.2**: Cinematic Replay Screen with 5 camera modes, scrubber timeline, and telemetry overlay.
- [x] **T2.3**: "WATCH CINEMATIC REPLAY" button on `RaceResultsScreen`.
- [x] **T2.4**: Road surface rain/wet asphalt reflection shaders and neon underglow.
- [x] **T2.5**: High-speed camera FOV warping and chromatic aberration during nitro boost.

### Module 3: Career & Customization
- [x] **T3.1**: Superbike garage with upgrade pipeline (Engine, Tires, Aero, Nitro).
- [x] **T3.2**: Track selection catalog and race modes (Quick Race, Championship, Time Trial, Daily Challenge).
- [ ] **T3.3**: Custom livery editor / neon vinyl accents for player bikes.
