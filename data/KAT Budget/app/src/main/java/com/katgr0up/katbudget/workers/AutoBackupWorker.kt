package com.katgr0up.katbudget.workers

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.katgr0up.katbudget.data.local.AppDatabase
import com.katgr0up.katbudget.data.repository.TransactionRepository
import com.katgr0up.katbudget.managers.BackupManager
import com.katgr0up.katbudget.managers.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AutoBackupWorker"
        const val AUTO_BACKUP_FILE_NAME = "AutoBackup_KatBudget.kat"
        private const val BACKUP_MIME_TYPE = "application/octet-stream"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val backupDir = resolveBackupDirectory() ?: return@withContext Result.failure()
            val backupBytes = createBackupBytes()

            if (backupBytes.isEmpty()) {
                Log.e(TAG, "Backup data is empty.")
                return@withContext Result.failure()
            }

            val backupFile = resolveBackupFile(backupDir) ?: return@withContext Result.failure()

            if (!writeBackupFile(backupFile, backupBytes)) {
                Log.e(TAG, "Unable to open backup output stream.")
                return@withContext Result.retry()
            }

            Log.d(TAG, "Auto backup completed.")
            Result.success()
        }.getOrElse { error ->
            Log.e(TAG, "Auto backup failed.", error)
            Result.retry()
        }
    }

    private fun resolveBackupDirectory(): DocumentFile? {
        val backupUriString = PreferencesManager(applicationContext).getAutoBackupPath()

        if (backupUriString.isNullOrBlank()) {
            Log.w(TAG, "Auto backup folder is not configured.")
            return null
        }

        val backupDir = DocumentFile.fromTreeUri(applicationContext, backupUriString.toUri())
        if (backupDir == null || !backupDir.exists() || !backupDir.isDirectory || !backupDir.canWrite()) {
            Log.e(TAG, "Backup folder is unavailable or not writable.")
            return null
        }

        return backupDir
    }

    private suspend fun createBackupBytes(): ByteArray {
        val repository = TransactionRepository(
            AppDatabase.getDatabase(applicationContext).transactionDao()
        )
        val allCategories = repository.expenseCategories.first() +
            repository.incomeCategories.first()

        return BackupManager.exportBackupDataBytes(
            repository.allSources.first(),
            repository.allTransactions.first(),
            repository.allDebts.first(),
            repository.allBudgets.first(),
            repository.allRecurrings.first(),
            repository.allGoals.first(),
            allCategories
        )
    }

    private fun resolveBackupFile(backupDir: DocumentFile): DocumentFile? {
        val existingFile = backupDir.findFile(AUTO_BACKUP_FILE_NAME)

        if (existingFile != null) {
            if (existingFile.isFile && existingFile.canWrite()) return existingFile

            Log.e(TAG, "Existing backup target is not a writable file.")
            return null
        }

        val createdFile = backupDir.createFile(BACKUP_MIME_TYPE, AUTO_BACKUP_FILE_NAME)
        if (createdFile == null || !createdFile.exists() || !createdFile.canWrite()) {
            Log.e(TAG, "Unable to create backup file.")
            return null
        }

        return createdFile
    }

    private fun writeBackupFile(backupFile: DocumentFile, backupBytes: ByteArray): Boolean {
        return applicationContext.contentResolver
            .openOutputStream(backupFile.uri, "wt")
            ?.use { output ->
                output.write(backupBytes)
                true
            } ?: false
    }
}
