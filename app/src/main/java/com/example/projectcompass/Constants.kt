package com.example.projectcompass

/* RabbitMQ constants */

// Kept for compatibility with consumer.kt, producer.kt.
// When they are gone, these are gone.
const val CONNECTION_NODE = "localhost"
const val CONNECTION_NAME = "amqp://guest:guest@localhost:5672/"

// Queue Constants
const val QUEUE_NAME = "Location Data"
const val DURABLE = true
// Factory Constants
const val HOST = "192.168.1.5"
const val PORT = 5672
const val USERNAME = "guest"
const val PASSWORD = "guest"

const val LOCATION_POST_WORK_NAME = "Post location data to RabbitMQ queue"