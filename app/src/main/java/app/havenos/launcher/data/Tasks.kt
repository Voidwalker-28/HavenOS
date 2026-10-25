package app.havenos.launcher.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class TaskItem(val text: String, val done: Boolean)

/** Tiny task list persisted to filesDir/tasks.json. In-memory list is the source of truth. */
class TasksStore(ctx: Context) {
    private val file = File(ctx.applicationContext.filesDir, "tasks.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val tasks = MutableStateFlow<List<TaskItem>>(emptyList())

    init {
        // Off the main thread: this store is constructed in Application.onCreate.
        scope.launch { tasks.value = load() }
    }

    @Synchronized
    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        update(tasks.value + TaskItem(t, false))
    }

    @Synchronized
    fun toggle(index: Int) {
        val cur = tasks.value
        if (index !in cur.indices) return
        update(cur.mapIndexed { i, item -> if (i == index) item.copy(done = !item.done) else item })
    }

    @Synchronized
    fun remove(index: Int) {
        val cur = tasks.value
        if (index !in cur.indices) return
        update(cur.filterIndexed { i, _ -> i != index })
    }

    private fun update(next: List<TaskItem>) {
        tasks.value = next
        scope.launch {
            try {
                val arr = JSONArray()
                for (t in next) arr.put(JSONObject().put("text", t.text).put("done", t.done))
                file.writeText(arr.toString())
            } catch (t: Throwable) {
                // persistence is best-effort; the in-memory list still works
            }
        }
    }

    private fun load(): List<TaskItem> {
        return try {
            if (!file.exists()) return emptyList()
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                TaskItem(o.optString("text"), o.optBoolean("done", false))
            }.filter { it.text.isNotBlank() }
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
