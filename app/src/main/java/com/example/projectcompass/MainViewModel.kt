package com.example.projectcompass

import androidx.lifecycle.*
import androidx.work.*
import kotlinx.coroutines.launch

/*
 * ViewModel can take care of holding and processing all the data needed for the UI
 * while surviving configuration changes.
 * A ViewModel acts as a communication center between the Repository and the UI.
 */
class MainViewModel(
    private val repository: MeasurementRepository,
    application: CompassApplication) : AndroidViewModel(application) {

    /*
     * LiveData is an observable, LifeCycle aware data holder.
     * It automatically stops or resumes observation depending on
     * the lifecycle of the component that listens for changes.
     *
     * Here, we transform Repository's data, from Flow to LiveData
     * and expose the list of measurements as LiveData to the UI.
     * This way, we ensure that every time the data changes in the database,
     * our UI will automatically be updated.
     * As a bonus, Repository is completely separated from the UI through the ViewModel.
     * We follow the same procedure for unpublished measurements. We want to get notified every time
     * a new unpublished measurement is available in order to post it through RabbitMQ (via WorkManager).
     */
    val allMeasurements: LiveData<List<Measurement>> = repository.allMeasurements.asLiveData()
    val unpublishedMeasurements: LiveData<List<Measurement>> = repository.unpublishedMeasurements.asLiveData()

    private val workManager = WorkManager.getInstance(application)

    /*
     * Launching two new coroutines and calling repository's suspend functions
     * insert and setPublished. This way, implementation is encapsulated from the UI.
     * viewModelScope - The coroutine scope of ViewModel based on it's lifecycle.
     * TODO
     *      Since the following functions will be most likely used not by the UI
     *      but the ViewModel itself, they could be easily replaced just by the
     *      repository.function() method calls.
     */
    fun insert(measurement: Measurement) = viewModelScope.launch {
        repository.insert(measurement)
    }

    fun setPublished(measurementID: Int) = viewModelScope.launch {
        repository.setPublished(measurementID)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

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

/*
 * Since MainViewModel has two dependencies, repository and application,
 * we need a custom ViewModelFactory for us to be able to pass these
 * dependencies as parameters to it's constructor.
 * The default implementation accepts no parameters.
 */
class MainViewModelFactory(private val repository: MeasurementRepository,
                           private val application: CompassApplication) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}