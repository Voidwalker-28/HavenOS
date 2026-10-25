# Licenses — third-party software and data

HavenOS's own code is MIT (see [LICENSE](../LICENSE)). Everything below
is someone else's work, reused under its own license. No code, resources,
strings, icons, or branding from any proprietary product are used in this
repository.

## Libraries

| Artifact | Version | License | Purpose |
|---|---|---|---|
| com.android.tools.build:gradle (AGP) | 8.12.0 | Apache-2.0 | Build |
| org.jetbrains.kotlin (Kotlin + Compose compiler plugin) | 2.1.20 | Apache-2.0 | Language / Compose |
| com.google.devtools.ksp:symbol-processing | 2.1.20-2.0.1 | Apache-2.0 | Room codegen |
| androidx.compose (ui, foundation, animation) | 1.11.0-beta02 | Apache-2.0 | UI toolkit |
| androidx.compose.material3:material3 | 1.5.0-alpha17 | Apache-2.0 | UI components |
| androidx.activity:activity-compose | 1.8.2 | Apache-2.0 | Activity + Compose host |
| androidx.lifecycle (runtime, runtime-compose) | 2.8.3 | Apache-2.0 | Lifecycle |
| androidx.room:room-runtime / room-compiler | 2.7.2 | Apache-2.0 | Layout / folder storage |
| androidx.datastore:datastore-preferences | 1.1.7 | Apache-2.0 | Settings storage |
| androidx.savedstate | 1.4.0 | Apache-2.0 | Saved state (transitive pin) |
| junit:junit (tests only, not shipped) | 4.13.2 | EPL-1.0 | Unit tests |

Transitive dependencies of the above (AndroidX core / annotation /
collection, kotlinx-coroutines, okio, protobuf-javalite, sqlite) are
Apache-2.0 / BSD-family open-source libraries pulled in by the listed
artifacts.

## Data services

| Service | Terms | Use |
|---|---|---|
| Open-Meteo (`api.open-meteo.com`, `geocoding-api.open-meteo.com`) | Free for non-commercial use; weather data licensed CC BY 4.0 — attribution: "Weather data by Open-Meteo.com" (also shown in Settings → About → Licenses). Terms: https://open-meteo.com/en/terms | Weather widget and at-a-glance weather; the app's only network feature |

## Fonts

None bundled. The app uses system font families.

## Assets

- Launcher icon: original vector (arch / doorway mark) drawn in-repo.
- Dock glyphs (`ic_search.xml`, `ic_apps.xml`): original vectors drawn
  in-repo.
- No bundled wallpapers.
