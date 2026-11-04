package app.havenos.launcher

import android.app.admin.DeviceAdminReceiver
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.havenos.launcher.data.EXTRA_NAV_ACTION
import app.havenos.launcher.theme.HavenTheme
import app.havenos.launcher.ui.HavenRoot

/** Device-admin receiver backing the "lock" gesture (force-lock policy only). */
class HavenDeviceAdminReceiver : DeviceAdminReceiver()

class MainActivity : ComponentActivity() {

    private var backAction: (() -> Boolean)? = null

    private var quickPanelSignal by mutableIntStateOf(0)

    // M5 §2.1: system "launcher settings" intent (ACTION_APPLICATION_
    // PREFERENCES, declared in the manifest): open full Settings.
    private var settingsSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getStringExtra(EXTRA_NAV_ACTION) == "quickPanel") {
            quickPanelSignal = 1
        }
        if (intent?.action == Intent.ACTION_APPLICATION_PREFERENCES) {
            settingsSignal = 1
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // A launcher never exits on Back: HavenRoot registers a handler for
        // its internal states (folder, drawer, settings, edit mode); anything
        // unhandled is swallowed here.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backAction?.invoke() != true) {
                    // swallow: a launcher never exits on Back
                }
            }
        })

        setContent {
            val app = HavenApp.of(this)
            val state by app.settings.state.collectAsState()
            // 3.1.1 (item 3): Safe Mode forces Solid rendering (blur
            // off) as a theme-level runtime override only — the stored
            // surfaceMaterial is untouched, so Re-enable restores it.
            val safeMode by app.safeMode.collectAsState()
            val themeState = if (safeMode) state.copy(surfaceMaterial = 0) else state
            val insetsController = remember(window) {
                WindowInsetsControllerCompat(window, window.decorView)
            }
            LaunchedEffect(state.navGesturesOn) {
                try {
                    if (state.navGesturesOn) {
                        insetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.navigationBars())
                    }
                } catch (t: Throwable) {
                    // insets are best-effort
                }
            }
            HavenTheme(themeState) {
                HavenRoot(
                    app,
                    onBackAction = { backAction = it },
                    quickPanelSignal = quickPanelSignal,
                    settingsSignal = settingsSignal,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getStringExtra(EXTRA_NAV_ACTION) == "quickPanel") {
            quickPanelSignal++
        }
        if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES) {
            settingsSignal++
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            HavenApp.of(this).widgetHost.startListening()
        } catch (t: Throwable) {
            // hosted widgets are best-effort
        }
    }

    // 3.1 A12: returning to home re-runs Today cards' stale checks
    // (their flows otherwise live as long as home composition does).
    override fun onResume() {
        super.onResume()
        try {
            HavenApp.of(this).noteHomeResumed()
        } catch (t: Throwable) {
            // tick is best-effort; cards still read cache
        }
    }

    override fun onStop() {
        try {
            HavenApp.of(this).widgetHost.stopListening()
        } catch (t: Throwable) {
            // hosted widgets are best-effort
        }
        super.onStop()
    }
}
