package com.example.projectcompass

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    /*
     * Perform basic application startup logic that should happen
     * only once for the entire life of the activity.
     * @param savedInstanceState Bundle object containing the activity's previously saved state.
     * If the activity has never existed before, the value of the Bundle object is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        // Call the super class onCreate to complete the creation of activity like the view hierarchy.
        super.onCreate(savedInstanceState)
        // Set the user interface layout for this activity.
        setContentView(R.layout.activity_main)

        // Create MainViewModel
        viewModel = ViewModelProvider(this, AndroidViewModelFactory(application)).get(MainViewModel::class.java)
        // Compose and schedule a work request to publish location data to RabbitMQ Queue.
        viewModel.postLocationData()
    }
}