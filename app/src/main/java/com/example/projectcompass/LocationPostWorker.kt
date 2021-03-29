package com.example.projectcompass

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.Worker

/*
 * Work is defined using the Worker class.
 * The doWork() method runs asynchronously on a background thread provided by WorkManager.
 */
class LocationPostWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    /*
     * @return The Result returned from doWork() informs the WorkManager service
     * whether the work succeeded.
     */
    override fun doWork() : Result {

        val latitude = inputData.getDouble("latitude", 0.0)
        val longitude = inputData.getDouble("longitude", 0.0)

        return try {
            val message = """{"latitude": $latitude, "longitude": $longitude}"""

            /* TODO
             * In the following snippet, we publish the message containing the location data to an exchange.
             * By default, the fanout exchange is used, broadcasting all messages to all consumers.
             * As a next step, we will change this to a direct exchange in order for each message to be published
             * to the queues whose binding key exactly matches the routing key of the message.
             * This way, we will have a clear separation between the location data sent by different users.
             */
            RabbitMQClient.channel.basicPublish(
                "",
                QUEUE_NAME,
                null,
                message.toByteArray(charset("UTF-8")))
            println("Sent message: $message")
            Result.success()
        } catch (throwable: Throwable) {
            Result.failure()
        }
    }
}