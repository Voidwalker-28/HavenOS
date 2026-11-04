package app.havenos.launcher.ui.assist

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.havenos.launcher.ui.mascot.BrainContext
import app.havenos.launcher.ui.mascot.BrainInput
import app.havenos.launcher.ui.mascot.MascotBrain
import app.havenos.launcher.ui.mascot.MascotResponse
import app.havenos.launcher.ui.mascot.ScriptedBrain
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

// 3.1 Phase F (F5): the local model pack, SCAFFOLD ONLY. No model is
// bundled, no runtime is picked, and nothing downloads unless the user
// asks. The URL and checksum stay EMPTY placeholders, exactly as the
// manual states — inventing values would point a download at a file
// nobody vetted. Phase 3 already concluded MediaPipe / llama.cpp /
// ExecuTorch do not integrate cleanly here, so no runtime is wired:
// LocalModelBrain delegates to the scripted brain until one does.

/** Feature flag, mirroring Phase E's USE_PIXEL_MASCOT pattern. */
const val USE_MODEL_BRAIN = false

/** Empty placeholders. Do not fill without a vetted model + checksum. */
const val MODEL_PACK_URL = ""
const val MODEL_PACK_SHA256 = ""

enum class ModelPackStatus { NOT_SET_UP, DOWNLOADING, READY, FAILED }

data class ModelPackInfo(
    val status: ModelPackStatus,
    val progress: Float = 0f,
    val bytes: Long = 0L,
    val sha256: String? = null,
    val error: String? = null,
)

fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(bytes)
    return hash.joinToString("") { "%02x".format(it) }
}

/** An empty expected checksum never verifies: no silent trust. */
fun verifyPackBytes(bytes: ByteArray, expectedSha256: String?): Boolean {
    if (expectedSha256.isNullOrBlank()) return false
    return sha256Hex(bytes) == expectedSha256.lowercase()
}

/** Pure download gates, evaluated before any network use. */
data class DownloadGate(
    val url: String,
    val wifiOnly: Boolean,
    val wifiConnected: Boolean,
    val freeBytes: Long,
    val expectedBytes: Long,
    val totalRamBytes: Long,
)

/** Scaffold floors, not measured model requirements: 100 MB headroom over
 * the pack size, 1 GB total RAM. A real pack revisits both. */
private const val PACK_HEADROOM_BYTES = 100L * 1024 * 1024
private const val MIN_RAM_BYTES = 1L * 1024 * 1024 * 1024

fun downloadGateError(gate: DownloadGate): String? = when {
    gate.url.isBlank() -> "No model URL configured"
    gate.wifiOnly && !gate.wifiConnected -> "Wi-Fi required"
    gate.freeBytes < gate.expectedBytes + PACK_HEADROOM_BYTES -> "Not enough free space"
    gate.totalRamBytes < MIN_RAM_BYTES -> "Not enough memory"
    else -> null
}

/** Pure status derivation from file presence (manager supplies the inputs). */
fun derivePackStatus(
    hasPack: Boolean,
    hasTmp: Boolean,
    @Suppress("UNUSED_PARAMETER") tmpBytes: Long,
    @Suppress("UNUSED_PARAMETER") expectedBytes: Long,
    shaMatches: Boolean,
): ModelPackStatus = when {
    !hasPack && hasTmp -> ModelPackStatus.DOWNLOADING
    !hasPack -> ModelPackStatus.NOT_SET_UP
    shaMatches -> ModelPackStatus.READY
    else -> ModelPackStatus.FAILED
}

/** Wi-Fi check for the download gate (no new permission: uses the normal
 * connectivity service, not a network request). */
fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

/**
 * App-private model-pack storage (filesDir/model-pack). The sidecar
 * records the verified sha + byte count; READY trusts pack length vs
 * sidecar (a full re-hash of a large file on every read would stall the
 * hub) — the SHA-256 check itself runs at download time, before rename.
 */
class ModelPackManager(private val filesDir: File) {
    private val dir get() = File(filesDir, "model-pack")
    private val packFile get() = File(dir, "model.bin")
    private val tmpFile get() = File(dir, "model.bin.tmp")
    private val sidecarFile get() = File(dir, "pack.json")

    fun packState(): ModelPackInfo {
        val hasPack = packFile.exists()
        val hasTmp = tmpFile.exists()
        if (hasPack) {
            val sidecar = readSidecar()
            val shaMatches = sidecar != null && sidecar.first == packFile.length()
            val status = derivePackStatus(true, hasTmp, 0L, 0L, shaMatches)
            return ModelPackInfo(status, bytes = packFile.length(), sha256 = sidecar?.second)
        }
        if (hasTmp) {
            val expected = readSidecar()?.first ?: 0L
            val progress = if (expected > 0) tmpFile.length().toFloat() / expected else 0f
            return ModelPackInfo(ModelPackStatus.DOWNLOADING, progress = progress, bytes = tmpFile.length())
        }
        return ModelPackInfo(ModelPackStatus.NOT_SET_UP)
    }

    private fun readSidecar(): Pair<Long, String>? {
        return try {
            if (!sidecarFile.exists()) return null
            val json = JSONObject(sidecarFile.readText())
            json.getLong("bytes") to json.getString("sha256")
        } catch (t: Throwable) {
            null
        }
    }

    private fun writeSidecar(bytes: Long, sha256: String) {
        dir.mkdirs()
        val json = JSONObject().put("bytes", bytes).put("sha256", sha256)
        sidecarFile.writeText(json.toString())
    }

    /**
     * Download on user request only. The empty-URL gate fires before any
     * network use, so "no new network calls" stays true in practice
     * until a real pack is configured. Cancellation deletes the partial
     * file; a checksum mismatch deletes it too. Returns the new state.
     */
    suspend fun download(
        url: String,
        expectedSha256: String,
        wifiOnly: Boolean,
        wifiConnected: Boolean,
        totalRamBytes: Long,
        onProgress: (Float) -> Unit,
    ): ModelPackInfo = withContext(Dispatchers.IO) {
        // URL / Wi-Fi / RAM first; free space needs the real length, so
        // it is checked after headers (Long.MAX_VALUE skips it here).
        val preGate = downloadGateError(
            DownloadGate(url, wifiOnly, wifiConnected, Long.MAX_VALUE, 0L, totalRamBytes),
        )
        if (preGate != null) {
            return@withContext ModelPackInfo(ModelPackStatus.FAILED, error = preGate)
        }
        dir.mkdirs()
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                connect()
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext ModelPackInfo(
                    ModelPackStatus.FAILED,
                    error = "Download failed (${connection.responseCode})",
                )
            }
            val expectedBytes = connection.contentLengthLong.takeIf { it > 0 } ?: 0L
            val spaceError = downloadGateError(
                DownloadGate(url, wifiOnly, wifiConnected, dir.usableSpace, expectedBytes, totalRamBytes),
            )
            if (spaceError != null) {
                return@withContext ModelPackInfo(ModelPackStatus.FAILED, error = spaceError)
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var written = 0L
            tmpFile.outputStream().buffered().use { out ->
                val buffer = ByteArray(64 * 1024)
                connection.inputStream.buffered().use { input ->
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                        out.write(buffer, 0, read)
                        written += read
                        if (expectedBytes > 0) onProgress(written.toFloat() / expectedBytes)
                    }
                }
            }
            val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
            // A blank expected checksum never verifies (see verifyPackBytes).
            if (expectedSha256.isBlank() || actualSha != expectedSha256.lowercase()) {
                tmpFile.delete()
                return@withContext ModelPackInfo(ModelPackStatus.FAILED, error = "Checksum mismatch")
            }
            if (tmpFile.renameTo(packFile)) {
                writeSidecar(written, actualSha)
                ModelPackInfo(ModelPackStatus.READY, bytes = written, sha256 = actualSha)
            } else {
                tmpFile.delete()
                ModelPackInfo(ModelPackStatus.FAILED, error = "Could not store model pack")
            }
        } catch (e: CancellationException) {
            tmpFile.delete()
            throw e
        } catch (t: Throwable) {
            tmpFile.delete()
            ModelPackInfo(ModelPackStatus.FAILED, error = t.message ?: "Download failed")
        } finally {
            connection?.disconnect()
        }
    }

    fun delete(): Boolean {
        var ok = true
        if (packFile.exists() && !packFile.delete()) ok = false
        if (tmpFile.exists() && !tmpFile.delete()) ok = false
        if (sidecarFile.exists() && !sidecarFile.delete()) ok = false
        return ok
    }
}

/**
 * The pack-aware chat seam. Behind USE_MODEL_BRAIN and only when a pack
 * is READY; until a runtime integrates cleanly it delegates to the
 * scripted brain, so chat never fabricates answers. A model wakes only
 * via this chat path and never returns a settings-changing action —
 * the scripted brain's actions are the only ones the layer executes.
 */
class LocalModelBrain(
    private val fallback: MascotBrain = ScriptedBrain(),
    private val packReady: () -> Boolean = { false },
) : MascotBrain {
    override suspend fun handle(input: BrainInput, ctx: BrainContext): MascotResponse {
        if (!USE_MODEL_BRAIN || !packReady()) return fallback.handle(input, ctx)
        // No runtime wired: delegation is the honest behaviour.
        return fallback.handle(input, ctx)
    }
}
