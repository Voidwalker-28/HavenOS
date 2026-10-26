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
