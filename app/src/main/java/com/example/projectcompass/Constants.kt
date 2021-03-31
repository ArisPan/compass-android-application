package com.example.projectcompass

/* RabbitMQ constants */

// Queue Constants
const val QUEUE_NAME = "Location Data"
const val DURABLE = true
// Factory Constants
const val HOST = "192.168.1.5"
const val PORT = 5672
const val USERNAME = "guest"
const val PASSWORD = "guest"

const val LOCATION_POST_WORK_NAME = "Post location data to RabbitMQ queue"