package com.khadr.forge.features.tasks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ── Queries ───────────────────────────────────────────────────────────────
    @Query("""
        SELECT * FROM tasks 
        ORDER BY 
            CASE priority 
                WHEN 'HIGH'   THEN 0 
                WHEN 'MEDIUM' THEN 1 
                WHEN 'LOW'    THEN 2 
            END ASC,
            createdAt DESC
    """)
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE isDone = 0 
        ORDER BY 
            CASE priority 
                WHEN 'HIGH'   THEN 0 
                WHEN 'MEDIUM' THEN 1 
                WHEN 'LOW'    THEN 2 
            END ASC,
            createdAt DESC
    """)
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDone = 0")
    fun getPendingCount(): Flow<Int>

    // ── Write ─────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("UPDATE tasks SET isDone = NOT isDone WHERE id = :id")
    suspend fun toggleDone(id: Long)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun clearCompleted()
}