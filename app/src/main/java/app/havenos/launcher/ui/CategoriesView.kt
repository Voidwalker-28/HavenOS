package app.havenos.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.AppRepository
import app.havenos.launcher.data.categoryIdOf
import app.havenos.launcher.data.heuristicCategoryForPackage
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import org.json.JSONObject

/**
 * M7 Wave 1 (P1) — Library Categories tab.
 *
 * NEW FILE ONLY. The coordinator renders this in the Library tab slot
 * (the Library itself, its search field and its other tabs stay untouched
 * here). Categories are computed locally from the installed catalog —
 * no network, no account, nothing leaves the device.
 *
 * Settings key referenced BY NAME only (the coordinator owns the store
 * entry; this file reads none of it directly):
 * - `categoryOverridesJson` (String, default ""): JSON of the form
 *   {"moves": {appKey: categoryId}, "names": {categoryId: customName}}.
 *   Moves change one app's category; names rename a whole category.
 *   The coordinator persists whatever [categoryOverridesWithMove] or
 *   [categoryOverridesWithRename] returns.
 *
 * Precedence for one app's category (highest first):
 * 1. a user move in the overrides,
 * 2. the system app category (ApplicationInfo category, when set),
 * 3. the keyword rule table below (label / package text),
 * 4. the existing conservative package heuristic,
 * 5. "other".
 *
 * Renames never change an id: they only change the displayed name, so a
 * renamed category keeps its apps when the rename is cleared.
 */

// ------------------------------------------------------------ categories

/** Category ids, in display order ("other" always last). */
val CATEGORY_ORDER: List<String> = listOf(
    "social", "game", "audio", "video", "image", "news", "maps",
    "productivity", "finance", "shopping", "health", "travel",
    "education", "tools", "other",
)

private val LIBRARY_CATEGORY_NAMES: Map<String, String> = mapOf(
    "social" to "Social",
    "game" to "Games",
    "audio" to "Music & Audio",
    "video" to "Video",
    "image" to "Photos",
    "news" to "News",
    "maps" to "Maps",
    "productivity" to "Productivity",
    "finance" to "Finance",
    "shopping" to "Shopping",
    "health" to "Health",
    "travel" to "Travel",
    "education" to "Education",
    "tools" to "Tools",
    "other" to "Other",
)

/** Keyword rule table, checked in order: (keywords, category id). */
val CATEGORY_KEYWORD_RULES: List<Pair<List<String>, String>> = listOf(
    listOf("bank", "wallet", "finance", "invest", "stock", "crypto", "paytm", "gpay") to "finance",
    listOf("shop", "store", "market", "amazon", "flipkart", "ebay", "myntra") to "shopping",
    listOf("health", "fitness", "workout", "doctor", "medical") to "health",
    listOf("travel", "flight", "hotel", "taxi", "cab", "booking") to "travel",
    listOf("learn", "school", "class", "course", "study", "education") to "education",
    listOf("music", "audio", "podcast", "radio", "spotify") to "audio",
    listOf("video", "tube", "stream", "cinema", "netflix", "youtube") to "video",
    listOf("photo", "camera", "gallery", "picture") to "image",
    listOf("chat", "message", "messenger", "whatsapp", "telegram", "instagram", "facebook") to "social",
    listOf("game", "gaming", "chess", "puzzle") to "game",
    listOf("map", "navigation", "navigate", "gps") to "maps",
    listOf("news", "headline") to "news",
    listOf("mail", "email", "note", "document", "office", "drive", "task", "calendar") to "productivity",
    listOf("browser", "chrome", "firefox", "settings", "file manager", "calculator", "clock", "weather", "keyboard", "vpn") to "tools",
)

/**
 * Category for an app from its system category, label and package only.
 * Pure function — this is the core the unit tests cover.
 */
fun assignCategory(platformCategory: Int, label: String, pkg: String): String {
    val system = categoryIdOf(platformCategory)
    if (system != "other") return system
    val haystack = "${label.lowercase()} $pkg".lowercase()
    for ((keywords, id) in CATEGORY_KEYWORD_RULES) {
        if (keywords.any { haystack.contains(it) }) return id
    }
    val heuristic = heuristicCategoryForPackage(pkg)
    if (heuristic != "other") return heuristic
    return "other"
}

/** Category for a catalog entry, applying user moves from [overrides]. */
fun assignCategory(entry: AppEntry, overrides: CategoryOverrides = CategoryOverrides()): String {
    overrides.moves[entry.key]?.let { return it }
    return assignCategory(entry.category, entry.label, entry.pkg)
}

/** Category for an app, with an explicit app key + overrides map (test seam). */
fun assignCategoryWithOverrides(
    platformCategory: Int,
    label: String,
    pkg: String,
    appKey: String,
    overrides: Map<String, String>,
): String {
    overrides[appKey]?.let { return it }
    return assignCategory(platformCategory, label, pkg)
}

// ------------------------------------------------------------- overrides

/** The decoded form of the `categoryOverridesJson` key. Never throws. */
data class CategoryOverrides(
    val moves: Map<String, String> = emptyMap(),
    val names: Map<String, String> = emptyMap(),
)

/** Parse `categoryOverridesJson`. Malformed input yields empty overrides. */
fun parseCategoryOverrides(json: String): CategoryOverrides {
    if (json.isBlank()) return CategoryOverrides()
    return try {
        val root = JSONObject(json)
        val moves = LinkedHashMap<String, String>()
        val movesObj = root.optJSONObject("moves")
        if (movesObj != null) {
            for (k in movesObj.keys()) {
                val v = movesObj.optString(k, "")
                if (k.isNotBlank() && v.isNotBlank()) moves[k] = v
            }
        }
        val names = LinkedHashMap<String, String>()
        val namesObj = root.optJSONObject("names")
        if (namesObj != null) {
            for (k in namesObj.keys()) {
                val v = namesObj.optString(k, "")
                if (k.isNotBlank() && v.isNotBlank()) names[k] = v
            }
        }
        CategoryOverrides(moves, names)
    } catch (t: Throwable) {
        CategoryOverrides()
    }
}

/** Encode overrides back to the `categoryOverridesJson` string form. */
fun encodeCategoryOverrides(overrides: CategoryOverrides): String {
    val root = JSONObject()
    val movesObj = JSONObject()
    for ((k, v) in overrides.moves) movesObj.put(k, v)
    val namesObj = JSONObject()
    for ((k, v) in overrides.names) namesObj.put(k, v)
    root.put("moves", movesObj)
    root.put("names", namesObj)
    return root.toString()
}

/**
 * Override plumbing for the coordinator: return the new
 * `categoryOverridesJson` value after moving [appKey] to [categoryId].
 * Persist the returned string; this function itself writes nothing.
 */
fun categoryOverridesWithMove(json: String, appKey: String, categoryId: String): String {
    val current = parseCategoryOverrides(json)
    val moves = current.moves.toMutableMap()
    if (categoryId.isBlank()) moves.remove(appKey) else moves[appKey] = categoryId
    return encodeCategoryOverrides(current.copy(moves = moves))
}

/**
 * Override plumbing for the coordinator: return the new
 * `categoryOverridesJson` value after renaming [categoryId] to [name].
 * A blank name clears the custom name (the default name returns).
 * Persist the returned string; this function itself writes nothing.
 */
fun categoryOverridesWithRename(json: String, categoryId: String, name: String): String {
    val current = parseCategoryOverrides(json)
    val names = current.names.toMutableMap()
    if (name.isBlank()) names.remove(categoryId) else names[categoryId] = name
    return encodeCategoryOverrides(current.copy(names = names))
}

/** Display name for a category id, honouring a custom rename. */
fun categoryDisplayName(categoryId: String, overrides: CategoryOverrides): String {
    overrides.names[categoryId]?.let { return it }
    LIBRARY_CATEGORY_NAMES[categoryId]?.let { return it }
    if (categoryId.isEmpty()) return categoryId
    return categoryId.first().uppercaseChar() + categoryId.drop(1)
}

/**
 * Group apps by category. Pure function: categories with no apps are
 * omitted; categories follow [CATEGORY_ORDER] (unknown ids sort after the
 * known ones, before "other"); apps inside a category sort by label.
 */
fun groupAppsByCategory(
    apps: List<AppEntry>,
    overrides: CategoryOverrides = CategoryOverrides(),
): Map<String, List<AppEntry>> {
    val grouped = LinkedHashMap<String, MutableList<AppEntry>>()
    for (app in apps) {
        val id = assignCategory(app, overrides)
        grouped.getOrPut(id) { mutableListOf() }.add(app)
    }
    for (list in grouped.values) list.sortBy { it.label.lowercase() }
    val orderIndex = CATEGORY_ORDER.withIndex().associate { it.value to it.index }
    val sortedKeys = grouped.keys.sortedWith(
        compareBy(
            { orderIndex[it] ?: (CATEGORY_ORDER.size - 1) },
            { categoryDisplayName(it, overrides).lowercase() },
        ),
    )
    val out = LinkedHashMap<String, List<AppEntry>>()
    for (k in sortedKeys) out[k] = grouped.getValue(k)
    return out
}

// ------------------------------------------------------------ composable

/**
 * The Library Categories tab content.
 *
 * @param apps the visible catalog (the coordinator filters hidden apps).
 * @param appRepo icon source for tiles and popover rows.
 * @param overridesJson the current `categoryOverridesJson` value.
 * @param expanded true on Expanded (3-column grid), false on Compact
 *   (2-column grid). The coordinator passes the size class; this file
 *   never measures the window itself.
 * @param onLaunch launch handler for a tapped app.
 * @param onMoveApp called when the user picks a new category for an app;
 *   the coordinator persists it via [categoryOverridesWithMove] and
 *   re-renders with the new [overridesJson].
 * @param onRenameCategory called with the new name (blank = clear);
 *   the coordinator persists it via [categoryOverridesWithRename].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoriesView(
    apps: List<AppEntry>,
    appRepo: AppRepository,
    overridesJson: String,
    expanded: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onMoveApp: (appKey: String, categoryId: String) -> Unit = { _, _ -> },
    onRenameCategory: (categoryId: String, name: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val overrides = remember(overridesJson) { parseCategoryOverrides(overridesJson) }
    val groups = remember(apps, overrides) { groupAppsByCategory(apps, overrides) }
    val groupList = remember(groups) { groups.entries.toList() }

    var openCategory by remember { mutableStateOf<String?>(null) }
    var renameCategory by remember { mutableStateOf<String?>(null) }
    var moveApp by remember { mutableStateOf<AppEntry?>(null) }

    if (groupList.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No apps", color = palette.muted, fontSize = 15.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (expanded) 3 else 2),
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(groupList, key = { it.key }) { (categoryId, members) ->
            GlassSurface(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.combinedClickable(
                    onClick = { openCategory = categoryId },
                    onLongClick = { renameCategory = categoryId },
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    // 4 mini icons (2 x 2) + category name.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (member in members.take(2)) {
                            AppIconImage(member, appRepo, 28.dp)
                        }
                        // Pad short rows so every tile keeps its shape.
                        repeat((2 - members.take(2).size).coerceAtLeast(0)) {
                            Spacer(Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (member in members.drop(2).take(2)) {
                            AppIconImage(member, appRepo, 28.dp)
                        }
                        repeat((2 - members.drop(2).take(2).size).coerceAtLeast(0)) {
                            Spacer(Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        categoryDisplayName(categoryId, overrides),
                        color = palette.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${members.size} apps",
                        color = palette.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                    Text(
                        "Long-press to rename",
                        color = palette.muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    // Popover with all apps in the tapped category.
    val openId = openCategory
    if (openId != null) {
        val members = groups[openId].orEmpty()
        Dialog(onDismissRequest = { openCategory = null }) {
            GlassSurface(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        categoryDisplayName(openId, overrides),
                        color = palette.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${members.size} apps · long-press an app to move it",
                        color = palette.muted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        Modifier.fillMaxWidth()
                            .heightIn(max = 420.dp),
                    ) {
                        items(members, key = { it.key }) { entry ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onLaunch(entry) },
                                        onLongClick = { moveApp = entry },
                                    )
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIconImage(entry, appRepo, 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    entry.label,
                                    color = palette.onSurface,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { openCategory = null }) { Text("Close") }
                }
            }
        }
    }

    // Rename dialog (tile long-press).
    val renameId = renameCategory
    if (renameId != null) {
        var name by remember(renameId) {
            mutableStateOf(overrides.names[renameId] ?: LIBRARY_CATEGORY_NAMES[renameId].orEmpty())
        }
        AlertDialog(
            onDismissRequest = { renameCategory = null },
            title = { Text("Rename category") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameCategory(renameId, name.trim())
                    renameCategory = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameCategory = null }) { Text("Cancel") } },
        )
    }

    // Move-to dialog (popover app long-press).
    val moving = moveApp
    if (moving != null) {
        val currentId = assignCategory(moving, overrides)
        AlertDialog(
            onDismissRequest = { moveApp = null },
            title = { Text("Move “${moving.label}” to…") },
            text = {
                LazyColumn(
                    Modifier.fillMaxWidth()
                        .heightIn(max = 320.dp),
                ) {
                    val ids = (CATEGORY_ORDER + groups.keys).distinct()
                    items(ids, key = { it }) { id ->
                        Row(
                            Modifier.fillMaxWidth()
                                .combinedClickable(onClick = {
                                    onMoveApp(moving.key, id)
                                    moveApp = null
                                })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (id == currentId) "● " else "○ ",
                                color = palette.accent,
                                fontSize = 14.sp,
                            )
                            Text(
                                categoryDisplayName(id, overrides),
                                color = palette.onSurface,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { moveApp = null }) { Text("Cancel") } },
        )
    }
}
