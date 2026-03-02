package com.khadr.forge.features.schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

// ─── Event Entity ─────────────────────────────────────────────────────────────
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id        : Long        = 0,
    val title     : String,
    val note      : String      = "",
    val date      : LocalDate,
    val startTime : LocalTime,
    val endTime   : LocalTime?  = null,
    val createdAt : Long        = System.currentTimeMillis()
)