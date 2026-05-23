package com.katgr0up.katbudget

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.katgr0up.katbudget.managers.AppUpdater

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (!AppUpdater.isUpdateDownloadComplete(context, downloadId)) return

        if (AppUpdater.openDownloadedApk(context)) {
            AppUpdater.clearSavedDownload(context)
        }
    }
}
