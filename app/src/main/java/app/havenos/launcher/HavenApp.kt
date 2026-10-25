package app.havenos.launcher

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import app.havenos.launcher.data.AppRepository
import app.havenos.launcher.data.HavenDb
import app.havenos.launcher.data.LayoutCoordinator
import app.havenos.launcher.data.Settings
import app.havenos.launcher.data.TasksStore
import app.havenos.launcher.data.WallpaperStore
import app.havenos.launcher.data.WeatherStore
import app.havenos.launcher.data.buildBackupJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class HavenApp : Application() {
    lateinit var db: HavenDb
        private set
    lateinit var settings: Settings
        private set
    /** M9 Worker D: the single state-layer writer (profiles, undo, snapshots, transactions). */
    lateinit var layoutStore: LayoutCoordinator
        private set
    lateinit var appRepo: AppRepository
        private set
    lateinit var wallpaper: WallpaperStore
        private set
    lateinit var weather: WeatherStore
        private set
    lateinit var tasks: TasksStore
        private set
    lateinit var widgetHost: AppWidgetHost
        private set
    lateinit var widgetManager: AppWidgetManager
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashLog()
        db = HavenDb.create(this)
        settings = Settings(this)
        layoutStore = LayoutCoordinator(this, db, settings)
        appRepo = AppRepository(this, settings)
        wallpaper = WallpaperStore(this)
        weather = WeatherStore(this)
        tasks = TasksStore(this)
        widgetManager = AppWidgetManager.getInstance(this)
        widgetHost = AppWidgetHost(this, WIDGET_HOST_ID)
        maybeDailyBackup()
    }

    // shortcut: daily backup runs on first launch of the day, not via WorkManager; upgrade if exact timing matters.
    private fun maybeDailyBackup() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Read the persisted state directly — never the pre-load default.
                val st = settings.persistedState()
                val today = System.currentTimeMillis() / 86_400_000L
                if (today > st.lastBackupDay) {
                    val json = buildBackupJson(this@HavenApp, st, st.backupIncludeWp)
                    File(filesDir, "backup-auto.json").writeText(json)
                    settings.setLastBackupDay(today)
                }
            } catch (t: Throwable) {
                // auto-backup is best-effort
            }
        }
    }

    /** Keep the last crash's stack trace for Settings → About & Help → Crash log. */
    private fun installCrashLog() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val text = buildString {
                    appendLine("HavenOS crash — ${java.util.Date()}")
                    appendLine(error.stackTraceToString())
                }
                File(filesDir, "last_crash.txt").writeText(text)
            } catch (t: Throwable) {
                // never mask the original crash
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        const val WIDGET_HOST_ID = 1024

        fun of(ctx: Context): HavenApp = ctx.applicationContext as HavenApp
    }
}
