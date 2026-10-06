package com.bitvibe.app.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.bitvibe.app.BuildConfig
import com.bitvibe.app.data.repository.SettingsRepository
import com.bitvibe.app.domain.player.MusicController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Self-update from GitHub Releases.
 *
 * The "Android APK" workflow publishes every build of `main` as a release tagged
 * `android-v<versionName>-b<build>`, where <build> is the versionCode. On launch the app
 * compares that with its own versionCode and, when newer, downloads the APK and installs it
 * through [PackageInstaller].
 *
 * Silent installs: on Android 12+ a session marked USER_ACTION_NOT_REQUIRED installs without
 * any prompt once BitVibe is its own "installer of record", i.e. after the first update it
 * installed itself. Those updates are applied when the app goes to the background and nothing
 * is playing, so the user comes back to the new version. Otherwise (first update, Android 11 and
 * older) Android shows its one-tap install confirmation, so we only start those from the app.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val musicController: MusicController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var lastCheckAt = 0L
    /** Whether the app's UI is visible (Android blocks starting the installer dialog otherwise). */
    var inForeground = false
        private set
    private var workJob: Job? = null
    private var pendingBackgroundInstall: Job? = null

    /** Debug builds are signed with a different key and can't install release APKs. */
    val canSelfUpdate: Boolean get() = !BuildConfig.DEBUG

    val currentVersionLabel: String
        get() = "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"

    // ── Lifecycle hooks (called by MainActivity) ─────────────────

    fun onAppForeground() {
        inForeground = true
        pendingBackgroundInstall?.cancel()
        when (val s = _state.value) {
            // Back from the "install unknown apps" settings screen.
            is UpdateState.NeedsPermission -> if (canRequestInstalls()) install(s.update, s.apk)
            else -> {
                if (!canSelfUpdate || workJob?.isActive == true) return
                if (System.currentTimeMillis() - lastCheckAt < CHECK_INTERVAL_MS) return
                workJob = scope.launch { autoCheck() }
            }
        }
    }

    fun onAppBackground() {
        inForeground = false
        val ready = _state.value as? UpdateState.Ready ?: return
        if (!ready.silent) return
        // Install once playback is stopped; replacing the app kills the music.
        pendingBackgroundInstall?.cancel()
        pendingBackgroundInstall = scope.launch {
            musicController.isPlaying.first { !it }
            if (!inForeground) install(ready.update, ready.apk)
        }
    }

    // ── User actions ─────────────────────────────────────────────

    /** Settings → Check for updates: ignores the throttle and a previously skipped build. */
    fun checkNow() {
        if (!canSelfUpdate || workJob?.isActive == true) return
        workJob = scope.launch {
            _state.value = UpdateState.Checking
            val update = fetchLatest()
            _state.value = when {
                update == null -> UpdateState.UpToDate
                else -> UpdateState.Available(update)
            }
        }
    }

    /** "Update now": download (if needed) and install, showing progress. */
    fun downloadAndInstall(update: AvailableUpdate) {
        if (workJob?.isActive == true && _state.value is UpdateState.Downloading) return
        workJob = scope.launch {
            val apk = download(update, userInitiated = true) ?: return@launch
            install(update, apk)
        }
    }

    /** "Later": don't offer this build again (a newer one, or Check for updates, still will). */
    fun skip(update: AvailableUpdate) {
        scope.launch { settingsRepository.setSkippedUpdateBuild(update.build) }
        _state.value = UpdateState.Idle
    }

    fun dismiss() {
        if (_state.value !is UpdateState.Downloading && _state.value !is UpdateState.Ready) {
            _state.value = UpdateState.Idle
        }
    }

    fun installReady() {
        val ready = _state.value as? UpdateState.Ready ?: return
        install(ready.update, ready.apk)
    }

    // ── Installer callbacks (UpdateInstallReceiver) ──────────────

    internal fun onInstallFailed(message: String) {
        Log.w(TAG, "Install failed: $message")
        _state.value = UpdateState.Failed("Couldn't install the update: $message")
    }

    /** The user backed out of Android's install confirmation: offer the update again. */
    internal fun onInstallAborted(update: AvailableUpdate) {
        _state.value = UpdateState.Available(update)
    }

    // ── Internals ────────────────────────────────────────────────

    private suspend fun autoCheck() {
        val update = fetchLatest() ?: return
        val autoUpdate = settingsRepository.autoUpdate.first()
        if (!autoUpdate) {
            if (update.build != settingsRepository.skippedUpdateBuild.first()) {
                _state.value = UpdateState.Available(update)
            }
            return
        }
        val apk = download(update, userInitiated = false) ?: return
        val silent = canInstallSilently()
        _state.value = UpdateState.Ready(update, apk, silent)
        // A silent update waits for the app to be backgrounded; one needing a tap is offered now.
        if (silent && !inForeground) onAppBackground()
    }

    /** The latest release if it's newer than this install, else null. Never throws. */
    private suspend fun fetchLatest(): AvailableUpdate? = withContext(Dispatchers.IO) {
        lastCheckAt = System.currentTimeMillis()
        try {
            val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val release = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val match = TAG_RE.matchEntire(release.optString("tag_name")) ?: return@withContext null
                val build = match.groupValues[2].toIntOrNull() ?: return@withContext null
                if (build <= BuildConfig.VERSION_CODE) return@withContext null

                val assets = release.optJSONArray("assets") ?: return@withContext null
                val apk = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") }
                    ?: return@withContext null

                AvailableUpdate(
                    versionName = match.groupValues[1],
                    build = build,
                    apkUrl = apk.getString("browser_download_url"),
                    sizeBytes = apk.optLong("size"),
                    notes = release.optString("body").trim()
                )
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed", e)
            null
        }
    }

    /** Downloads the APK with retries; large downloads get cut off when the network hands over. */
    private suspend fun download(update: AvailableUpdate, userInitiated: Boolean): File? {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val dest = File(dir, "bitvibe-${update.build}.apk")
        // Drop older downloads.
        dir.listFiles()?.filter { it != dest }?.forEach { it.delete() }
        if (dest.exists() && update.sizeBytes > 0 && dest.length() == update.sizeBytes) return dest

        var lastError: Exception? = null
        for (attempt in 1..DOWNLOAD_ATTEMPTS) {
            _state.value = UpdateState.Downloading(update, 0f, userInitiated)
            try {
                withContext(Dispatchers.IO) {
                    val conn = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 30_000
                    }
                    try {
                        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                            throw IllegalStateException("HTTP ${conn.responseCode}")
                        }
                        val total = conn.contentLengthLong.takeIf { it > 0 } ?: update.sizeBytes
                        conn.inputStream.use { input ->
                            dest.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var written = 0L
                                var lastReported = 0f
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n < 0) break
                                    output.write(buffer, 0, n)
                                    written += n
                                    if (total > 0) {
                                        val progress = (written.toFloat() / total).coerceAtMost(1f)
                                        if (progress - lastReported >= 0.01f) {
                                            lastReported = progress
                                            _state.value = UpdateState.Downloading(update, progress, userInitiated)
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
                return dest
            } catch (e: Exception) {
                lastError = e
                dest.delete()
                if (attempt < DOWNLOAD_ATTEMPTS) delay(2_000L * attempt)
            }
        }
        Log.w(TAG, "Update download failed", lastError)
        _state.value = if (userInitiated) {
            UpdateState.Failed("Download kept failing (${lastError?.message}). Check your connection and try again.")
        } else {
            UpdateState.Idle // quiet background attempt; we'll retry on a later launch
        }
        return null
    }

    private fun install(update: AvailableUpdate, apk: File) {
        if (!canRequestInstalls()) {
            _state.value = UpdateState.NeedsPermission(update, apk)
            return
        }
        _state.value = UpdateState.Installing(update)
        scope.launch {
            try {
                withContext(Dispatchers.IO) { commitSession(update, apk) }
            } catch (e: Exception) {
                onInstallFailed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private fun commitSession(update: AvailableUpdate, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java)
                .setAction(UpdateInstallReceiver.ACTION_INSTALL_STATUS)
                .putExtra(UpdateInstallReceiver.EXTRA_VERSION_NAME, update.versionName)
                .putExtra(UpdateInstallReceiver.EXTRA_BUILD, update.build)
                .putExtra(UpdateInstallReceiver.EXTRA_APK_URL, update.apkUrl)
                .putExtra(UpdateInstallReceiver.EXTRA_SIZE, update.sizeBytes)
            // Mutable: the installer adds the status extras to this intent.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pendingIntent.intentSender)
        }
    }

    /** Android 8+ needs the per-app "install unknown apps" permission. */
    private fun canRequestInstalls(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** True when Android will apply the update without asking (Android 12+, we installed ourselves). */
    private fun canInstallSilently(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !canRequestInstalls()) return false
        return try {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    fun unknownSourcesSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(android.net.Uri.parse("package:${context.packageName}"))

    private companion object {
        const val TAG = "UpdateManager"
        const val REPO = "tanviralamtusar/BitVibe"
        val TAG_RE = Regex("""^android-v(.+)-b(\d+)$""")
        const val CHECK_INTERVAL_MS = 30 * 60 * 1000L
        const val DOWNLOAD_ATTEMPTS = 3
    }
}

data class AvailableUpdate(
    val versionName: String,
    val build: Int,
    val apkUrl: String,
    val sizeBytes: Long,
    val notes: String
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AvailableUpdate) : UpdateState
    data class Downloading(val update: AvailableUpdate, val progress: Float, val userInitiated: Boolean) : UpdateState
    /** Downloaded. [silent] updates install by themselves when the app is backgrounded. */
    data class Ready(val update: AvailableUpdate, val apk: File, val silent: Boolean) : UpdateState
    data class NeedsPermission(val update: AvailableUpdate, val apk: File) : UpdateState
    data class Installing(val update: AvailableUpdate) : UpdateState
    data class Failed(val message: String) : UpdateState
}
