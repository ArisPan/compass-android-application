package com.example.projectcompass

import androidx.annotation.WorkerThread
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

    @WorkerThread
    suspend fun insert(measurement: Measurement) {
        measurementDAO.insert(measurement)
    }

    @WorkerThread
    suspend fun insertAll(measurements: List<Measurement>) {
        measurementDAO.insertAll(measurements)
    }

    @WorkerThread
    suspend fun setPublished(measurementID: Int) {
        measurementDAO.setPublished(measurementID)
    }

    @WorkerThread
    suspend fun deleteAll() {
        measurementDAO.deleteAll()
    }
}