package com.khadr.forge.features.reminders.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao
) {
    fun getUpcomingReminders(): Flow<List<ReminderEntity>> = dao.getUpcomingReminders()
    fun getPastReminders(): Flow<List<ReminderEntity>>     = dao.getPastReminders()
    suspend fun getById(id: Long): ReminderEntity?         = dao.getById(id)

    suspend fun addReminder(r: ReminderEntity): Long       = dao.insertReminder(r)
    suspend fun updateReminder(r: ReminderEntity)          = dao.updateReminder(r)
    suspend fun setDone(id: Long, done: Boolean)           = dao.setDone(id, done)
    suspend fun deleteReminder(id: Long)                   = dao.deleteReminder(id)
}