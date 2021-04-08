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

/* Location Tracking constants */
const val LOCATION_PERMISSION_REQUEST_CODE = 1
const val REQUEST_CHECK_SETTINGS = 2
const val REQUESTING_LOCATION_UPDATES_KEY = "Requesting location updates"
const val TRACKING_KEY = "Tracking"