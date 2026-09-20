# Legend Racer — Game Design Document (GDD)

## 1. Executive Summary
- **Game Title**: Legend Racer
- **Genre**: Arcade Highway Superbike Racing
- **Platform**: Android (Jetpack Compose, Kotlin, Hardware-Accelerated Canvas)
- **Target Frame Rate**: 60 FPS (standard) / 120 FPS (high refresh displays)
- **Core Loop**: Race highway tracks -> Execute high-speed overtakes & near misses -> Earn cash & reputation -> Upgrade and unlock superbikes in the Garage -> Compete in Championships, Time Trials, and Daily Challenges -> Review dramatic 30-second cinematic race replays.

---

## 2. Gameplay & Mechanics

### 2.1 Road Geometry & Highway Architecture
- **Multi-Lane System**: 6 distinct traffic lanes (`[-0.72f, -0.44f, -0.16f, 0.16f, 0.44f, 0.72f]`).
- **Procedural Curvature**: Smooth highway curvature, banking, and elevation milestones representing iconic cross-city courses (Neo Tokyo, Cyber City, Sunset Coastal, Desert Run).
- **Turn Advisory System**: Real-time HUD chevron indicators warning players of approaching sharp curves.

### 2.2 Vehicle Dynamics & Physics
- **Acceleration & Top Speed**: Tiered superbike performance classes from entry 250cc sportbikes to 1000cc+ carbon hyperbikes.
- **Leaning & Steering**: Responsive dynamic lean angle physics affecting motorcycle visual perspective and turn radius.
- **Braking & Drifting**: High-traction braking and dynamic deceleration.
- **Nitro Boost**: Rapid nitrous oxide injection generating acceleration spikes, motion blur, and screen-shake effects.
- **Slipstreaming & Draft**: Drafting behind opponent vehicles generates a speed boost and particle draft trails.
- **Close-Call Near Misses**: Overtaking within centimeters of traffic without colliding triggers score multipliers, cash rewards, and neon particle bursts.

### 2.3 Controls
- **Hexagonal Buttons**: On-screen `<<` and `>>` digital steer buttons.
- **Analog Steering Wheel**: Rotational touch wheel with progressive angle lock.
- **Motion Tilt**: Real-time accelerometer & gyroscope orientation-aware sensor steering (tilting right side down turns right, left side down turns left).
- **Virtual Joystick**: 360-degree analog steering nub.

### 2.4 30-Second Cinematic Replay System
- Post-race playback capturing the final 30 seconds of race inputs and telemetry.
- 5 Cinematic Camera Presets:
  1. *Dynamic Director Cut* (auto-switching camera)
  2. *Trackside Sweep*
  3. *Helicopter Follow Cam*
  4. *Action Exhaust Cam*
  5. *Helmet Cockpit View*
- Timeline scrubber, play/pause controls, 0.5x / 1.0x / 2.0x playback speeds, and live steering/telemetry HUD.

---

## 3. Game Modes
1. **Quick Race**: Immediate race on any unlocked highway circuit.
2. **Championship**: Multi-stage tournament series with points standing and trophy rewards.
3. **Time Trial**: Solo lap record hunting against personal ghost benchmarks.
4. **Daily Challenge**: Procedurally selected daily bounty with high reward multipliers.
5. **Practice / Free Ride**: Infinite open highway cruising to master tilt steering and close calls.
