package com.example.rabbitmq


import com.rabbitmq.client.ConnectionFactory
import org.json.JSONObject


class Send {
    companion object {
        const val QUEUE_NAME = "hello"
    }
}

fun main(argv: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    val connection = factory.newConnection("amqp://guest:guest@localhost:5672/")
    val channel = connection.createChannel()

    channel.queueDeclare(Send.QUEUE_NAME, false, false, false, null)
    //val message = "Hello Woooorld2!"
//    val rootObject = JSONObject()
//    rootObject.put("name","test name")
//    rootObject.put("age","25")
    val message = """{"userID":"3", "x":"52.4995", "y":"26.5005", "speed":"3.8"}"""
    channel.basicPublish("", Send.QUEUE_NAME, null, message.toByteArray(charset("UTF-8")))
    println(" [x] Sent '$message'")

    channel.close()
    connection.close()
}
