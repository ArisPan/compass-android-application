package com.example.projectcompass

import kotlinx.coroutines.flow.Flow

/*
 * A repository class abstracts access to multiple data sources.
 * It provides a clean API for data access to the rest of the application.
 * @params
 *  measurementDAO - The DAO is passed into the repository constructor as opposed to the whole database.
 *  Since the DAO contains all the read/write methods for the database, it should suffice.
 */
class MeasurementRepository(private val measurementDAO: MeasurementDAO) {

    val allMeasurements: Flow<List<Measurement>> = measurementDAO.loadAll()
    val unpublishedMeasurements: Flow<List<Measurement>> = measurementDAO.loadUnpublished()

    /* TODO
     * @Suppress("RedundantSuspendModifier")
     * @WorkerThread
     * Check: https://developer.android.com/codelabs/android-room-with-a-view-kotlin#8
     */
    suspend fun insert(measurement: Measurement) {
        measurementDAO.insert(measurement)
    }

    suspend fun insertAll(measurements: List<Measurement>) {
        measurementDAO.insertAll(measurements)
    }

    suspend fun setPublished(measurementID: Int) {
        measurementDAO.setPublished(measurementID)
    }
}