package com.example.projectcompass

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/*
 * @Dao - Data Access Objects, or DAOs are used to interact with stored data in the app's database.
 * Each DAO includes methods that offer abstract access to your app's database.
 * At compile time, Room automatically generates implementations of the DAOs that you define.
 *
 * @Insert - Allows the definition of methods that insert
 * their parameters into the appropriate table in the database.
 * When an @Insert method is called, Room inserts each passed entity
 * instance into the corresponding database table.
 *
 * @Query - Allows you to write SQL statements and expose them as DAO methods.
 * Room validates SQL queries at compile time. This means that if there's a problem with your query,
 * a compilation error occurs instead of a runtime failure.
 */
@Dao
interface MeasurementDAO {
    /*
     * Inserts a single Measurement object.
     * TODO
     *  @return - Returns a long value which is the new rowId of the inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(measurement: Measurement)

    /*
     * Inserts a list of Measurements.
     * TODO
     *  @return - Returns a list of long values
     *  with each value being the rowId of each inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(measurements: List<Measurement>)

    /*
     * Flow is used to observe data changes. Useful for displaying updated data in the UI.
     * When Room queries return Flow, the queries are automatically run asynchronously
     * on a background thread. Thus, keyword 'suspend' can be omitted.
     */
    // Loads all Measurement tuples ordered by measurement id in ascending order.
    @Query("SELECT * FROM Measurement ORDER BY id ASC")
    fun loadAll(): Flow<List<Measurement>>

    /*
     * Loads all Measurement tuples that have not been sent through RabbitMQ.
     * They are ordered by measurement id in ascending order.
     * @return - A List of Measurements.
     */
    @Query("SELECT * FROM Measurement WHERE hasBeenPublished == 0 ORDER BY id ASC")
    fun loadUnpublished(): Flow<List<Measurement>>

    // Update given measurement's hasBeenPublished field with 'true'.
    @Query("UPDATE Measurement SET hasBeenPublished = 1 WHERE id == :id")
    suspend fun setPublished(id: Int)

    @Query("DELETE FROM Measurement")
    suspend fun deleteAll()
}