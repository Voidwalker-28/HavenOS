# PERMISSIONS — HavenOS (M2)

Feature → permission type → when requested → fallback.

| Feature | Permission | Type | When requested | Fallback if denied |
|---|---|---|---|---|
| App catalog + drawer | *(none — `<queries>` MAIN/LAUNCHER + theme-intent visibility declarations, not permissions)* | Manifest declaration | n/a | n/a |
| Weather (current + forecast + city search) | INTERNET | Normal (manifest) | Never prompted; used ONLY for Open-Meteo (`api.open-meteo.com`, `geocoding-api.open-meteo.com` — hard allowlist in `WeatherNet.fetch`) | Turning off Settings → Widgets → Weather network disables all network use |
| Agenda / Calendar widgets, calendar search | READ_CALENDAR | Runtime (dangerous) | From the widget's placeholder or Settings → Permissions & Access → Fix | Widgets show an "enable" placeholder; no calendar data is read |
| Contacts search section | READ_CONTACTS | Runtime (dangerous) | Settings → Search → Contacts, or Permissions & Access → Fix | Contacts section is omitted from search results |
| Apply HavenOS wallpaper to system / lock screen | SET_WALLPAPER | Normal (manifest) | Never prompted; used only by the explicit Apply buttons in Settings → Wallpaper | n/a |
| Notification badges + Now Playing | Notification access (notification listener service) | Special access (system settings screen) | Settings → Icons & Badges / the Now Playing widget's enable prompt | Badges hidden; Now Playing shows its enable prompt — never silently empty |
| Lock gesture (double-tap to lock etc.) | Device admin (force-lock policy only, `device_admin_policies.xml`) | Special (system activation screen) | First use of the lock gesture action | The gesture opens the activation screen instead of locking |
| Most-used sorting / recents | *(none — HavenOS tracks its own launches)* | — | — | Usage-access is NOT requested; a read-only status row in Permissions & Access reports the system usage-access state for transparency |
| Hidden-apps lock | Device credential (Keyguard confirm) | System prompt | Opening the hidden-apps list when "Lock hidden apps" is on | List stays locked |
| Navigation gestures | Accessibility service | Special access (system settings screen) | Settings → Gestures → Navigation gestures status card → Fix button | Feature off; system buttons keep working |
| Navigation gestures (edge strips) | SYSTEM_ALERT_WINDOW (overlay) | Special access (system settings screen) | Settings → Gestures → Navigation gestures status card → Fix button | Gestures do nothing |
| System-wide nav-bar hide | WRITE_SECURE_SETTINGS | Manifest (ADB grant only) | Never requested at runtime — granted only by the user's one-time ADB command `adb shell pm grant app.havenos.launcher android.permission.WRITE_SECURE_SETTINGS`; unlocks the system-wide hide-bar option | In-HavenOS immersive only |

Manifest permissions in the vc3 APK (verified with aapt): INTERNET,
READ_CALENDAR, READ_CONTACTS, SET_WALLPAPER, SYSTEM_ALERT_WINDOW,
WRITE_SECURE_SETTINGS — plus the androidx
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` self-permission. No location
permission: weather location is a manual city, never device GPS.
