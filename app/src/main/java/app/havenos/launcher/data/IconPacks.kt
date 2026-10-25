package app.havenos.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Xml
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

data class IconPack(val pkg: String, val label: String)

/**
 * Minimal icon-pack support: discover packs via the de-facto theme intents,
 * read their appfilter.xml (resource or asset), map components to drawables.
 * Everything is best-effort and cached; a broken pack silently falls back to
 * the app's own icon.
 */
object IconPacks {
    private val THEME_ACTIONS = listOf(
        "com.anddoes.launcher.THEME",
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.gridappsinc.launcher.theme",
    )

    // Written from parallel icon loads (Dispatchers.Default) and discovery (IO).
    private val mapCache = java.util.concurrent.ConcurrentHashMap<String, Map<String, String>>()
    private val resCache = java.util.concurrent.ConcurrentHashMap<String, android.content.res.Resources>()

    fun discover(ctx: Context): List<IconPack> {
        val pm = ctx.packageManager
        val found = linkedMapOf<String, String>()
        for (action in THEME_ACTIONS) {
            try {
                val infos = pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
                for (info in infos) {
                    val pkg = info.activityInfo.packageName
                    if (pkg !in found) {
                        found[pkg] = info.loadLabel(pm)?.toString() ?: pkg
                    }
                }
            } catch (t: Throwable) {
                // keep going with the other actions
            }
        }
        return found.map { (pkg, label) -> IconPack(pkg, label) }
    }

    fun iconFor(ctx: Context, pack: String, pkg: String, cls: String): Drawable? {
        val map = mapping(ctx, pack)
        val name = map["$pkg/$cls"] ?: map[pkg] ?: return null
        return try {
            val res = resources(ctx, pack) ?: return null
            val id = res.getIdentifier(name, "drawable", pack)
            if (id == 0) null else res.getDrawable(id, null)
        } catch (t: Throwable) {
            null
        }
    }

    private fun resources(ctx: Context, pack: String): android.content.res.Resources? {
        resCache[pack]?.let { return it }
        return try {
            ctx.packageManager.getResourcesForApplication(pack).also { resCache[pack] = it }
        } catch (t: Throwable) {
            null
        }
    }

    private fun mapping(ctx: Context, pack: String): Map<String, String> {
        mapCache[pack]?.let { return it }
        val out = mutableMapOf<String, String>()
        try {
            val res = resources(ctx, pack)
            val xmlId = res?.getIdentifier("appfilter", "xml", pack) ?: 0
            if (res != null && xmlId != 0) {
                parseAppfilter(res.getXml(xmlId), out)
            } else {
                // Fall back to an assets/appfilter.xml.
                val am = ctx.createPackageContext(pack, 0).assets
                am.open("appfilter.xml").use { stream ->
                    val parser = Xml.newPullParser()
                    parser.setInput(stream, null)
                    parseAppfilter(parser, out)
                }
            }
        } catch (t: Throwable) {
            // broken pack: empty mapping, system icons are used
        }
        mapCache[pack] = out
        return out
    }

    private fun parseAppfilter(parser: XmlPullParser, out: MutableMap<String, String>) {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component") ?: ""
                val drawable = parser.getAttributeValue(null, "drawable") ?: ""
                // component looks like ComponentInfo{pkg/cls} or pkg/cls
                val inner = component.substringAfter('{', "").substringBefore('}', "")
                    .ifBlank { component }
                if (inner.isNotBlank() && drawable.isNotBlank()) out[inner] = drawable
            }
            event = parser.next()
        }
    }
}
