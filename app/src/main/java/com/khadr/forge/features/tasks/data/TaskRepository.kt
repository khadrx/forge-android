package com.khadr.forge.features.tasks.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val dao: TaskDao
) {
    fun getAllTasks()     : Flow<List<TaskEntity>> = dao.getAllTasks()
    fun getPendingTasks() : Flow<List<TaskEntity>> = dao.getPendingTasks()
    fun getPendingCount() : Flow<Int>              = dao.getPendingCount()

    suspend fun insertTask(t: TaskEntity): Long    = dao.insertTask(t)
    suspend fun updateTask(t: TaskEntity)           = dao.updateTask(t)
    suspend fun deleteTask(id: Long)                = dao.deleteTask(id)
    suspend fun toggleDone(id: Long)                = dao.toggleDone(id)
    suspend fun clearCompleted()                    = dao.clearCompleted()
}