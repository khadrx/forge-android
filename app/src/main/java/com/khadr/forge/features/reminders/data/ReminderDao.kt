package com.khadr.forge.features.reminders.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY date ASC, time ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isDone = 0 ORDER BY date ASC, time ASC")
    fun getUpcomingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isDone = 1 ORDER BY date DESC, time DESC")
    fun getPastReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(r: ReminderEntity): Long

    @Update
    suspend fun updateReminder(r: ReminderEntity)

    @Query("UPDATE reminders SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)
}