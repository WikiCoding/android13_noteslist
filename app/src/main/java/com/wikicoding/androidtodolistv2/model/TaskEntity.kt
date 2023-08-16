package com.wikicoding.androidtodolistv2.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** on the entity we create the tableName and the table columns**/
@Entity(tableName = "notes-table")
data class TaskEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val title: String="",
    var completed: Boolean=false,
    @ColumnInfo(name = "date")
    val timestamp: String="")