package com.khadr.forge.core.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.khadr.forge.MainActivity
import com.khadr.forge.R

class AlarmService : Service() {

    private var ringtone : Ringtone? = null
    private var vibrator : Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId    = intent?.getLongExtra(EXTRA_REMINDER_ID, -1L) ?: -1L
        val title         = intent?.getStringExtra(EXTRA_TITLE)           ?: getString(R.string.reminders)
        val note          = intent?.getStringExtra(EXTRA_NOTE)            ?: ""
        val silent        = intent?.getBooleanExtra(AlarmScheduler.EXTRA_SILENT,         false) ?: false
        val vibrationOnly = intent?.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATION_ONLY, false) ?: false
        val fullScreen    = intent?.getBooleanExtra(AlarmScheduler.EXTRA_FULL_SCREEN,    true)  ?: true
        val ringtoneUri   = intent?.getStringExtra(AlarmScheduler.EXTRA_RINGTONE_URI)   ?: ""

        startForeground(NOTIF_ID, buildNotification(reminderId, title, note, fullScreen))

        if (!silent) {
            if (!vibrationOnly) playRingtone(ringtoneUri)
            vibrate()
        }

        return START_NOT_STICKY
    }

    // ── Ringtone ──────────────────────────────────────────────────────────────
    private fun playRingtone(uriStr: String) {
        try {
            val uri = when {
                uriStr.isNotBlank() -> Uri.parse(uriStr)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.also { r ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) r.isLooping = true
                r.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_ALARM)
                    .build()
                r.play()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── Vibration ─────────────────────────────────────────────────────────────
    private fun vibrate() {
        try {
            val pattern = longArrayOf(0L, 500L, 300L, 500L, 300L, 500L)
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private fun buildNotification(reminderId: Long, title: String, note: String, fullScreen: Boolean): Notification {
        ensureChannel()

        val openIntent = PendingIntent.getActivity(
            this, reminderId.toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = PendingIntent.getBroadcast(
            this, (reminderId * 2).toInt(),
            Intent(this, DismissAlarmReceiver::class.java).apply {
                action = DismissAlarmReceiver.ACTION_DISMISS
                putExtra(DismissAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = PendingIntent.getBroadcast(
            this, (reminderId * 2 + 1).toInt(),
            Intent(this, DismissAlarmReceiver::class.java).apply {
                action = DismissAlarmReceiver.ACTION_SNOOZE
                putExtra(DismissAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(AlarmReceiver.EXTRA_TITLE, title)
                putExtra(AlarmReceiver.EXTRA_NOTE, note)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(note.ifBlank { getString(R.string.reminders) })
            .setContentIntent(openIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(0, "Dismiss",    dismissIntent)
            .addAction(0, "Snooze 5m", snoozeIntent)

        if (fullScreen) builder.setFullScreenIntent(openIntent, true)

        return builder.build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID, "Forge Alarms", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                enableVibration(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() { ringtone?.stop(); vibrator?.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE       = "title"
        const val EXTRA_NOTE        = "note"
        const val NOTIF_ID          = 9001
        const val ALARM_CHANNEL_ID  = "forge_alarms"
    }
}