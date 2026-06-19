package com.spmods.spgram.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.spmods.spgram.data.core.coRunCatching
import com.spmods.spgram.data.datasource.remote.UpdateRemoteDateSource
import com.spmods.spgram.data.service.UpdateInstallReceiver
import com.spmods.spgram.domain.models.UpdateInfo
import com.spmods.spgram.domain.models.UpdateState
import com.spmods.spgram.domain.repository.AuthRepository
import com.spmods.spgram.domain.repository.AuthStep
import com.spmods.spgram.domain.repository.StringProvider
import com.spmods.spgram.domain.repository.UpdateRepository
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateRepositoryImpl(
    private val context: Context,
    private val remote: UpdateRemoteDateSource,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope,
    private val stringProvider: StringProvider
) : UpdateRepository {

    private val tag = "UpdateRepository"

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var currentUpdateInfo: UpdateInfo? = null
    private var downloadJob: Job? = null

    override suspend fun checkForUpdates() {
        // auth Ready වෙනකම් wait කරනවා — immediately return කරන්නේ නැහැ
        authRepository.authState.first { it is AuthStep.Ready }

        _updateState.value = UpdateState.Checking

        coRunCatching {
            val info = remote.fetchLatestUpdate()
                ?: return@coRunCatching run {
                    _updateState.value =
                        UpdateState.Error(stringProvider.getString("update_no_update_found"))
                }

            val currentVersionCode = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            } catch (e: Exception) {
                0
            }

            Log.d(tag, "Current: $currentVersionCode  Latest: ${info.versionCode}")

            if (info.versionCode <= currentVersionCode) {
                _updateState.value = UpdateState.UpToDate
                return@coRunCatching
            }

            currentUpdateInfo = info
            _updateState.value = UpdateState.UpdateAvailable(info)

        }.onFailure {
            _updateState.value =
                UpdateState.Error(it.message ?: "Failed to check for updates")
        }
    }

    override fun downloadUpdate() {
        val info = currentUpdateInfo ?: return
        if (downloadJob?.isActive == true) return

        _updateState.value = UpdateState.Downloading(0f, info.fileSize)

        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                val apkFile = File(context.cacheDir, info.fileName)

                val url = URL(info.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 60_000
                }

                val totalBytes = if (info.fileSize > 0) {
                    info.fileSize
                } else {
                    connection.contentLengthLong.takeIf { it > 0 } ?: -1L
                }

                connection.inputStream.use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            // Check if cancelled
                            if (downloadJob?.isCancelled == true) {
                                apkFile.delete()
                                return@launch
                            }
                            output.write(buffer, 0, bytes)
                            downloaded += bytes
                            if (totalBytes > 0) {
                                val progress = downloaded.toFloat() / totalBytes.toFloat()
                                _updateState.value =
                                    UpdateState.Downloading(progress.coerceIn(0f, 1f), totalBytes)
                            }
                        }
                    }
                }

                _updateState.value = UpdateState.ReadyToInstall(apkFile.absolutePath)

            } catch (e: Exception) {
                Log.e(tag, "Download failed", e)
                _updateState.value =
                    UpdateState.Error(e.message ?: "Download failed")
            }
        }
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val info = currentUpdateInfo
        if (info != null) {
            _updateState.value = UpdateState.UpdateAvailable(info)
        } else {
            _updateState.value = UpdateState.Idle
        }
    }

    override fun installUpdate() {
        val state = _updateState.value as? UpdateState.ReadyToInstall ?: return
        val file = File(state.filePath)
        if (!file.exists()) {
            _updateState.value = UpdateState.Error("APK file not found, please download again")
            currentUpdateInfo?.let { _updateState.value = UpdateState.UpdateAvailable(it) }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL
                ).apply {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }

                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)

                FileInputStream(file).use { input ->
                    session.openWrite("package", 0, file.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }

                val intent = Intent(context, UpdateInstallReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                session.commit(pendingIntent.intentSender)
                session.close()
                return
            } catch (e: Exception) {
                Log.e(tag, "PackageInstaller flow failed, using fallback", e)
            }
        }

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
    }
    override suspend fun getTdLibVersion(): String = remote.getTdLibVersion()

    override suspend fun getTdLibCommitHash(): String = remote.getTdLibCommitHash()
}
