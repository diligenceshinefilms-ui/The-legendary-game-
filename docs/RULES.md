# Legend Racer — Development & Performance Rules

## 1. Frame Budget & Memory Rules (CRITICAL)
1. **Target 60 FPS / 120 FPS**: Frame execution budget is 16.6ms for 60Hz and 8.3ms for 120Hz displays.
2. **Zero Allocation in Draw Loops**: Never instantiate objects, lists, lambdas, or paints inside `Canvas { ... }` or `onDraw`. Reuse cached instances or pre-allocated buffers.
3. **Synchronized Buffer Reads**: Any concurrent buffer access between the physics coroutine (GameEngine) and UI state collectors must be synchronized or managed via thread-safe primitives.
4. **Sensor Listener Lifecycle**: Always unregister `SensorEventListener` in `onDispose` or when pausing/exiting the race screen to avoid battery drain and memory leaks.

---

## 2. Architecture & Code Quality Rules
1. **Never Break the Game Loop**: Do not replace the existing working `GameEngine` or `BikePhysics` calculations without explicit prior approval and a phased migration plan.
2. **Reactive StateFlow Pattern**: Maintain `activeEngine` as `StateFlow<GameEngine?>` to guarantee Compose views react seamlessly to lifecycle transitions and avoid black screens.
3. **Touch Target Size**: All interactive buttons, pedals, and HUD controls must meet or exceed 48dp x 48dp for accessibility.
4. **Clean Verification**: Run `compile_applet` before finishing any task to guarantee zero build failures.
