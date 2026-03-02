package com.khadr.forge.features.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// ─── Priority ──────────────────────────────────────────────────────────────────
// ordinal: HIGH=0, MEDIUM=1, LOW=2 — used for minByOrNull sort in DashboardViewModel
enum class TaskPriorityEntity { HIGH, MEDIUM, LOW }

// ─── Task Entity ───────────────────────────────────────────────────────────────
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id          : Long                 = 0,
    val title       : String,
    val description : String               = "",
    val category    : String               = "",
    val priority    : TaskPriorityEntity   = TaskPriorityEntity.MEDIUM,
    val isDone      : Boolean              = false,
    val dueDate     : LocalDate?           = null,
    val createdAt   : Long                 = System.currentTimeMillis()
)