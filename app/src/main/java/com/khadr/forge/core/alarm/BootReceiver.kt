package com.khadr.forge.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.khadr.forge.features.reminders.data.ReminderRepository
import com.khadr.forge.features.reminders.data.RepeatInterval
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Re-schedules all active reminders after the device reboots,
 * since AlarmManager alarms are cleared on reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderRepository

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val upcoming = repository.getUpcomingReminders().first()
            val today    = LocalDate.now()

            upcoming.forEach { reminder ->
                // Only re-schedule future reminders
                if (reminder.date >= today && !reminder.isDone) {
                    AlarmScheduler.schedule(
                        context        = context,
                        reminderId     = reminder.id,
                        triggerAtMillis = reminder.triggerAtMillis(),
                        title          = reminder.title,
                        note           = reminder.note
                    )
                }
            }
        }
    }
}