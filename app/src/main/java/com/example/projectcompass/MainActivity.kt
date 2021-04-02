package com.example.projectcompass

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            (application as CompassApplication).repository,
            (application as CompassApplication)
        )
    }

    /*
     * @param
     *  savedInstanceState - Bundle object containing the activity's previously saved state.
     *  If the activity has never existed before, the value of the Bundle object is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        // Call the super class onCreate to complete the creation of activity like the view hierarchy.
        super.onCreate(savedInstanceState)
        // Set the user interface layout for this activity.
        setContentView(R.layout.activity_main)

        // A more appropriate place for deleteAll() is in a callback.
        // Keep the call here for testing purposes.
        println("Dropping DB.")
        viewModel.deleteAll()

        // Debug Thread Blocking
        println("MainActivity -> Thread ID: ${Thread.currentThread().id}")

        viewModel.allMeasurements.observe(this, Observer {
            measurements -> measurements?.let {}
        })

        viewModel.unpublishedMeasurements.observe(this, Observer {
            unpublishedMeasurements -> unpublishedMeasurements?.let {
            // Compose and schedule a work request to publish location data to RabbitMQ Queue.
            println("In unpublishedMeasurements.observe -> Measurements: ${it.toString()}")
            viewModel.postLocationData(viewModel.unpublishedMeasurements.value!!) }
        })

        createAndSaveLocationData()
    }

    // Debug DB
    private fun createAndSaveLocationData() {

        val measurement0 = Measurement(
            0,
            49.65536761889327,
            10.850841940328824,
            0.7f,
            30f,
            "1/4/21",
            4000L,
            0,
            false
        )

        val measurement1 = Measurement(
            0,
            59.65536761889327,
            60.850841940328824,
            0.8f,
            35f,
            "1/4/21",
            4500L,
            0,
            false
        )

        val measurement2 = Measurement(
            0,
            19.65536761889327,
            90.850841940328824,
            0.5f,
            40f,
            "1/4/21",
            5000L,
            0,
            false
        )

        viewModel.insert(measurement0)
        viewModel.insert(measurement1)
        viewModel.insert(measurement2)
    }

    override fun onResume() {
        super.onResume()
        // Debug DB
        println("In onResume() -> Measurements: ${viewModel.allMeasurements.value}")
    }
}