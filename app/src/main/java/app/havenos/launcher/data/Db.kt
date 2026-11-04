package app.havenos.launcher.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import app.havenos.launcher.data.network.ApiCacheDao
import app.havenos.launcher.data.network.ApiCacheRow
import app.havenos.launcher.data.network.ApiDiagnosticsDao
import app.havenos.launcher.data.network.ApiDiagnosticsRow
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Entity(tableName = "home_items")
data class HomeItem(
    @PrimaryKey val id: String,
    val kind: Int,
    // Legacy paged-grid placement (vc5 removed pages): page/col/row are
    // still stored so the one-time scroll-flow migration (ui/Grid.kt
    // migrateToFlow) can sort by them, but home no longer reads them for
    // placement. New items always carry page = 0, col = 0, row = 0.
    val page: Int,
    // Grid coordinates are in HALF-CELL units since DB v2 (1 cell = 2 units).
    // Still used to size flow rows (colSpan/rowSpan -> widget height).
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int,
    val payload: String,
    // Free-canvas placement (fractions of the page, 0..1); legacy since
    // pages were removed. 0 until converted from grid coordinates.
    val fx: Float = 0f,
    val fy: Float = 0f,
    val fw: Float = 0f,
    val fh: Float = 0f,
    // Scroll-flow position (vc5): 0..n in display order for the single
    // continuous home. -1 = unmigrated pre-vc5 paged item; the migration
    // in HavenRoot assigns positions exactly once (items already >= 0 are
    // never renumbered, so a later user reorder survives any re-run).
    val flowOrder: Int = -1,
)

object ItemKind {
    const val APP = 0
    const val FOLDER = 1
    const val CLOCK = 2
    const val WEATHER = 3
    const val AGENDA = 4
    const val NOW_PLAYING = 5
    const val STORAGE = 6
    const val CALENDAR = 7
    const val COUNTDOWN = 8
    const val TASKS = 9
    const val NOTES = 10
    const val GALLERY = 11
    const val QUICK_TILES = 12
    const val CLOCK_PAGE = 13
    const val STACK = 14
    const val TP_WIDGET = 15 // payload = appWidgetId (Int as string)
    const val SUGGESTED = 16
    const val BATTERY = 17
    const val QUICK_SEARCH = 18
}

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String,
    val name: String,
    val membersJson: String,
)

/** Parse a JSON string-list (e.g. folder members); emptyList on any error. Never throws. */
fun parseMembers(json: String): List<String> {
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val v = arr.optString(i, "")
            if (v.isEmpty()) null else v
        }.filterNotNull()
    } catch (e: Exception) {
        emptyList()
    }
}

fun encodeMembers(members: List<String>): String = JSONArray(members).toString()

/** Per-app label overrides (app key -> custom label). Never throws. */
fun parseLabelOverrides(json: String): Map<String, String> = try {
    val obj = org.json.JSONObject(json)
    val out = LinkedHashMap<String, String>()
    for (k in obj.keys()) {
        val v = obj.optString(k, "")
        if (v.isNotBlank()) out[k] = v
    }
    out
} catch (t: Throwable) {
    emptyMap()
}

fun encodeLabelOverrides(overrides: Map<String, String>): String {
    val obj = org.json.JSONObject()
    for ((k, v) in overrides) {
        if (v.isNotBlank()) obj.put(k, v)
    }
    return obj.toString()
}

/** Parse the category display-name overrides (id -> name); emptyMap on any error. Never throws. */
fun parseCatNames(json: String): Map<String, String> = try {
    val obj = org.json.JSONObject(json)
    val out = mutableMapOf<String, String>()
    for (k in obj.keys()) {
        val v = obj.optString(k, "")
        if (v.isNotBlank()) out[k] = v
    }
    out
} catch (t: Throwable) {
    emptyMap()
}

@Dao
interface HomeItemDao {
    @Query("SELECT * FROM home_items")
    fun observeAll(): Flow<List<HomeItem>>

    @Query("SELECT * FROM home_items")
    suspend fun all(): List<HomeItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HomeItem>)

    @Query("DELETE FROM home_items")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<HomeItem>) {
        clear()
        if (items.isNotEmpty()) upsertAll(items)
    }
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun observeAll(): Flow<List<Folder>>

    @Query("SELECT * FROM folders")
    suspend fun all(): List<Folder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: Folder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(folders: List<Folder>)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM folders")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(folders: List<Folder>) {
        clear()
        if (folders.isNotEmpty()) upsertAll(folders)
    }
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // v2: grid coordinates become half-cell units (x2) + free-canvas columns.
        db.execSQL("UPDATE home_items SET col = col * 2, row = row * 2, colSpan = colSpan * 2, rowSpan = rowSpan * 2")
        db.execSQL("ALTER TABLE home_items ADD COLUMN fx REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE home_items ADD COLUMN fy REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE home_items ADD COLUMN fw REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE home_items ADD COLUMN fh REAL NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // v3: scroll-flow position. -1 marks pre-vc5 paged items for the
        // one-time in-app migration (ui/Grid.kt migrateToFlow); the column
        // default must stay -1, never 0, or unmigrated items would look
        // migrated and the migration would silently skip them. Column
        // name is the Room default (the field name, camelCase), matching
        // every other column in this table (membersJson, colSpan, ...).
        db.execSQL("ALTER TABLE home_items ADD COLUMN flowOrder INTEGER NOT NULL DEFAULT -1")
    }
}

// M9 Worker D (2026-10-23): schema v4. HomeItem/Folder columns are
// UNTOUCHED (positions migrate nowhere — existing user layouts survive
// verbatim); v4 only ADDS two tables, so the migration cannot scramble
// a layout:
// - layout_profiles: one row per (window class, item id) holding that
//   profile's placement (LayoutProfiles.kt). Empty on upgrade: every
//   profile derives from canonical order on first use.
// - layout_snapshots: auto-snapshot rows (LayoutSnapshots.kt).
// MIGRATION_3_4's SQL is also exposed via migration34Sql() so the unit
// test can pin the table/column names without a database.
@Entity(tableName = "layout_profiles")
data class LayoutProfileRow(
    @PrimaryKey val key: String, // "$windowClass|$itemId"
    val windowClass: String,
    val itemId: String,
    val page: Int,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int,
)

@Entity(tableName = "layout_snapshots")
data class LayoutSnapshotRow(
    @PrimaryKey val id: Long, // createdAt ms
    val createdAtMs: Long,
    val pageCount: Int,
    val itemCount: Int,
    val folderCount: Int,
    val layoutJson: String,
)

@Dao
interface LayoutProfileDao {
    @Query("SELECT * FROM layout_profiles")
    suspend fun all(): List<LayoutProfileRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<LayoutProfileRow>)

    @Query("DELETE FROM layout_profiles")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(rows: List<LayoutProfileRow>) {
        clear()
        if (rows.isNotEmpty()) upsertAll(rows)
    }
}

@Dao
interface LayoutSnapshotDao {
    @Query("SELECT * FROM layout_snapshots ORDER BY createdAtMs DESC")
    suspend fun allNewestFirst(): List<LayoutSnapshotRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: LayoutSnapshotRow)

    @Query("DELETE FROM layout_snapshots WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM layout_snapshots WHERE id NOT IN (:keepIds)")
    suspend fun pruneExcept(keepIds: List<Long>)
}

// Coordination fix §5 (schema v6): durable undo + durable dirty flags.
// - undo_entries: one row per undo/redo frame. direction is "UNDO" or
//   "REDO"; layoutJson is the full snapshot JSON (items, folders,
//   dock, hidden, Today cards, recents); profilesJson is the
//   LayoutProfiles JSON (placements + dirty flags), so Sync/install
//   undo restores profiles too. seq orders frames oldest→newest.
// - profile_dirty: one row per dirty window class. The dirty set used
//   to live in memory + backup JSON only (a process restart lost it);
//   now switch-time repair survives restart.
@Entity(tableName = "undo_entries")
data class UndoEntryRow(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val direction: String, // "UNDO" | "REDO"
    val layoutJson: String,
    val profilesJson: String,
    // 3.1 A9: grid-override action ("L:8:6" / "P:-1:-1"), "" = none.
    // Added by MIGRATION_7_8; default keeps old constructions valid.
    val gridJson: String = "",
)

@Entity(tableName = "profile_dirty")
data class ProfileDirtyRow(
    @PrimaryKey val windowClass: String,
)

@Dao
interface UndoEntryDao {
    @Query("SELECT * FROM undo_entries WHERE direction = :direction ORDER BY seq ASC")
    suspend fun all(direction: String): List<UndoEntryRow>

    @Insert
    suspend fun insert(row: UndoEntryRow)

    @Query(
        "DELETE FROM undo_entries WHERE direction = :direction AND seq = " +
            "(SELECT MAX(seq) FROM undo_entries WHERE direction = :direction)",
    )
    suspend fun deleteLatest(direction: String)

    @Query("DELETE FROM undo_entries WHERE direction = :direction")
    suspend fun clearDirection(direction: String)

    @Query("DELETE FROM undo_entries")
    suspend fun clearAll()

    @Query(
        "DELETE FROM undo_entries WHERE direction = :direction AND seq NOT IN " +
            "(SELECT seq FROM undo_entries WHERE direction = :direction ORDER BY seq DESC LIMIT :keep)",
    )
    suspend fun prune(direction: String, keep: Int)
}

@Dao
interface ProfileDirtyDao {
    @Query("SELECT * FROM profile_dirty")
    suspend fun all(): List<ProfileDirtyRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<ProfileDirtyRow>)

    @Query("DELETE FROM profile_dirty")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(rows: List<ProfileDirtyRow>) {
        clear()
        if (rows.isNotEmpty()) upsertAll(rows)
    }
}

fun migration34Sql(): List<String> = listOf(
    "CREATE TABLE IF NOT EXISTS `layout_profiles` (`key` TEXT NOT NULL, `windowClass` TEXT NOT NULL, `itemId` TEXT NOT NULL, `page` INTEGER NOT NULL, `col` INTEGER NOT NULL, `row` INTEGER NOT NULL, `colSpan` INTEGER NOT NULL, `rowSpan` INTEGER NOT NULL, PRIMARY KEY(`key`))",
    "CREATE TABLE IF NOT EXISTS `layout_snapshots` (`id` INTEGER NOT NULL, `createdAtMs` INTEGER NOT NULL, `pageCount` INTEGER NOT NULL, `itemCount` INTEGER NOT NULL, `folderCount` INTEGER NOT NULL, `layoutJson` TEXT NOT NULL, PRIMARY KEY(`id`))",
)

val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        for (sql in migration34Sql()) db.execSQL(sql)
    }
}

// v2 M1 (2026-10-26): schema v5 only ADDS the api_cache table (see
// data/network/ApiCache.kt); existing tables are untouched, so layouts
// survive the upgrade verbatim.
val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `api_cache` " +
                "(`key` TEXT NOT NULL, `json` TEXT NOT NULL, `updatedAtMs` INTEGER NOT NULL, " +
                "PRIMARY KEY(`key`))",
        )
    }
}

// Coordination fix §5 (2026-10-29): schema v6 is ADDITIVE ONLY — two
// new tables + one defaulted column — so a v5 install upgrades with
// layouts, profiles, snapshots, and API cache untouched. New tables
// start empty (undo depth 0, no dirty flags after upgrade; a dirty
// flag set before the upgrade was memory-only and is already lost by
// the upgrade restart itself). api_cache rows gain
// consecutiveFailures = 0 (step 7 maintains it; the default keeps old
// rows valid). SQL is exposed via migration56Sql() so the unit test
// pins it, following the migration34Sql() precedent.
fun migration56Sql(): List<String> = listOf(
    "CREATE TABLE IF NOT EXISTS `undo_entries` (`seq` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `direction` TEXT NOT NULL, `layoutJson` TEXT NOT NULL, `profilesJson` TEXT NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `profile_dirty` (`windowClass` TEXT NOT NULL, PRIMARY KEY(`windowClass`))",
    "ALTER TABLE `api_cache` ADD COLUMN `consecutiveFailures` INTEGER NOT NULL DEFAULT 0",
)

val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        for (sql in migration56Sql()) db.execSQL(sql)
    }
}

// 3.1 Phase A2 (2026-10-30): schema v7 is ADDITIVE ONLY — one new
// table, no column changes — so a v6 install upgrades with layouts,
// profiles, snapshots, API cache, undo frames, and dirty flags
// untouched. The table starts empty: Diagnostics rows appear after
// the first post-upgrade fetch per API. SQL is exposed via
// migration67Sql() so the unit test pins it, following the
// migration34Sql()/migration56Sql() precedent.
fun migration67Sql(): List<String> = listOf(
    "CREATE TABLE IF NOT EXISTS `api_diagnostics` (`api` TEXT NOT NULL, `resultClass` TEXT NOT NULL, `httpCode` INTEGER NOT NULL, `timeMs` INTEGER NOT NULL, `bodySnippet` TEXT NOT NULL, PRIMARY KEY(`api`))",
)

val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        for (sql in migration67Sql()) db.execSQL(sql)
    }
}

// 3.1 A9: undo frames gain a grid-override action. ADDITIVE ONLY —
// one nullable-by-default column; existing rows read as "" (no grid
// action), which is exactly their old behaviour. SQL is exposed via
// migration78Sql() so the unit test pins it, following the
// migration34Sql()/migration56Sql()/migration67Sql() precedent.
fun migration78Sql(): List<String> = listOf(
    "ALTER TABLE `undo_entries` ADD COLUMN `gridJson` TEXT NOT NULL DEFAULT ''",
)

val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        for (sql in migration78Sql()) db.execSQL(sql)
    }
}

@Database(
    entities = [HomeItem::class, Folder::class, LayoutProfileRow::class, LayoutSnapshotRow::class, ApiCacheRow::class, UndoEntryRow::class, ProfileDirtyRow::class, ApiDiagnosticsRow::class],
    version = 8,
    exportSchema = false,
)
abstract class HavenDb : RoomDatabase() {
    abstract fun homeItems(): HomeItemDao
    abstract fun folders(): FolderDao
    abstract fun layoutProfiles(): LayoutProfileDao
    abstract fun layoutSnapshots(): LayoutSnapshotDao
    abstract fun apiCache(): ApiCacheDao
    abstract fun apiDiagnostics(): ApiDiagnosticsDao
    abstract fun undoEntries(): UndoEntryDao
    abstract fun profileDirty(): ProfileDirtyDao

    companion object {
        fun create(ctx: Context): HavenDb =
            Room.databaseBuilder(ctx.applicationContext, HavenDb::class.java, "havenos.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
    }
}
