package com.riverking.mobile.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.riverking.mobile.auth.AppUpdateInfoDto

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

class AppUpdateInstaller(
    private val activity: ComponentActivity,
) {
    private val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var activeDownloadId: Long? = null
    private var pendingDownloadedUri: Uri? = null
    private var receiverRegistered = false
    private var onError: ((String) -> Unit)? = null
    private var latestStrings: RiverStrings? = null

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId <= 0L || downloadId != activeDownloadId) return
            handleDownloadComplete(downloadId)
        }
    }

    fun startUpdate(update: AppUpdateInfoDto, strings: RiverStrings, onError: (String) -> Unit) {
        this.onError = onError
        this.latestStrings = strings
        if (update.usesApkDownload) {
            startApkDownload(update, strings)
        } else {
            runCatching { openExternalUrl(activity, update.installUrl) }
                .onFailure { onError(it.message ?: strings.unavailable) }
        }
    }

    fun resumePendingInstall(strings: RiverStrings, onError: (String) -> Unit) {
        this.onError = onError
        this.latestStrings = strings
        val uri = pendingDownloadedUri ?: return
        if (canRequestPackageInstalls()) {
            pendingDownloadedUri = null
            launchInstaller(uri, strings)
        }
    }

    fun dispose() {
        if (!receiverRegistered) return
        runCatching { activity.unregisterReceiver(downloadReceiver) }
        receiverRegistered = false
    }

    private fun startApkDownload(update: AppUpdateInfoDto, strings: RiverStrings) {
        val url = update.installUrl.trim()
        if (url.isBlank()) {
            update.fallbackUrl?.let {
                runCatching { openExternalUrl(activity, it) }
                    .onFailure { error -> onError?.invoke(error.message ?: strings.unavailable) }
                return
            }
            onError?.invoke(strings.unavailable)
            return
        }
        ensureReceiverRegistered()
        val fileName = update.downloadFileName
            ?.takeIf { it.isNotBlank() }
            ?: "riverking-${update.latestVersionName.ifBlank { "latest" }}.apk"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(strings.appTitle)
            .setDescription(strings.updateDownloadDescription)
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, fileName)
        runCatching {
            activeDownloadId = downloadManager.enqueue(request)
        }.onFailure {
            update.fallbackUrl?.let { fallback ->
                runCatching { openExternalUrl(activity, fallback) }
                    .onFailure { error -> onError?.invoke(error.message ?: strings.unavailable) }
                return
            }
            onError?.invoke(it.message ?: strings.unavailable)
        }
    }

    private fun handleDownloadComplete(downloadId: Long) {
        activeDownloadId = null
        val cursor = runCatching {
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        }.getOrNull()
        val strings = latestStrings
        cursor?.use {
            if (!it.moveToFirst()) {
                onError?.invoke(strings?.updateDownloadedUnavailable ?: "Downloaded update is unavailable")
                return
            }
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                onError?.invoke(strings?.updateDownloadFailed ?: "Update download failed")
                return
            }
        } ?: run {
            onError?.invoke(strings?.updateDownloadFailed ?: "Update download failed")
            return
        }
        val uri = downloadManager.getUriForDownloadedFile(downloadId)
        if (uri == null) {
            onError?.invoke(strings?.updateDownloadedUnavailable ?: "Downloaded update is unavailable")
            return
        }
        promptInstall(uri)
    }

    private fun promptInstall(uri: Uri) {
        if (!canRequestPackageInstalls()) {
            pendingDownloadedUri = uri
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            )
            runCatching { activity.startActivity(settingsIntent) }
                .onFailure {
                    val message = latestStrings?.updateInstallerPermissionUnavailable
                        ?: "Installer permission is unavailable"
                    onError?.invoke(it.message ?: message)
                }
            return
        }
        launchInstaller(uri, null)
    }

    private fun launchInstaller(uri: Uri, strings: RiverStrings?) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { activity.startActivity(installIntent) }
            .onFailure {
                onError?.invoke(
                    it.message
                        ?: strings?.updateInstallerUnavailable
                        ?: latestStrings?.updateInstallerUnavailable
                        ?: "Installer unavailable",
                )
            }
    }

    private fun canRequestPackageInstalls(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.packageManager.canRequestPackageInstalls()

    private fun ensureReceiverRegistered() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            activity,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }
}
