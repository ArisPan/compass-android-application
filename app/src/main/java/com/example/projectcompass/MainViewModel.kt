package com.example.projectcompass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.work.*
import kotlin.coroutines.coroutineContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    /*
     * Our use case demands that given a network connection, retrieved location data will be
     * periodically sent to a remote server. This transaction is a textbook background service.
     * WorkManager is used to schedule this transaction, separating it's execution from the main thread.
     * postLocationData() is responsible for composing and scheduling the work request.
     */
    internal fun postLocationData() {

        // Create Network constraint.
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        // Build Data object to pass to Worker Class.
        val coordinates: Data = workDataOf("latitude" to 49.65536761889327,
                                                "longitude" to 10.850841940328824)

        // Add WorkRequest to publish location data to RabbitMQ Queue.
        val locationPost: WorkRequest = OneTimeWorkRequestBuilder<LocationPostWorker>()
            .setInputData(coordinates)
            .setConstraints(constraints)
            .build()

        // Actually start the work.
        workManager.enqueue(locationPost)
    }
}