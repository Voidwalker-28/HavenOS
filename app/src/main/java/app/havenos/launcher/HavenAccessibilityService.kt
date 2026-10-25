package app.havenos.launcher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings as GlobalSettings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import app.havenos.launcher.data.EXTRA_NAV_ACTION
import app.havenos.launcher.data.NavEdge
import app.havenos.launcher.data.Settings
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.data.classifySwipe
import app.havenos.launcher.data.navThresholdPx
import app.havenos.launcher.data.parseNavMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val NAV_POLICY_KEY = "policy_control"
private const val NAV_HIDE_VALUE = "immersive.navigation=*"
private const val NAV_HOLD_MS = 450L

/** True when the user has enabled this service in system Accessibility settings. */
fun isNavServiceEnabled(ctx: Context): Boolean = try {
    val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        ?.any {
            it.resolveInfo.serviceInfo.packageName == ctx.packageName &&
                it.resolveInfo.serviceInfo.name == HavenAccessibilityService::class.java.name
        } ?: false
} catch (t: Throwable) {
    false
}

/** True when WRITE_SECURE_SETTINGS was granted via adb (`pm grant`). */
fun hideBarGranted(ctx: Context): Boolean = try {
    ctx.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
        PackageManager.PERMISSION_GRANTED
} catch (t: Throwable) {
    false
}

/** Serializes policy syncs: racing on/off callers must not interleave. */
private val navPolicyMutex = Mutex()

/**
 * Keep Settings.Global "policy_control" in sync with the nav settings: hide the
 * system nav bar (immersive) only while gestures are on AND the accessibility
 * service is actually running with overlay permission AND the hide-bar toggle
 * and grant are on; restore the user's original value (saved once) otherwise.
 * Never throws.
 */
suspend fun syncNavBarPolicy(ctx: Context, settings: Settings) {
    navPolicyMutex.withLock {
        try {
            val st = settings.persistedState()
            val wantHide = st.navGesturesOn && st.navHideBar && hideBarGranted(ctx) &&
                isNavServiceEnabled(ctx) && GlobalSettings.canDrawOverlays(ctx)
            val cr = ctx.contentResolver
            if (wantHide) {
                val current = GlobalSettings.Global.getString(cr, NAV_POLICY_KEY)
                if (current != NAV_HIDE_VALUE) {
                    if (!st.navSavedPolicySet) settings.setNavSavedPolicy(current ?: "", true)
                    GlobalSettings.Global.putString(cr, NAV_POLICY_KEY, NAV_HIDE_VALUE)
                } else if (!st.navSavedPolicySet) {
                    // Bar already hidden but the prior value was never saved
                    // (e.g. app data cleared while hidden): adopt the key so a
                    // later restore deletes it and returns to the system default.
                    settings.setNavSavedPolicy("", true)
                }
            } else if (st.navSavedPolicySet) {
                restoreSavedPolicy(ctx, settings, st)
            }
        } catch (t: Throwable) {
            // secure-settings writes can fail (grant revoked mid-flight); best-effort
        }
    }
}

/** Restore half of [syncNavBarPolicy], for when the service goes away. Never throws. */
suspend fun restoreNavBarPolicy(ctx: Context, settings: Settings) {
    navPolicyMutex.withLock {
        try {
            val st = settings.persistedState()
            if (st.navSavedPolicySet) restoreSavedPolicy(ctx, settings, st)
        } catch (t: Throwable) {
            // best-effort
        }
    }
}

private suspend fun restoreSavedPolicy(ctx: Context, settings: Settings, st: SettingsState) {
    // putString(null) deletes the key, returning the system to its default.
    GlobalSettings.Global.putString(
        ctx.contentResolver, NAV_POLICY_KEY,
        if (st.navSavedPolicy.isEmpty()) null else st.navSavedPolicy,
    )
    settings.setNavSavedPolicy("", false)
}

/**
 * Edge-swipe navigation (FNG-style). Declares canRetrieveWindowContent=false and
 * never reads screen content; it only owns three transparent overlay strips that
 * translate edge swipes into global Back/Home/Recents actions.
 */
class HavenAccessibilityService : AccessibilityService() {

    private var scope: CoroutineScope? = null
    private var windowManager: WindowManager? = null
    private val overlays = mutableListOf<View>()
    private var appliedKey: String? = null
    private var lastState: SettingsState? = null
    private var restoreLaunched = false

    // Live config, read by the edge views; refreshed on every rebuild.
    private var parsedMap: Map<String, String> = emptyMap()
    private var thresholdPx: Float = 0f
    private var hapticsOn: Boolean = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        restoreLaunched = false
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        scope?.cancel()
        val settings = HavenApp.of(this).settings
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { s ->
            s.launch {
                settings.state.collect { st ->
                    lastState = st
                    reconcile(st)
                    syncNavBarPolicy(this@HavenAccessibilityService, settings)
                }
            }
        }
    }

    // We never read screen content; both callbacks are intentionally empty.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        appliedKey = null // force rebuild with the new metrics
        lastState?.let { reconcile(it) }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        removeOverlays()
        scope?.cancel()
        scope = null
        // The service is gone, so nothing will re-hide the bar: put the user's back.
        if (!restoreLaunched) {
            restoreLaunched = true
            CoroutineScope(Dispatchers.IO).launch {
                restoreNavBarPolicy(
                    this@HavenAccessibilityService,
                    HavenApp.of(this@HavenAccessibilityService).settings,
                )
            }
        }
    }

    private fun reconcile(state: SettingsState) {
        if (!state.navGesturesOn || !GlobalSettings.canDrawOverlays(this)) {
            removeOverlays()
            appliedKey = null
            return
        }
        val key = "${state.navEdgeDp}|${state.navSensitivity}|${state.navHaptics}|${state.navMapJson}"
        if (overlays.isNotEmpty() && key == appliedKey) return
        removeOverlays()
        appliedKey = key
        parsedMap = parseNavMap(state.navMapJson)
        hapticsOn = state.navHaptics
        val density = resources.displayMetrics.density
        thresholdPx = navThresholdPx(state.navSensitivity, density)
        val edgePx = (state.navEdgeDp * density).toInt().coerceAtLeast(1)
        val sideHeight = (resources.displayMetrics.heightPixels * 0.6f).toInt()
        addOverlay(NavEdge.BOTTOM, WindowManager.LayoutParams.MATCH_PARENT, edgePx, Gravity.BOTTOM)
        addOverlay(NavEdge.LEFT, edgePx, sideHeight, Gravity.LEFT or Gravity.CENTER_VERTICAL)
        addOverlay(NavEdge.RIGHT, edgePx, sideHeight, Gravity.RIGHT or Gravity.CENTER_VERTICAL)
    }

    private fun addOverlay(edge: NavEdge, width: Int, height: Int, gravity: Int) {
        val wm = windowManager ?: return
        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { this.gravity = gravity }
        val view = EdgeView(edge)
        try {
            wm.addView(view, params)
            overlays += view
        } catch (t: Throwable) {
            // overlay permission revoked between the check and here; stay down.
            // All-or-nothing: drop any strips already added so the next
            // reconcile retries from a clean slate.
            appliedKey = null
            removeOverlays()
        }
    }

    private fun removeOverlays() {
        val wm = windowManager
        for (view in overlays) {
            try {
                wm?.removeView(view)
            } catch (t: Throwable) {
                // not attached; nothing to remove
            }
        }
        overlays.clear()
    }

    private fun fire(trigger: String, view: View) {
        try {
            if (hapticsOn) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            when (parsedMap[trigger]) {
                "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "quickPanel" -> startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(EXTRA_NAV_ACTION, "quickPanel")
                    },
                )
                else -> Unit // "none" or unmapped trigger
            }
        } catch (t: Throwable) {
            // a dead service must never crash
        }
    }

    /**
     * One transparent edge strip. Side edges fire the moment [classifySwipe]
     * triggers; the bottom edge distinguishes a swipe (fires "up" on lift) from
     * a swipe-and-hold (fires "upHold" after [NAV_HOLD_MS] if the finger is
     * still down and still beyond 0.7× threshold). After any fire, events are
     * ignored until the next ACTION_DOWN.
     */
    private inner class EdgeView(private val edge: NavEdge) : View(this@HavenAccessibilityService) {

        private val handler = Handler(Looper.getMainLooper())
        private var startX = 0f
        private var startY = 0f
        private var lastDy = 0f
        private var down = false
        private var fired = false
        private var crossed = false

        private val holdRunnable = Runnable {
            if (down && !fired && -lastDy > 0.7f * thresholdPx) {
                fired = true
                fire("upHold", this)
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            // A torn-down view must not fire a phantom upHold later.
            handler.removeCallbacks(holdRunnable)
            down = false
            fired = false
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    down = true
                    fired = false
                    crossed = false
                    startX = event.x
                    startY = event.y
                    lastDy = 0f
                    handler.removeCallbacks(holdRunnable)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!down || fired) return true
                    val dx = event.x - startX
                    val dy = event.y - startY
                    lastDy = dy
                    if (edge == NavEdge.BOTTOM) {
                        if (!crossed && classifySwipe(edge, dx, dy, thresholdPx) == "up") {
                            crossed = true
                            handler.postDelayed(holdRunnable, NAV_HOLD_MS)
                        }
                    } else {
                        val trigger = classifySwipe(edge, dx, dy, thresholdPx)
                        if (trigger != null) {
                            fired = true
                            fire(trigger, this)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(holdRunnable)
                    // Fire only if the final movement still classifies as this
                    // edge's swipe at half threshold — a swipe that retreated
                    // or went diagonal before release is a cancel, not a press.
                    if (edge == NavEdge.BOTTOM && down && !fired && crossed &&
                        classifySwipe(edge, event.x - startX, event.y - startY, thresholdPx * 0.5f) == "up"
                    ) {
                        fired = true
                        fire("up", this)
                    }
                    down = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(holdRunnable)
                    down = false
                }
            }
            return true
        }
    }
}
