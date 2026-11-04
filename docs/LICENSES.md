# LICENSES — HavenOS

Everything reused, with its license. HavenOS code is original; no code, resources,
strings, icons or branding from any proprietary launcher (Nova, Niagara, Smart Launcher,
One UI, Apple) were used — those products were studied for feature *behavior* only via
the REA analysis notes (see ASSUMPTIONS.md).

## Libraries (all fetched artifacts, recorded per prompt §2)

| Artifact | Version | License | Why |
|---|---|---|---|
| com.android.tools.build:gradle (AGP) | 8.12.0 | Apache-2.0 | Build |
| org.jetbrains.kotlin (Kotlin + Compose compiler plugin) | 2.1.20 | Apache-2.0 | Language / Compose |
| com.google.devtools.ksp:symbol-processing | 2.1.20-2.0.1 | Apache-2.0 | Room codegen |
| androidx.compose (ui, foundation, animation) | 1.11.0-beta02 | Apache-2.0 | UI toolkit |
| androidx.compose.material3:material3 | 1.5.0-alpha17 | Apache-2.0 | UI components |
| androidx.activity:activity-compose | 1.8.2 | Apache-2.0 | Activity + Compose host |
| androidx.lifecycle (runtime, runtime-compose) | 2.8.3 | Apache-2.0 | Lifecycle |
| androidx.room:room-runtime / room-compiler | 2.7.2 | Apache-2.0 | Layout/folder storage |
| androidx.datastore:datastore-preferences | 1.1.7 | Apache-2.0 | Settings storage |
| androidx.savedstate | 1.4.0 | Apache-2.0 | Saved state (transitive pin) |
| junit:junit (tests only, not shipped) | 4.13.2 | EPL-1.0 | Unit tests |

Transitive dependencies of the above (androidx core/annotation/collection,
kotlinx-coroutines, okio, protobuf-javalite, sqlite) are Apache-2.0 / BSD-family
open-source libraries pulled by the listed artifacts.

## Data services
| Service | Terms | Use |
|---|---|---|
| Open-Meteo (api.open-meteo.com, geocoding-api.open-meteo.com) | Free for non-commercial use; weather data licensed CC BY 4.0 — attribution: "Weather data by Open-Meteo.com" (also shown in Settings → About → Licenses). Terms checked 2026-10-20: https://open-meteo.com/en/terms | Weather widget + at-a-glance weather; the app's only network feature |

## Fonts
None bundled (system font; the Serif/Monospace choices map to platform font families). Any future bundled font will be SIL OFL and listed here.

## Assets
- Launcher icon: original vector (arch/doorway mark) drawn in-repo — see BRAND.md.
- Dock glyphs `ic_search.xml`, `ic_apps.xml`: original vectors drawn in-repo.
- No bundled wallpapers in M1.
