package com.katgr0up.katbudget.managers

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.katgr0up.katbudget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.net.URL

object AppUpdater {
    const val DEFAULT_UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/KATGR0UP/KAT-Budget/main/data/update.json"

    private const val APK_FILE_NAME = "kat_budget_update.apk"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val PROVIDER_SUFFIX = ".provider"
    private const val PREFS_NAME = "kat_budget_update"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val UPDATE_JSON_TIMEOUT_MILLIS = 10_000L

    data class UpdateInfo(
        val versionCode: Long,
        val versionName: String,
        val releaseNotes: String,
        val apkUrl: String
    )

    sealed class UpdateCheckResult {
        data class Available(val update: UpdateInfo) : UpdateCheckResult()
        object UpToDate : UpdateCheckResult()
        data class Failed(val error: Throwable) : UpdateCheckResult()
    }

    suspend fun checkForUpdate(
        context: Context,
        updateJsonUrl: String = DEFAULT_UPDATE_JSON_URL,
        fallbackVersionName: String = "",
        fallbackReleaseNotes: String = ""
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val json = withTimeout(UPDATE_JSON_TIMEOUT_MILLIS) {
                JSONObject(readUpdateJson(updateJsonUrl))
            }

            val onlineVersionCode = json.optLong("versionCode", -1L)
            if (onlineVersionCode <= currentVersionCode(context)) {
                return@runCatching UpdateCheckResult.UpToDate
            }

            val apkUrl = json.optString("apkUrl").trim()
            require(isHttpsUrl(apkUrl)) { "Update APK URL must use HTTPS." }

            UpdateCheckResult.Available(
                UpdateInfo(
                    versionCode = onlineVersionCode,
                    versionName = json.optString("versionName", fallbackVersionName),
                    releaseNotes = json.optString("releaseNotes", fallbackReleaseNotes),
                    apkUrl = apkUrl
                )
            )
        }.getOrElse { error ->
            UpdateCheckResult.Failed(error)
        }
    }

    fun downloadApk(context: Context, url: String) {
        enqueueApkDownload(context, url)
    }

    fun installApk(context: Context) {
        openDownloadedApk(context)
    }

    fun enqueueApkDownload(context: Context, url: String): Long? {
        val downloadUri = url
            .trim()
            .takeIf { it.isNotBlank() }
            ?.toUri()
            ?.takeIf { it.scheme.equals("https", ignoreCase = true) }
            ?: return null

        val request = DownloadManager.Request(downloadUri)
            .setTitle(context.getString(R.string.update_download_title))
            .setDescription(context.getString(R.string.update_download_description))
            .setMimeType(APK_MIME_TYPE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                APK_FILE_NAME
            )

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return null

        return runCatching { downloadManager.enqueue(request) }
            .getOrNull()
            ?.also { saveDownloadId(context, it) }
    }

    fun isUpdateDownloadComplete(context: Context, downloadId: Long): Boolean {
        if (downloadId <= 0L || downloadId != savedDownloadId(context)) return false

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return false
        val query = DownloadManager.Query().setFilterById(downloadId)

        val cursor = downloadManager.query(query) ?: return false

        return runCatching {
            cursor.use {
                if (!it.moveToFirst()) {
                    false
                } else {
                    val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    statusIndex >= 0 && it.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL
                }
            }
        }.getOrDefault(false)
    }

    fun clearSavedDownload(context: Context) {
        updatePrefs(context).edit {
            remove(KEY_DOWNLOAD_ID)
        }
    }

    fun openDownloadedApk(context: Context): Boolean {
        val apkFile = getUpdateFile(context)
        if (!apkFile.exists() || apkFile.length() <= 0L) return false

        if (!context.packageManager.canRequestPackageInstalls()) {
            openInstallPermissionSettings(context)
            return false
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}$PROVIDER_SUFFIX",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return runCatching {
            context.startActivity(installIntent)
            true
        }.getOrDefault(false)
    }

    private fun openInstallPermissionSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun readUpdateJson(url: String): String {
        val cleanUrl = url.trim()
        require(isHttpsUrl(cleanUrl)) { "Update JSON URL must use HTTPS." }

        val connection = URL(cleanUrl).openConnection().apply {
            connectTimeout = UPDATE_JSON_TIMEOUT_MILLIS.toInt()
            readTimeout = UPDATE_JSON_TIMEOUT_MILLIS.toInt()
        }

        return connection.getInputStream()
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    private fun isHttpsUrl(url: String): Boolean {
        return url.toUri().scheme.equals("https", ignoreCase = true)
    }

    private fun currentVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.longVersionCode
    }

    private fun getUpdateFile(context: Context): File {
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            APK_FILE_NAME
        )
    }

    private fun saveDownloadId(context: Context, downloadId: Long) {
        updatePrefs(context).edit {
            putLong(KEY_DOWNLOAD_ID, downloadId)
        }
    }

    private fun savedDownloadId(context: Context): Long {
        return updatePrefs(context).getLong(KEY_DOWNLOAD_ID, -1L)
    }

    private fun updatePrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
