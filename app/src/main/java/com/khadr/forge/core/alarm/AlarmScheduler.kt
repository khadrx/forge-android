package com.khadr.forge.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    // ── Extra keys passed through to AlarmService ─────────────────────────────
    const val EXTRA_SILENT        = "forge_silent"
    const val EXTRA_VIBRATION_ONLY= "forge_vibration_only"
    const val EXTRA_FULL_SCREEN   = "forge_full_screen"
    const val EXTRA_RINGTONE_URI  = "forge_ringtone_uri"

    fun schedule(
        context        : Context,
        reminderId     : Long,
        triggerAtMillis: Long,
        title          : String,
        note           : String,
        silent         : Boolean = false,
        ringtoneUri    : String  = "",
        vibrationOnly  : Boolean = false,
        fullScreen     : Boolean = true
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val intent = buildIntent(context, reminderId, title, note, silent, ringtoneUri, vibrationOnly, fullScreen)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            intent
        )
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = buildIntent(context, reminderId, "", "", false, "", false, false)
        alarmManager.cancel(intent)
    }

    private fun buildIntent(
        context       : Context,
        reminderId    : Long,
        title         : String,
        note          : String,
        silent        : Boolean,
        ringtoneUri   : String,
        vibrationOnly : Boolean,
        fullScreen    : Boolean
    ): PendingIntent {
        val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmReceiver.EXTRA_TITLE,       title)
            putExtra(AlarmReceiver.EXTRA_NOTE,        note)
            putExtra(EXTRA_SILENT,         silent)
            putExtra(EXTRA_VIBRATION_ONLY, vibrationOnly)
            putExtra(EXTRA_FULL_SCREEN,    fullScreen)
            putExtra(EXTRA_RINGTONE_URI,   ringtoneUri)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}