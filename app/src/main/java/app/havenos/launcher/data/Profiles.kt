package app.havenos.launcher.data

import app.havenos.launcher.HavenApp
import app.havenos.launcher.ui.parseSnapshot
import app.havenos.launcher.ui.snapshotJson
import org.json.JSONArray
import org.json.JSONObject

/**
 * A named home profile: one layout snapshot (items + folders + dock + the
 * per-profile settings: look, density, dark mode, wallpaper). The snapshot
 * format is shared with layout-undo snapshots (ui/Grid.kt snapshotJson).
 */
data class Profile(val name: String, val snapshotJson: String)

fun parseProfiles(json: String): List<Profile> = try {
    val arr = JSONArray(json)
    val out = mutableListOf<Profile>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val name = o.optString("name", "")
        if (name.isNotBlank()) out += Profile(name, o.optString("snapshot", ""))
    }
    out
} catch (t: Throwable) {
    emptyList()
}

fun encodeProfiles(profiles: List<Profile>): String {
    val arr = JSONArray()
    for (p in profiles) {
        arr.put(JSONObject().put("name", p.name).put("snapshot", p.snapshotJson))
    }
    return arr.toString()
}

/** Snapshot JSON: Grid.kt items/folders plus dock + per-profile settings. */
fun layoutSnapshotJson(items: List<HomeItem>, folders: List<Folder>, state: SettingsState): String {
    val root = try {
        JSONObject(snapshotJson(items, folders))
    } catch (t: Throwable) {
        JSONObject()
    }
    root.put("dock", JSONArray(state.dock))
    root.put("look", state.look)
    root.put("density", state.density)
    root.put("darkMode", state.darkMode)
    root.put("wallpaperMode", state.wallpaperMode)
    root.put("wpKind", state.wpKind)
    root.put("wpUri", state.wpUri)
    root.put("wpTint", state.wpTint)
    root.put("wpSolid", state.wpSolid)
    return root.toString()
}

/** Apply a layout snapshot (items/folders/dock + whichever settings it carries). */
suspend fun applyLayoutSnapshot(app: HavenApp, json: String) {
    val root = try {
        JSONObject(json)
    } catch (t: Throwable) {
        return
    }
    if (root.has("items") || root.has("folders")) {
        val rawItems = root.optJSONArray("items")
        val (items, folders) = parseSnapshot(json)
        // Never let a corrupt snapshot empty the home (same rule as backups).
        if (rawItems != null && rawItems.length() > 0 && items.isEmpty()) return
        app.db.homeItems().replaceAll(items)
        app.db.folders().replaceAll(folders)
    }
    if (root.has("dock")) {
        val arr = root.optJSONArray("dock")
        val dock = mutableListOf<String>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "")
                if (v.isNotBlank()) dock += v
            }
        }
        app.settings.setDock(dock)
    }
    if (root.has("look")) app.settings.setLook(root.optString("look", "Minimal"))
    if (root.has("density")) app.settings.setDensity(root.optInt("density", 40))
    if (root.has("darkMode")) app.settings.setDarkMode(root.optInt("darkMode", 0))
    if (root.has("wallpaperMode")) app.settings.setWallpaperMode(root.optInt("wallpaperMode", 0))
    if (root.has("wpKind")) app.settings.setWpKind(root.optInt("wpKind", 0))
    if (root.has("wpUri")) app.settings.setWpUri(root.optString("wpUri", ""))
    if (root.has("wpTint")) app.settings.setWpTint(root.optInt("wpTint", 0))
    if (root.has("wpSolid")) app.settings.setWpSolid(root.optInt("wpSolid", 0))
}

/** Capture the live home (DB + settings) as a named profile. */
suspend fun captureProfile(app: HavenApp, state: SettingsState, name: String): Profile {
    val items = app.db.homeItems().all()
    val folders = app.db.folders().all()
    return Profile(name, layoutSnapshotJson(items, folders, state))
}

suspend fun applyProfile(app: HavenApp, profile: Profile) {
    applyLayoutSnapshot(app, profile.snapshotJson)
}

/**
 * Switch to profile [target]: the live state is first saved back into the
 * profile being left (when that profile is a saved one), then the target is
 * applied and marked active. Used by Settings → Profiles and by the
 * profile-schedule effect in HavenRoot.
 */
suspend fun switchProfile(app: HavenApp, state: SettingsState, target: String) {
    val profiles = parseProfiles(state.profilesJson).toMutableList()
    val targetProfile = profiles.firstOrNull { it.name == target } ?: return
    if (state.activeProfile != target) {
        val leaving = profiles.indexOfFirst { it.name == state.activeProfile }
        if (leaving >= 0) {
            profiles[leaving] = captureProfile(app, state, state.activeProfile)
            app.settings.setProfilesJson(encodeProfiles(profiles))
        }
    }
    applyProfile(app, targetProfile)
    app.settings.setActiveProfile(target)
}
