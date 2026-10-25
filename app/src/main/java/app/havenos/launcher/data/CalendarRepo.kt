package app.havenos.launcher.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import org.json.JSONArray
import java.util.Calendar

data class CalEvent(
    val title: String,
    val beginMs: Long,
    val color: Int,
    /** CalendarContract Instances row id; 0 when unknown. */
    val id: Long = 0L,
    /** All-day events sort first in day view (Doc 3 §2.5). */
    val allDay: Boolean = false,
)
data class CalInfo(val id: Long, val name: String)

/** Parse Settings.calendarsJson: "" / blank -> null (all calendars). Never throws. */
fun parseAllowedCalendars(json: String): Set<Long>? {
    if (json.isBlank()) return null
    return try {
        val arr = JSONArray(json)
        val out = mutableSetOf<Long>()
        for (i in 0 until arr.length()) out += arr.optLong(i, -1L)
        out - (-1L)
    } catch (t: Throwable) {
        null
    }
}

/**
 * Calendar reads. The CALLER checks READ_CALENDAR first; every query here
 * still try/catches and returns empty on any failure (revoked grant, OEM quirks).
 */
object CalendarRepo {

    fun listCalendars(ctx: Context): List<CalInfo> = try {
        val out = mutableListOf<CalInfo>()
        val proj = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        ctx.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, proj, null, null, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            while (c.moveToNext()) {
                out += CalInfo(c.getLong(idIdx), c.getString(nameIdx) ?: "")
            }
        }
        out
    } catch (t: Throwable) {
        emptyList()
    }

    /**
     * [dayOffset] != null -> events of that single day (0 = today, 1 = tomorrow).
     * [dayOffset] == null -> [now, now + 14 days).
     */
    fun upcoming(ctx: Context, limit: Int, allowedIds: Set<Long>?, dayOffset: Int?): List<CalEvent> {
        val begin: Long
        val end: Long
        if (dayOffset != null) {
            val start = startOfDay(System.currentTimeMillis())
            begin = start + dayOffset * 86_400_000L
            end = begin + 86_400_000L
        } else {
            begin = System.currentTimeMillis()
            end = begin + 14 * 86_400_000L
        }
        // Chronological, with all-day events first within each day.
        return queryWindow(ctx, begin, end, limit * 4)
            .filter { allowedIds == null || it.calendarId in allowedIds }
            .sortedWith(
                compareBy<RawEvent> { startOfDay(it.beginMs) }
                    .thenByDescending { it.allDay }
                    .thenBy { it.beginMs },
            )
            .take(limit)
            .map { CalEvent(it.title, it.beginMs, it.color, it.id, it.allDay) }
    }

    /** Days of the given month (1-based day-of-month set) that have at least one event. */
    fun monthEventDays(ctx: Context, year: Int, month: Int, allowedIds: Set<Long>?): Set<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val begin = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        val dayCal = Calendar.getInstance()
        return queryWindow(ctx, begin, end, 600)
            .filter { allowedIds == null || it.calendarId in allowedIds }
            .map { e -> dayCal.apply { timeInMillis = e.beginMs }.get(Calendar.DAY_OF_MONTH) }
            .toSet()
    }

    private data class RawEvent(
        val title: String,
        val beginMs: Long,
        val color: Int,
        val calendarId: Long,
        val id: Long = 0L,
        val allDay: Boolean = false,
    )

    private fun queryWindow(ctx: Context, begin: Long, end: Long, limit: Int): List<RawEvent> = try {
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, begin)
        ContentUris.appendId(builder, end)
        val proj = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.ALL_DAY,
        )
        val out = mutableListOf<RawEvent>()
        ctx.contentResolver.query(builder.build(), proj, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            val rowIdx = c.getColumnIndexOrThrow(CalendarContract.Instances._ID)
            val tIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val bIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val colIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)
            val idIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val allDayIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            while (c.moveToNext() && out.size < limit) {
                out += RawEvent(
                    title = c.getString(tIdx) ?: "(untitled)",
                    beginMs = c.getLong(bIdx),
                    color = c.getInt(colIdx),
                    calendarId = c.getLong(idIdx),
                    id = c.getLong(rowIdx),
                    allDay = c.getInt(allDayIdx) == 1,
                )
            }
        }
        out
    } catch (t: Throwable) {
        emptyList()
    }

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
