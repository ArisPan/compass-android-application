package com.example.projectcompass

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var currentUnpublishedMeasurementsListSize: Int = 0

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

        // A more appropriate place for deleteAll() is in a callback. Keep it here for testing.
        viewModel.deleteAll()

        viewModel.allMeasurements.observe(this, Observer { measurements ->
            measurements?.let {}
        })

        viewModel.unpublishedMeasurements.observe(this, Observer { unpublishedMeasurements ->
            unpublishedMeasurements?.let {

                var newUnpublishedMeasurements: List<Measurement> = emptyList()

                if (currentUnpublishedMeasurementsListSize <= it.size)
                    newUnpublishedMeasurements = it.subList(currentUnpublishedMeasurementsListSize, (it.size))

                currentUnpublishedMeasurementsListSize = it.size

                if (newUnpublishedMeasurements.isNotEmpty())
                    viewModel.postLocationData(newUnpublishedMeasurements)
            }
        })

        for (index in 0..1000) {
            Handler(Looper.getMainLooper()).postDelayed({
                createAndSaveLocationData(index)
            }, 2000)
        }
    }

    // Simulate live location data updates.
    private fun createAndSaveLocationData(index: Int) {

        val id: Int = 0
        val latitude: Double = 10.65536761889327
        val longitude: Double = 10.850841940328824
        val speed: Float = 0.1f
        val accuracy: Float = 10f
        val date: String = "5/4/2021"
        val time: Long = 1000L
        val userID: Int = 0
        val hasBeenPublished: Boolean = false

        val measurement = Measurement(
                id,
                latitude + index.toDouble(),
                longitude + index.toDouble(),
                speed + index.toFloat(),
                accuracy + index.toFloat(),
                date,
                time + index.toLong(),
                userID,
                hasBeenPublished
        )

        viewModel.insert(measurement)
    }

    override fun onResume() {
        super.onResume()
        // Debug DB
        println("In onResume() -> Measurements: ${viewModel.allMeasurements.value}")
    }
}