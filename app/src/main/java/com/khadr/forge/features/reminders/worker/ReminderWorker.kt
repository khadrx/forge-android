package com.khadr.forge.features.reminders.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.khadr.forge.core.notifications.NotificationHelper
import com.khadr.forge.features.reminders.data.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams      : WorkerParameters,
    private val repository      : ReminderRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
        if (reminderId == -1L) return Result.failure()

        val reminder = repository.getById(reminderId) ?: return Result.failure()

        // Don't fire if user already marked it done
        if (reminder.isDone) return Result.success()

        NotificationHelper.post(
            context = context,
            id      = reminderId.toInt(),
            title   = reminder.title,
            body    = reminder.note.ifBlank { reminder.date.toString() }
        )

        // Auto-mark as done for non-repeating reminders
        if (reminder.repeat == com.khadr.forge.features.reminders.data.RepeatInterval.NONE) {
            repository.setDone(reminderId, true)
        }

        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val TAG_PREFIX      = "reminder_"

        /** Schedule or re-schedule a reminder via WorkManager */
        fun schedule(context: Context, reminderId: Long, triggerAtMillis: Long) {
            val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            val data  = workDataOf(KEY_REMINDER_ID to reminderId)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("$TAG_PREFIX$reminderId")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "$TAG_PREFIX$reminderId",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        /** Cancel a scheduled reminder */
        fun cancel(context: Context, reminderId: Long) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("$TAG_PREFIX$reminderId")
        }
    }
}