package app.havenos.launcher.data

// 3.1 Phase F (F4): the AI Privacy Ledger's stored facts. Counts and times
// ONLY — never content. No Android import: this file is pure so the ledger
// tests run on the plain JVM. The ledger JSONs are flat string maps kept
// deliberately hand-rolled (no org.json) for the same reason.

/** Every assist feature the ledger lists (prompt §F4, exactly these five). */
enum class AssistFeatureId(val id: String) {
    SEARCH_COMMANDS("search_commands"),
    APP_PREDICTION("app_prediction"),
    MASCOT_FIND("mascot_find"),
    MASCOT_CHAT("mascot_chat"),
    SUGGESTIONS("suggestions"),
}

/** Tolerant flat-map parse: garbage in yields an empty map, never a crash. */
fun parseStringLongMap(json: String): Map<String, Long> {
    val out = LinkedHashMap<String, Long>()
    for (part in json.removePrefix("{").removeSuffix("}").split(",")) {
        val kv = part.split(":")
        if (kv.size != 2) continue
        val key = kv[0].trim().removeSurrounding("\"")
        val value = kv[1].trim().toLongOrNull() ?: continue
        if (key.isNotEmpty()) out[key] = value
    }
    return out
}

fun parseStringIntMap(json: String): Map<String, Int> {
    val out = LinkedHashMap<String, Int>()
    for (part in json.removePrefix("{").removeSuffix("}").split(",")) {
        val kv = part.split(":")
        if (kv.size != 2) continue
        val key = kv[0].trim().removeSurrounding("\"")
        val value = kv[1].trim().toIntOrNull() ?: continue
        if (key.isNotEmpty()) out[key] = value
    }
    return out
}

fun encodeStringLongMap(map: Map<String, Long>): String =
    map.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\":${it.value}" }

fun encodeStringIntMap(map: Map<String, Int>): String =
    map.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\":${it.value}" }

/**
 * Record one use of a feature: last-used time + incremented count, in a
 * single pair so the caller can persist both JSONs in one DataStore write.
 */
fun recordAssistUse(
    lastUsedJson: String,
    countsJson: String,
    feature: AssistFeatureId,
    nowMs: Long,
): Pair<String, String> {
    val lastUsed = parseStringLongMap(lastUsedJson).toMutableMap()
    val counts = parseStringIntMap(countsJson).toMutableMap()
    lastUsed[feature.id] = nowMs
    counts[feature.id] = (counts[feature.id] ?: 0) + 1
    return encodeStringLongMap(lastUsed) to encodeStringIntMap(counts)
}

/** "Clear learned data": both ledger JSONs back to empty. */
fun clearAssistLedgerJsons(): Pair<String, String> = "{}" to "{}"
