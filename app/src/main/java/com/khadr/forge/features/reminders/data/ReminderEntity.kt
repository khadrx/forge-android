package com.khadr.forge.features.reminders.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

// ─── Repeat Interval ──────────────────────────────────────────────────────────
enum class RepeatInterval { NONE, DAILY, WEEKLY, MONTHLY }

// ─── Reminder Entity ──────────────────────────────────────────────────────────
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id         : Long            = 0,
    val title      : String,
    val note       : String          = "",
    val date       : LocalDate,
    val time       : LocalTime,
    val repeat     : RepeatInterval  = RepeatInterval.NONE,
    val isDone     : Boolean         = false,
    val createdAt  : Long            = System.currentTimeMillis()
) {
    /** Epoch millis for WorkManager scheduling */
    fun triggerAtMillis(): Long {
        val dateTime = date.atTime(time)
        return dateTime
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}