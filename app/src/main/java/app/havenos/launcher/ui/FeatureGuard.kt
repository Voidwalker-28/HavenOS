package app.havenos.launcher.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/**
 * 3.1.1 (item 2): failure isolation for the four soft features. A feature
 * failure is logged locally, disables ONLY that feature for the session,
 * and shows one toast. It never reaches the Activity or the main thread's
 * uncaught path (that path belongs to the crash-loop marker, item 3).
 */

/** Soft features, each with its exact session-disable toast text. */
enum class FeatureId(val toastText: String) {
    MASCOT("Mascot paused after an error"),
    ISLAND("Island paused after an error"),
    FIND("Find paused after an error"),
    ONLINE_CARDS("Online cards paused after an error"),
}

/**
 * Session-only disable set (process memory, never persisted, never
 * uploaded). A MutableState so hosts recompose when a feature is cut.
 */
object FeatureSessionState {
    var disabled by mutableStateOf<Set<FeatureId>>(emptySet())
        private set

    fun disable(feature: FeatureId) {
        if (feature !in disabled) disabled = disabled + feature
    }

    fun isDisabled(feature: FeatureId): Boolean = feature in disabled
}

/** One logged feature error: type, message, top 5 frames, local only. */
data class FeatureError(
    val timeMs: Long,
    val feature: String,
    val clazz: String,
    val message: String,
    val frames: String,
)

/** Append a feature error to filesDir/feature_errors.txt (last 20 kept). Never throws. */
fun logFeatureError(context: Context, feature: FeatureId, t: Throwable) {
    try {
        val frames = t.stackTrace.take(5).joinToString(" | ") {
            "${it.className}.${it.methodName}:${it.lineNumber}"
        }
        val line = listOf(
            System.currentTimeMillis().toString(),
            feature.name,
            t::class.java.name,
            (t.message ?: "").take(200).replace('|', '/').replace('\n', ' '),
            frames,
        ).joinToString("|")
        val file = File(context.filesDir, "feature_errors.txt")
        val kept = try {
            file.readLines()
        } catch (_: Exception) {
            emptyList()
        }.plus(line).takeLast(20)
        file.writeText(kept.joinToString("\n") + "\n")
    } catch (_: Exception) {
        // Logging must never crash the launcher.
    }
}

/** Read the local feature-error log (Diagnostics UI). Never throws. */
fun readFeatureErrors(context: Context): List<FeatureError> {
    return try {
        val file = File(context.filesDir, "feature_errors.txt")
        if (!file.exists()) return emptyList()
        file.readLines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 5) return@mapNotNull null
            FeatureError(
                timeMs = parts[0].toLongOrNull() ?: return@mapNotNull null,
                feature = parts[1],
                clazz = parts[2],
                message = parts[3],
                frames = parts.drop(4).joinToString("|"),
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun showFeatureToast(context: Context, feature: FeatureId) {
    try {
        Toast.makeText(context, feature.toastText, Toast.LENGTH_LONG).show()
    } catch (_: Exception) {
        // A toast must never crash the launcher.
    }
}

/**
 * Handler for one feature: log locally, disable the feature for the
 * session, show one toast on Main. Cancellation is never swallowed here —
 * the handler only runs for real failures.
 */
fun featureErrorHandler(context: Context, feature: FeatureId): CoroutineExceptionHandler =
    CoroutineExceptionHandler { _, t ->
        if (t is CancellationException) return@CoroutineExceptionHandler
        logFeatureError(context, feature, t)
        FeatureSessionState.disable(feature)
        Handler(Looper.getMainLooper()).post { showFeatureToast(context, feature) }
    }

/**
 * A child scope of [parentScope] that is supervised: one child's failure
 * is delivered to the feature handler (log + disable + toast) and never
 * cancels siblings or the parent. Cancellation still flows down from the
 * parent (composition-leave cancels the scope), because the SupervisorJob
 * is parented on the parent scope's Job.
 */
fun supervisedChildScope(
    parentScope: CoroutineScope,
    context: Context,
    feature: FeatureId,
): CoroutineScope {
    val parentJob: Job? = parentScope.coroutineContext[Job]
    return CoroutineScope(
        parentScope.coroutineContext +
            SupervisorJob(parentJob) +
            featureErrorHandler(context, feature),
    )
}

/**
 * Suspend helper for LaunchedEffect bodies: runs [block], and on failure
 * logs, disables the feature, toasts once on Main, and returns null.
 * Cancellation propagates untouched.
 */
suspend fun <T> runFeatureGuarded(
    context: Context,
    feature: FeatureId,
    block: suspend () -> T,
): T? {
    return try {
        block()
    } catch (t: CancellationException) {
        throw t
    } catch (t: Throwable) {
        logFeatureError(context, feature, t)
        FeatureSessionState.disable(feature)
        withContext(Dispatchers.Main) { showFeatureToast(context, feature) }
        null
    }
}
