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
     * 3.1 A11 flow 16 + A12: what a toggle edit means for one API's
     * worker. SCHEDULE = enabled with a cadence; CANCEL = disabled
     * (cancelled outright, not left to no-op on its next run); NONE
     * = no periodic work exists (dictionary, on demand only).
     */
    enum class ApiWorkerAction { SCHEDULE, CANCEL, NONE }

    fun apiWorkerAction(api: ApiId, isEnabled: Boolean): ApiWorkerAction =
        if (!isEnabled) ApiWorkerAction.CANCEL
        else if (cadence(api) == null) ApiWorkerAction.NONE
        else ApiWorkerAction.SCHEDULE

    private fun enqueueApi(wm: WorkManager, api: ApiId, constraints: Constraints) {
        val (interval, unit) = cadence(api) ?: return
        val request = PeriodicWorkRequestBuilder<ApiRefreshWorker>(interval, unit)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .setInputData(Data.Builder().putString(ApiRefreshWorker.KEY_API, api.name).build())
            .build()
        wm.enqueueUniquePeriodicWork(workName(api), ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Apply one API's action (toggle-edit path): cancel or schedule just it. */
    fun applyWorkerAction(context: Context, api: ApiId, isEnabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        when (apiWorkerAction(api, isEnabled)) {
            ApiWorkerAction.CANCEL -> wm.cancelUniqueWork(workName(api))
            ApiWorkerAction.NONE -> Unit
            ApiWorkerAction.SCHEDULE -> enqueueApi(
                wm, api,
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
        }
    }

    /**
     * Enqueue (KEEP) one unique periodic worker per ENABLED API, and
     * CANCEL a disabled API's worker outright (3.1 A12). Safe to call
     * on every launch: KEEP never duplicates or resets an existing
     * schedule. Requires network; no charging/idle constraints, so a
     * tablet on Wi-Fi still refreshes while idle. Workers still
     * re-check their toggle at run time (refreshApi gates on it).
     */
    fun scheduleAll(context: Context, isEnabled: (ApiId) -> Boolean = { true }) {
        val wm = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        for (api in periodicApis) {
            if (!isEnabled(api)) {
                wm.cancelUniqueWork(workName(api))
                continue
            }
            enqueueApi(wm, api, constraints)
        }
    }
}
