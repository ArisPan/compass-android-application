package com.example.projectcompass

import android.app.Application

class CompassApplication : Application() {

    // Because these objects should only be created when they're first needed,
    // rather than at app startup, we're using Kotlin's property delegation: by lazy.
    private val database by lazy { MeasurementRoomDatabase.getDatabase(this) }
    val repository by lazy { MeasurementRepository(database.measurementDAO()) }
}