package com.example.projectcompass

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.Worker
import com.google.gson.Gson

/*
 * Work is defined using the Worker class.
 * The doWork() method runs asynchronously on a background thread provided by WorkManager.
 */
class LocationPostWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    val database = MeasurementRoomDatabase.getDatabase(applicationContext)

    /*
     * @return The Result returned from doWork() informs the WorkManager service
     * whether the work succeeded.
     */
    override fun doWork() : Result {

        val measurementID = inputData.getInt("id", 0)

        return try {
            println("Worker -> Thread ID: ${Thread.currentThread().id}")

            // Parse the contents of measurement object to JSON format.
            val jsonMessage = Gson().toJson(database.measurementDAO().load(measurementID))

            // Publish the message to a fanout exchange. Broadcasts to all consumers.
            RabbitMQClient.channel.basicPublish(
                "",
                QUEUE_NAME,
                null,
                jsonMessage.toByteArray(charset("UTF-8"))
            )
            println("Sent message: $jsonMessage")
            database.measurementDAO().setPublished(measurementID)  // Declare measurement as published.

            Result.success()
        } catch (throwable: Throwable) {
            Result.failure()
        }
    }
}