package com.wikicoding.androidtodolistv2.dao

import androidx.room.*
import com.wikicoding.androidtodolistv2.model.TaskEntity
import kotlinx.coroutines.flow.Flow

/** on the Dao we create the database operations/queries**/
@Dao
interface TaskDao {
    @Insert
    suspend fun insert(taskEntity: TaskEntity)

    @Update
    suspend fun update(taskEntity: TaskEntity)

    @Delete
    suspend fun delete(taskEntity: TaskEntity)

    /**Has to be a List<TaskEntity>, ArrayList does not compile at run-time**/
    /**Flow from kotlinx.coroutines.flow.Flow**/
    @Query("SELECT * FROM `notes-table` ORDER BY completed ASC, date DESC")
    fun fetchAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM `notes-table` WHERE id=:id")
    fun fetchTaskById(id: Int): Flow<TaskEntity>

}