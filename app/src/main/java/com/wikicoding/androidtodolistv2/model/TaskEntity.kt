package com.wikicoding.androidtodolistv2.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/** on the entity we create the tableName and the table columns**/
@Entity(tableName = "notes-table")
data class TaskEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val title: String="",
    var completed: Boolean=false,
    @ColumnInfo(name = "date")
    val timestamp: String="") : Serializable /**implements Serializable so I can putExtra on the intent**/