package com.example.projectcompass

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class CompassApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    // Because these objects should only be created when they're first needed,
    // rather than at app startup, we're using Kotlin's property delegation: by lazy.
    private val database by lazy { MeasurementRoomDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { MeasurementRepository(database.measurementDAO()) }
}