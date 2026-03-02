package com.khadr.forge.core.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.khadr.forge.features.budget.data.BudgetCategory
import com.khadr.forge.features.budget.data.TransactionDao
import com.khadr.forge.features.budget.data.TransactionEntity
import com.khadr.forge.features.budget.data.TransactionType
import com.khadr.forge.features.reminders.data.ReminderDao
import com.khadr.forge.features.reminders.data.ReminderEntity
import com.khadr.forge.features.reminders.data.RepeatInterval
import com.khadr.forge.features.schedule.data.EventDao
import com.khadr.forge.features.schedule.data.EventEntity
import com.khadr.forge.features.tasks.data.TaskDao
import com.khadr.forge.features.tasks.data.TaskEntity
import com.khadr.forge.features.tasks.data.TaskPriorityEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ForgeTypeConverters {
    @TypeConverter fun fromLocalDateTime(v: LocalDateTime?): String? = v?.toString()
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter fun toLocalDateTime(v: String?): LocalDateTime? = v?.let { LocalDateTime.parse(it) }
    @TypeConverter fun fromLocalDate(v: LocalDate?): String? = v?.toString()
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter fun toLocalDate(v: String?): LocalDate? = v?.let { LocalDate.parse(it) }
    @TypeConverter fun fromLocalTime(v: LocalTime?): String? = v?.toString()
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter fun toLocalTime(v: String?): LocalTime? = v?.let { LocalTime.parse(it) }
    @TypeConverter fun fromTaskPriority(v: TaskPriorityEntity): String = v.name
    @TypeConverter fun toTaskPriority(v: String): TaskPriorityEntity = TaskPriorityEntity.valueOf(v)
    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)
    @TypeConverter fun fromBudgetCategory(v: BudgetCategory): String = v.name
    @TypeConverter fun toBudgetCategory(v: String): BudgetCategory = BudgetCategory.valueOf(v)
    @TypeConverter fun fromRepeatInterval(v: RepeatInterval): String = v.name
    @TypeConverter fun toRepeatInterval(v: String): RepeatInterval = RepeatInterval.valueOf(v)
    @TypeConverter fun fromStringList(v: List<String>?): String? = v?.joinToString("|")
    @TypeConverter fun toStringList(v: String?): List<String> = v?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
}

@Database(
    entities  = [TaskEntity::class, EventEntity::class, TransactionEntity::class, ReminderEntity::class],
    version   = 5,   // 1→tasks | 2→+events | 3→+transactions | 4→+reminders | 5→tasks schema fix
    exportSchema = true
)
@TypeConverters(ForgeTypeConverters::class)
abstract class ForgeDatabase : RoomDatabase() {
    abstract fun taskDao()        : TaskDao
    abstract fun eventDao()       : EventDao
    abstract fun transactionDao() : TransactionDao
    abstract fun reminderDao()    : ReminderDao
    companion object { const val DATABASE_NAME = "forge_db" }
}