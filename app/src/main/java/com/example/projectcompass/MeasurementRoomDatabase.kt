package com.example.projectcompass

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * MeasurementRoomDatabase defines the database configuration
 * and serves as the app's main access point to the persisted data.
 * @Database - Annotate the class to be a Room database.
 * @params
 *  entities - Each entity corresponds to a table that will be created in the database.
 *  version - The current version of our schema.
 *      NOTE: Every time you alter the schema by adding, removing or modifying tables,
 *      you have to increase the database version. Alongside version update, you have to provide
 *      the appropriate Migration.
 */
@Database(entities = [Measurement::class], version = 1)
abstract class MeasurementRoomDatabase : RoomDatabase() {

    /*
     * For each DAO class that is associated with the database,
     * the database class must define an abstract method that has zero arguments
     * and returns an instance of the DAO class. It basically exposes DAOs
     * through an abstract "getter" method for each @Dao.
     * Here, we only have one DAO class.
     */
    abstract fun measurementDAO() : MeasurementDAO

    /*
     * We follow the singleton design pattern when instantiating a Database object.
     * Each RoomDatabase instance is fairly expensive, and you rarely need access to
     * multiple instances within a single process.
     */
    companion object {

        @Volatile
        private var INSTANCE: MeasurementRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope) : MeasurementRoomDatabase {
            // If INSTANCE is null, create the Database. Else, return it.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeasurementRoomDatabase::class.java,
                    "MeasurementDatabase"
                )
                        .addCallback(RoomDatabaseCallback(scope))
                        .build()
                INSTANCE = instance
                instance
            }
        }

        private class RoomDatabaseCallback(
                private val scope: CoroutineScope) : RoomDatabase.Callback() {

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        dropDB(database.measurementDAO())
                    }
                }
            }
        }

        suspend fun dropDB(measurementDAO: MeasurementDAO) {
            println("In DatabaseCallback -> Drop DB.")
            measurementDAO.deleteAll()
        }
    }
}