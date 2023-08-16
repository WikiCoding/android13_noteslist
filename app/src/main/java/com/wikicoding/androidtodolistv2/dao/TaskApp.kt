package com.wikicoding.androidtodolistv2.dao

import android.app.Application
import com.wikicoding.androidtodolistv2.db.TaskDatabase

/** initialization of the db connection when called**/
class TaskApp: Application() {
    val db by lazy {
        TaskDatabase.getInstance(this)
    }
}