package com.khadr.forge.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title      = intent.getStringExtra(EXTRA_TITLE) ?: return
        val note       = intent.getStringExtra(EXTRA_NOTE)  ?: ""

        if (reminderId == -1L) return

        // Forward ALL extras (including ringtoneUri, silent, vibrationOnly, fullScreen)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmService.EXTRA_REMINDER_ID,              reminderId)
            putExtra(AlarmService.EXTRA_TITLE,                    title)
            putExtra(AlarmService.EXTRA_NOTE,                     note)
            // ↓ these come from AlarmScheduler.buildIntent()
            putExtra(AlarmScheduler.EXTRA_SILENT,         intent.getBooleanExtra(AlarmScheduler.EXTRA_SILENT,         false))
            putExtra(AlarmScheduler.EXTRA_VIBRATION_ONLY, intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATION_ONLY, false))
            putExtra(AlarmScheduler.EXTRA_FULL_SCREEN,    intent.getBooleanExtra(AlarmScheduler.EXTRA_FULL_SCREEN,    true))
            putExtra(AlarmScheduler.EXTRA_RINGTONE_URI,   intent.getStringExtra(AlarmScheduler.EXTRA_RINGTONE_URI)   ?: "")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE       = "title"
        const val EXTRA_NOTE        = "note"
    }
}