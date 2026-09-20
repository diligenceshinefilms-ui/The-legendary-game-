# Legend Racer — Quality Assurance & Test Plan

## 1. Automated & Build Checks
- [ ] `./gradlew compileDebugKotlin` / `compile_applet`: Zero compilation errors.
- [ ] Unit & Robolectric test suite execution: `gradle :app:testDebugUnitTest`.

## 2. Core Game Loop & Physics Verification
- [ ] **Frame Rate Stability**: Continuous 60 FPS verified across 3+ laps without hitching or garbage collection pauses.
- [ ] **Collision Detection**: Accurate vehicle bounding boxes on all 6 highway lanes without clipping or false positives.
- [ ] **Near-Miss Detection**: Close proximity overtakes correctly award combo multipliers and trigger neon particle bursts.
- [ ] **Nitro Dynamics**: Boost activation properly increases acceleration, engages visual particle exhaust, and depletes nitrous bar.

## 3. Controls & Sensor Matrix
- [ ] **Motion Tilt (Landscape Left / 90°)**: Tilting right edge down steers RIGHT (`+1.0f`); tilting left edge down steers LEFT (`-1.0f`).
- [ ] **Motion Tilt (Landscape Right / 270°)**: Inverted landscape maintains correct right=right, left=left steering.
- [ ] **Motion Tilt (Portrait / 0°)**: Portrait tilt maintains intuitive directional steering.
- [ ] **Touch Drag Fallback**: Dragging on the spirit level gauge allows manual steer testing without physical accelerometer.
- [ ] **Digital Hex Buttons**: Instant response on tap/hold without input delay.
- [ ] **Rotational Wheel & Joystick**: 360-degree input mapping smoothly translates to vehicle lean angle.

## 4. Replay & Visual Systems
- [ ] **30-Second Buffer**: Buffer strictly retains the last 30 seconds of race data without memory creep.
- [ ] **Cinematic Replay Screen**: All 5 camera presets (Director, Trackside, Heli, Action, Cockpit) render correctly.
- [ ] **Timeline Scrubber**: Scrubbing backwards and forwards accurately visualizes historical vehicle positions and telemetry.
- [ ] **Navigation Stability**: Transitioning from Race -> Race Results -> Replay -> Home produces zero black screens.
