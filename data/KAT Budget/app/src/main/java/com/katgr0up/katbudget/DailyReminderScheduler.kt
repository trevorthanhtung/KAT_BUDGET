package com.katgr0up.katbudget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object DailyReminderScheduler {
    private const val TAG = "DailyReminderScheduler"
    private const val DAILY_REMINDER_HOUR = 20
    private const val DAILY_REMINDER_MINUTE = 0
    private const val DAILY_REMINDER_REQUEST_CODE = 1001

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextTriggerAtMillis(),
                AlarmManager.INTERVAL_DAY,
                createPendingIntent(appContext)
            )
        }.onFailure { error ->
            Log.e(TAG, "Unable to schedule daily reminder.", error)
        }
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DAILY_REMINDER
        }

        return PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerAtMillis(): Long {
        return Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, DAILY_REMINDER_HOUR)
            set(Calendar.MINUTE, DAILY_REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis
    }
}
