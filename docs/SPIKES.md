# SPIKES — HavenOS

Measured numbers only. Nothing here is invented; entries appear when a measurement
was actually taken (builder prompt §0 / §7).

## M1 (2026-10-20)
- No device measurements taken (no emulator/device in the build sandbox).
  Splash-to-welcome time, intro frame times, glass-blur quality (spike S5:
  system-wallpaper bitmap readability on Redmi Pad 2 / HyperOS), and restricted-settings
  behavior are all pending the device checklist — see DEVICE_CHECKLIST.md and
  UNVERIFIED.md.
- Known device facts carried over from earlier on-device measurement (Redmi Pad 2,
  HyperOS V816, Android 16): wallpaper bitmap read THROWS SecurityException
  (so §8 glass ladder will resolve to Partial or Flat for the system wallpaper),
  cross-window blur was ENABLED with battery saver off. These were measured on
  2026-10-18 with a facts app, not with HavenOS.
