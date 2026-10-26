package app.havenos.launcher.data.network

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.havenos.launcher.HavenApp
import java.util.concurrent.TimeUnit

/**
 * WorkManager periodic refresh for v2 APIs (M1). One worker class,
 * parameterized by the "api" input ([ApiId] name); one unique periodic
 * work per API (see [ApiRefreshScheduler]).
 *
 * Gating: the worker reads the persisted Settings FIRST and returns
 * success-without-fetching when its API's toggle is off — so toggling
 * an API off stops its network use without cancelling/re-enqueuing
 * work. Failures also return success: a failed fetch must not trigger
 * WorkManager retries (that would burn rate limits); the next
 * scheduled run retries, and the card keeps showing stale cache meanwhile.
 * Dictionary has no worker: it is on-demand only ([ApiStore.dictionaryFlow]).
 */
class ApiRefreshWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val api = runCatching { ApiId.valueOf(inputData.getString(KEY_API) ?: "") }.getOrNull()
            ?: return Result.success()
        if (api == ApiId.DICTIONARY) return Result.success()
        val app = applicationContext as HavenApp
        return try {
            val state = app.settings.persistedState()
            // Toggle check first: off means skip, never fetch.
            if (!ApiToggles.isEnabled(state, api)) return Result.success()
            app.apiStore.refreshApi(api, state)
            Result.success()
        } catch (t: Throwable) {
            // Never crash the worker, never retry-storm: next run retries.
            Result.success()
        }
    }

    companion object {
        const val KEY_API = "api"
    }
}

/** Unique work names + cadences (maxima, not targets). */
object ApiRefreshScheduler {
    /** Unique periodic-work name for [api]. */
    fun workName(api: ApiId): String = "api-refresh-${api.name.lowercase()}"

    /** Cadence for [api]: (interval, unit). Dictionary has none. */
    fun cadence(api: ApiId): Pair<Long, TimeUnit>? = when (api) {
        ApiId.QUOTE -> 24L to TimeUnit.HOURS
        ApiId.SCORES -> 15L to TimeUnit.MINUTES
        ApiId.CRYPTO -> 30L to TimeUnit.MINUTES
        ApiId.APOD -> 24L to TimeUnit.HOURS
        ApiId.GITHUB -> 1L to TimeUnit.HOURS
        ApiId.CURRENCY -> 24L to TimeUnit.HOURS
        ApiId.DICTIONARY -> null
    }

    /** APIs with a periodic worker (everything except dictionary). */
    val periodicApis: List<ApiId> =
        ApiId.entries.filter { it != ApiId.DICTIONARY }

    /**
     * Enqueue (KEEP) one unique periodic worker per API. Safe to call
     * on every launch: KEEP never duplicates or resets an existing
     * schedule. Requires network; no charging/idle constraints, so a
     * tablet on Wi-Fi still refreshes while idle.
     */
    fun scheduleAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        for (api in periodicApis) {
            val (interval, unit) = cadence(api) ?: continue
            val request = PeriodicWorkRequestBuilder<ApiRefreshWorker>(interval, unit)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
                .setInputData(Data.Builder().putString(ApiRefreshWorker.KEY_API, api.name).build())
                .build()
            wm.enqueueUniquePeriodicWork(workName(api), ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
