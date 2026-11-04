package app.havenos.launcher.data.network

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Generic v2 API cache: one row per (API, parameter) fetch. The payload
 * is the raw response JSON string; parsing stays with the feature
 * worker that owns the API, so this layer never needs a schema change
 * when a response shape grows a field.
 */

/** One cached API response. [key] format is defined by [ApiCacheKeys]. */
@Entity(tableName = "api_cache")
data class ApiCacheRow(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAtMs: Long,
    // Coordination fix §5/§7: consecutive failed refreshes (default 0
    // for pre-v6 rows via the migration). Maintained ONLY by
    // ApiStore.refreshApi; diagnostics only — the card status line is
    // driven by cache age, not by this counter.
    val consecutiveFailures: Int = 0,
)

@Dao
interface ApiCacheDao {
    @Query("SELECT * FROM api_cache WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): ApiCacheRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ApiCacheRow)
}

/**
 * Cache key format per API. Downstream workers MUST use these exact
 * keys — a card that reads a different key will never see the worker's
 * cache. Format: "<api>" or "<api>:<lowercase-param>".
 */
object ApiCacheKeys {
    fun quote(): String = "quote"
    fun scores(sport: String): String = "scores:${sport.lowercase()}"
    fun crypto(): String = "crypto"
    fun apod(): String = "apod"
    fun githubRepos(username: String): String = "github:repos:${username.lowercase()}"
    fun githubEvents(username: String): String = "github:events:${username.lowercase()}"
    fun currency(base: String): String = "currency:${base.lowercase()}"
    fun dictionary(word: String): String = "dictionary:${word.lowercase()}"
}

/**
 * 3.1 Phase A2: one Diagnostics row per API (not per cache key):
 * the last fetch outcome, so Device & Data can show what actually
 * happened on a real network and the developer can confirm the Quote
 * and NASA response shapes. [bodySnippet] holds the first 200 chars
 * of the response body for parse errors ONLY, otherwise empty —
 * never keys, tokens, or personal data (API bodies never contain
 * request credentials; the snippet is still capped and
 * parse-error-only by construction).
 */
@Entity(tableName = "api_diagnostics")
data class ApiDiagnosticsRow(
    @PrimaryKey val api: String, // ApiId.name
    val resultClass: String, // ApiFailureClass.name, or "SUCCESS"
    val httpCode: Int, // -1 when no HTTP response was received
    val timeMs: Long,
    val bodySnippet: String = "",
)

@Dao
interface ApiDiagnosticsDao {
    @Query("SELECT * FROM api_diagnostics WHERE api = :api LIMIT 1")
    suspend fun get(api: String): ApiDiagnosticsRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ApiDiagnosticsRow)

    @Query("SELECT * FROM api_diagnostics ORDER BY api ASC")
    suspend fun all(): List<ApiDiagnosticsRow>
}

/**
 * Cache policy per API (maxima, not targets — a worker never refreshes
 * more often than this, and a card never waits on a fresh fetch when a
 * row within this age exists). Dictionary has no periodic refresh at
 * all: it is fetched on demand and cached by word forever (until the
 * row is deleted); [maxAgeMs] returns Long.MAX_VALUE for it so
 * cache-first reads never treat a stored word as stale.
 */
object ApiCachePolicy {
    const val DAY_MS: Long = 24L * 60 * 60 * 1000
    const val HOUR_MS: Long = 60L * 60 * 1000
    const val MINUTE_MS: Long = 60L * 1000

    fun maxAgeMs(api: ApiId): Long = when (api) {
        ApiId.QUOTE -> DAY_MS
        ApiId.SCORES -> 15 * MINUTE_MS
        ApiId.CRYPTO -> 30 * MINUTE_MS
        ApiId.APOD -> DAY_MS
        ApiId.GITHUB -> HOUR_MS
        ApiId.CURRENCY -> DAY_MS
        ApiId.DICTIONARY -> Long.MAX_VALUE
    }

    /** True when [row] is missing or older than this API's max age. */
    fun isStale(api: ApiId, row: ApiCacheRow?, nowMs: Long): Boolean {
        if (row == null) return true
        val age = nowMs - row.updatedAtMs
        return age < 0 || age > maxAgeMs(api)
    }
}

/**
 * Coordination fix §7: typed card status (not stringly status), used
 * by every API card's status line. Age ratio alone drives it — the
 * consecutive-failure counter is diagnostics only, never UI — because
 * worker cadence equals max age, so sustained failure necessarily ages
 * the row past threshold. Dictionary is exempt by construction
 * (maxAgeMs = Long.MAX_VALUE, so the ratio can never reach 2).
 */
sealed interface CardStatus {
    data object Fresh : CardStatus
    data object Stale : CardStatus
    data class Expired(val lastUpdatedMs: Long) : CardStatus
    data object EmptyOffline : CardStatus
}

fun cardStatus(api: ApiId, row: ApiCacheRow?, nowMs: Long): CardStatus {
    if (row == null) return CardStatus.EmptyOffline
    val maxAge = ApiCachePolicy.maxAgeMs(api)
    val age = nowMs - row.updatedAtMs
    return when {
        age <= maxAge -> CardStatus.Fresh
        age / maxAge < 2 -> CardStatus.Stale
        else -> CardStatus.Expired(row.updatedAtMs)
    }
}
