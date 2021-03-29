package com.example.projectcompass

import android.os.StrictMode

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

/*
 * Singleton containing all the necessary setup for RabbitMQ.
 * Object declarations are initialized lazily, when accessed for the first time.
 * After that, each reference corresponds to the same instance.
 */
object RabbitMQClient {

    private var factory: ConnectionFactory = ConnectionFactory()
    private var connection: Connection
    internal var channel: Channel

    init {
        /*
         * StrictMode is used to catch accidental disk or network access on specified thread.
         * Since RabbitMQ messaging will be happening on a background thread, we specify that
         * every access detection will be disabled for this specific thread.
         */
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Adjust connection settings.
        factory.host = HOST
        factory.port = PORT
        factory.virtualHost = "/"
        factory.username = USERNAME
        factory.password = PASSWORD

        connection = factory.newConnection()

        channel = connection.createChannel()
        channel.queueDeclare(QUEUE_NAME, DURABLE, false, false, null)
        println("\nInitialized!\n")
    }

    fun close() {
        channel.close()
        connection.close()
    }
}