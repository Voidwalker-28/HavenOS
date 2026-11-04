package app.havenos.launcher.ui.island

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.widget.Toast
import app.havenos.launcher.ui.FeatureId
import app.havenos.launcher.ui.FeatureSessionState
import app.havenos.launcher.ui.logFeatureError
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.havenos.launcher.data.BadgeStore
import app.havenos.launcher.data.HavenNotificationListener
import kotlinx.coroutines.delay

/**
 * Live island activities, observed — never invented.
 *
 * - Charging: sticky ACTION_BATTERY_CHANGED broadcast, no permission.
 * - Media: MediaSessionManager active sessions through the EXISTING
 *   HavenNotificationListener component (the same gate the Now Playing
 *   widget uses). Polled every 2s, only while the listener is
 *   connected — same cadence as that widget, so no new battery cost
 *   class. Unconnected => no media activity, never a placeholder.
 * - Notifications: BadgeStore counts. The activity is TRANSIENT: it
 *   is live only within NOTIFICATION_WINDOW_MS of the last count
 *   increase, and only for bursts newer than [dismissedBurstAt].
 *   [dismissedBurstAt] is the burst timestamp the user dismissed; a
 *   later burst re-arms the pill.
 *
 * Returns activities sorted by [islandPriority], highest first.
 */
@Composable
fun rememberIslandActivities(
    labelForPkg: (String) -> String,
    dismissedBurstAt: Long,
): List<IslandActivity> {
    val ctx = LocalContext.current
    val connected by BadgeStore.connected.collectAsState()
    val counts by BadgeStore.counts.collectAsState()

    // --- Charging (sticky broadcast; the register call returns the last intent) ---
    var charging by remember { mutableStateOf<IslandActivity.Charging?>(null) }
    DisposableEffect(ctx) {
        val parse: (Intent?) -> Unit = { intent ->
            if (intent == null) {
                charging = null
            } else {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = if (status == BatteryManager.BATTERY_STATUS_CHARGING &&
                    level >= 0 && scale > 0
                ) {
                    IslandActivity.Charging((level * 100 / scale).coerceIn(0, 100))
                } else {
                    null
                }
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = parse(intent)
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            ctx.registerReceiver(receiver, filter)
        }
        parse(sticky)
        onDispose {
            try {
                ctx.unregisterReceiver(receiver)
            } catch (t: Throwable) {
                // receiver already torn down with the host; nothing to undo
            }
        }
    }

    // --- Media (callbacks only, 3.1 A12: NO 2s poll) ---
    // The session list listener fires when a player starts/stops;
    // per-controller callbacks fire on track and play-state changes.
    // Position ticks also land in onPlaybackStateChanged on some
    // players, so that callback only flips the playing flag (and
    // only when it actually flips) instead of re-reading sessions.
    // Everything runs on the main thread: getActiveSessions is one
    // quick binder call, and the effect owns all registrations.
    var media by remember { mutableStateOf<IslandActivity.Media?>(null) }
    DisposableEffect(ctx, connected) {
        var callbacks: List<Pair<MediaController, MediaController.Callback>> = emptyList()
        var listener: MediaSessionManager.OnActiveSessionsChangedListener? = null
        var mm: MediaSessionManager? = null
        fun clearCallbacks() {
            for ((controller, callback) in callbacks) {
                try {
                    controller.unregisterCallback(callback)
                } catch (t: Throwable) {
                    // controller already dead; nothing to unregister
                }
            }
            callbacks = emptyList()
        }
        fun refresh() {
            val sessions = try {
                mm?.getActiveSessions(ComponentName(ctx, HavenNotificationListener::class.java))
                    ?: emptyList()
            } catch (t: Throwable) {
                // 3.1.1 (item 2): a persistent session-list failure
                // disables the island for the session (logged + one
                // toast, first failure only — the host unmounts the
                // island, so this cannot spam).
                if (!FeatureSessionState.isDisabled(FeatureId.ISLAND)) {
                    logFeatureError(ctx, FeatureId.ISLAND, t)
                    FeatureSessionState.disable(FeatureId.ISLAND)
                    try {
                        Toast.makeText(ctx, FeatureId.ISLAND.toastText, Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {
                    }
                }
                emptyList()
            }
            clearCallbacks()
            callbacks = sessions.mapNotNull { controller ->
                val callback = object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        refresh()
                    }

                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        val playing = state?.state == PlaybackState.STATE_PLAYING
                        val current = media
                        if (current != null && current.controller == controller &&
                            current.playing != playing
                        ) {
                            media = current.copy(playing = playing)
                        }
                    }

                    override fun onSessionDestroyed() {
                        refresh()
                    }
                }
                try {
                    controller.registerCallback(callback)
                    controller to callback
                } catch (t: Throwable) {
                    null
                }
            }
            media = mediaFromSessions(sessions)
        }
        if (connected) {
            mm = try {
                ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            } catch (t: Throwable) {
                null
            }
            if (mm != null) {
                listener = MediaSessionManager.OnActiveSessionsChangedListener { refresh() }
                try {
                    mm.addOnActiveSessionsChangedListener(
                        listener,
                        ComponentName(ctx, HavenNotificationListener::class.java),
                    )
                } catch (t: Throwable) {
                    // listener unavailable; the first refresh below
                    // still shows the current session, if any
                }
                refresh()
            } else {
                media = null
            }
        } else {
            media = null
        }
        onDispose {
            try {
                listener?.let { mm?.removeOnActiveSessionsChangedListener(it) }
            } catch (t: Throwable) {
                // manager already gone; callbacks are cleared below
            }
            clearCallbacks()
        }
    }

    // --- Notifications: transient burst window ---
    val total = counts.values.sum()
    var lastIncreaseMs by remember { mutableLongStateOf(0L) }
    var prevTotal by remember { mutableStateOf(0) }
    // Clock state, advanced once a second while a burst is visible, so
    // the window expiry recomposes (and hides the pill) on its own.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(total) {
        if (total > prevTotal) lastIncreaseMs = System.currentTimeMillis()
        prevTotal = total
    }
    val notifVisible = total > 0 && lastIncreaseMs > dismissedBurstAt &&
        nowMs - lastIncreaseMs < NOTIFICATION_WINDOW_MS
    LaunchedEffect(notifVisible) {
        while (notifVisible) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val notifications = if (notifVisible) {
        IslandActivity.Notifications(
            total = total,
            apps = counts.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .map { IslandAppCount(it.key, labelForPkg(it.key), it.value) },
        )
    } else {
        null
    }

    return listOfNotNull(notifications, media, charging)
        .sortedBy { islandPriority(it) }
}

/**
 * Smallest media pick from an already-fetched session list: answers
 * title/artist/playing + controls. 3.1 A12: the session fetch moved
 * into the callback effect above; this pure pick is shared by the
 * initial refresh and every callback-driven refresh.
 */
private fun mediaFromSessions(sessions: List<MediaController>): IslandActivity.Media? = try {
    val controller: MediaController =
        sessions.firstOrNull { it.metadata != null } ?: sessions.firstOrNull() ?: return null
    val md: MediaMetadata? = controller.metadata
    IslandActivity.Media(
        title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() } ?: "Unknown title",
        artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
        playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
        controller = controller,
    )
} catch (t: Throwable) {
    null
}

/** Media transport actions, best-effort: a dead session must never crash home. */
fun IslandActivity.Media.togglePlayPause() {
    try {
        if (playing) controller?.transportControls?.pause()
        else controller?.transportControls?.play()
    } catch (t: Throwable) {
        // session gone mid-tap; the session callback clears the activity
    }
}

fun IslandActivity.Media.skipPrevious() {
    try {
        controller?.transportControls?.skipToPrevious()
    } catch (t: Throwable) {
        // session gone mid-tap; the session callback clears the activity
    }
}

fun IslandActivity.Media.skipNext() {
    try {
        controller?.transportControls?.skipToNext()
    } catch (t: Throwable) {
        // session gone mid-tap; the session callback clears the activity
    }
}
