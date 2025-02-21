package com.android.attendanceapp

import android.app.Application
import com.android.attendanceapp.database.AppDatabase

class AppClass:Application() {
    lateinit var database: AppDatabase
    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }
}