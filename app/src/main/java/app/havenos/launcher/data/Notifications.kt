package app.havenos.launcher.data

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Badge counts + Now Playing both ride on notification access. The listener
 * keeps a per-package count of posted notifications; the media side reads
 * MediaSessionManager active sessions with this component name. When access
 * is not granted, [BadgeStore.connected] stays false and the UI shows an
 * honest enable prompt instead of silently empty widgets.
 */
object BadgeStore {
    /** package name -> active notification count. */
    val counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val connected = MutableStateFlow(false)
}

class HavenNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        BadgeStore.connected.value = true
        recompute()
    }

    override fun onListenerDisconnected() {
        BadgeStore.connected.value = false
        BadgeStore.counts.value = emptyMap()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = recompute()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = recompute()

    private fun recompute() {
        try {
            val active = activeNotifications ?: return
            val counts = mutableMapOf<String, Int>()
            for (sbn in active) {
                if (sbn.isOngoing && sbn.notification?.category ==
                    android.app.Notification.CATEGORY_TRANSPORT
                ) {
                    continue // media sessions are not badges
                }
                // A grouped notification posts a summary AND its children;
                // counting the summary would inflate the count (e.g. 3
                // messages showing as 4). Count children only.
                val flags = sbn.notification?.flags ?: 0
                if (flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) continue
                counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
            }
            BadgeStore.counts.value = counts
        } catch (t: Throwable) {
            // listener mid-teardown; keep the last known counts
        }
    }
}
