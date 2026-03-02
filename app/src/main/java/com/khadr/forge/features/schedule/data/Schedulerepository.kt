package com.khadr.forge.features.schedule.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val dao: EventDao
) {
    fun getAllEvents(): Flow<List<EventEntity>> = dao.getAllEvents()

    fun getEventsForDate(date: LocalDate): Flow<List<EventEntity>> =
        dao.getEventsForDate(date.toString())

    fun getDatesWithEvents(): Flow<List<LocalDate>> =
        dao.getDatesWithEvents().map { strings ->
            strings.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        }

    suspend fun addEvent(event: EventEntity): Long    = dao.insertEvent(event)
    suspend fun updateEvent(event: EventEntity)       = dao.updateEvent(event)
    suspend fun deleteEvent(id: Long)                 = dao.deleteEvent(id)
}