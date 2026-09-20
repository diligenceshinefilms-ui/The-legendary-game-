# Legend Racer — UI / UX & Design System

## 1. Visual Theme: "Carbon Cyberpunk"
Legend Racer features a sleek, high-contrast, dark neon racing aesthetic designed for readability at high speeds (200+ KM/H) under variable lighting conditions.

### Palette
- **Canvas Background**: `GalaxyVoid` (`#070913`), `CarbonDark` (`#0C0E17`)
- **Card / Surface**: `CarbonCard` (`#141724`), `CarbonSurface` (`#1D2133`)
- **Borders & Framing**: `CarbonCardBorder` (`#2A2E45`)
- **Primary Action / Speed**: `NeonCyan` (`#00E5FF`)
- **Nitro / Warning / Intensity**: `NeonOrange` (`#FF5722`), `ElectricYellow` (`#FFD600`)
- **High-Tier / Slipstream**: `NitroPurple` (`#D500F9`)
- **Text & Accents**: `TextWhite` (`#FFFFFF`), `TextGray` (`#9E9E9E`), `TextMuted` (`#616161`)

---

## 2. In-Game HUD Layout
1. **Top Center**: Floating 3D Highway Checkpoint Banner (`>>> [ CHECKPOINT 022 km ]`), Lap Counter, and Procedural Curve Advisory Chevrons.
2. **Top Left / Right**: Tachometer, Gear Indicator, KM/H Speedometer, Elapsed Lap Time, Cash Counter.
3. **Bottom Left**: Steering Controls (Hexagonal Digital Buttons, Rotational Wheel, or Motion Tilt Spirit Level Gauge).
4. **Bottom Right**: Throttle Pedal, High-Friction Brake Pedal, Nitro Ignition Button.
5. **Center Screen**: Near-miss combo banners (`CLOSE CALL! +500 PTS`), Draft particle trails, asphalt sparks.

---

## 3. Post-Race Cinematic Replay Interface
- **Letterbox Bars**: 16:9 cinematic bars with live `REC 30S CINEMATIC REPLAY` indicator.
- **Top Bar**: Exit button and real-time telemetry badge showing KM/H and active steer angle.
- **Bottom HUD Card**:
  - 30-Second Scrubber timeline slider with sub-second timestamps.
  - Camera mode selector pills (Director, Trackside, Heli, Action, Cockpit).
  - Play / Pause and Speed Multipliers (0.5x, 1.0x, 2.0x).
