package com.khadr.forge.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired when the user taps Dismiss or Snooze on the alarm notification. */
class DismissAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        // إيقاف الخدمة اللي بتشغل الرنين / الاهتزاز / الشاشة الكاملة
        context.stopService(Intent(context, AlarmService::class.java))

        if (action == ACTION_SNOOZE) {
            // ── قراءة الإعدادات الأصلية من الـ Intent ─────────────────────────────
            val title = intent.getStringExtra(AlarmReceiver.EXTRA_TITLE) ?: ""
            val note = intent.getStringExtra(AlarmReceiver.EXTRA_NOTE) ?: ""

            val silent = intent.getBooleanExtra(AlarmScheduler.EXTRA_SILENT, false)
            val vibrationOnly = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATION_ONLY, false)
            val fullScreen = intent.getBooleanExtra(AlarmScheduler.EXTRA_FULL_SCREEN, true)
            val ringtoneUri = intent.getStringExtra(AlarmScheduler.EXTRA_RINGTONE_URI) ?: ""

            // ── حساب وقت السكون (5 دقائق مثلاً) ────────────────────────────────
            val snoozeMillis = System.currentTimeMillis() + SNOOZE_DURATION_MS

            // ── إعادة جدولة المنبه بنفس الإعدادات ───────────────────────────────
            AlarmScheduler.schedule(
                context = context,
                reminderId = reminderId,
                triggerAtMillis = snoozeMillis,
                title = title,
                note = note,
                silent = silent,
                ringtoneUri = ringtoneUri,
                vibrationOnly = vibrationOnly,
                fullScreen = fullScreen
            )
        }

        // اختياري: يمكنك هنا إضافة إشعار صغير "Snoozed for 5 minutes" لو حابب
    }

    companion object {
        const val ACTION_DISMISS = "com.khadr.forge.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.khadr.forge.ACTION_SNOOZE_ALARM"
        const val EXTRA_REMINDER_ID = "reminder_id"

        // يفضل تحدده في مكان واحد عشان لو غيرت المدة بعدين يتغير من مكان واحد
        private const val SNOOZE_DURATION_MS = 5 * 60 * 1000L  // 5 دقائق
    }
}