package com.example.projectcompass

import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var requestingLocationUpdates = false
    private var tracking = false
    private var markerCount = 0

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

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Create an instance of the Fused Location Provider Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initializeLocationCallback()

        val trackingButton = findViewById<Button>(R.id.button_tracking)
        trackingButton.setOnClickListener {
            // On click, reverse the value of tracking signaling the change in state.
            tracking = !tracking
            if (tracking) {
                createLocationRequest()
                trackingButton.text = getString(R.string.stop_button)
                trackingButton.setBackgroundColor(Color.RED)
            }
            else {
                // Stop location updates.
                fusedLocationClient.removeLocationUpdates(locationCallback)
                trackingButton.text = getString(R.string.start_button)
                trackingButton.setBackgroundColor(Color.GREEN)
            }
        }
        val clearMapButton = findViewById<Button>(R.id.button_mapClear)
        clearMapButton.setOnClickListener {
            map.clear()
        }

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
    }

    override fun onMarkerClick(p0: Marker?) = false

    /*
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)
        setUpMap()
    }

    private fun setUpMap() {
        if (!checkPermission())
            return

        map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                currentLocation = location
                println("setupMap() -> Latitude: ${currentLocation.latitude}\nLongitude: ${currentLocation.longitude}")
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun initializeLocationCallback() {

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {

                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation

                val measurement = Measurement(
                        0,
                        currentLocation.latitude,
                        currentLocation.longitude,
                        0.1f,
                        30f,
                        "PLACEHOLDER",
                        1000L,
                        0,
                        false
                )
                viewModel.insert(measurement)
                println("Current Location -> Latitude: ${currentLocation.latitude} Longitude: ${currentLocation.longitude}")
                placeMarkerOnMap(LatLng(currentLocation.latitude, currentLocation.longitude))
            }
        }
    }

    private fun createLocationRequest() {

        locationRequest = LocationRequest.create().apply {
            interval = 12000
            fastestInterval = 7000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Get the current location settings of user's device.
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        // Check whether the current location settings are satisfied
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // All location settings are satisfied. The client can initialize location requests here.
        task.addOnSuccessListener {
            requestingLocationUpdates = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult()
                    e.startResolutionForResult(
                            this@MainActivity,
                            REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun startLocationUpdates() {

        if (!checkPermission())
            return

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        )
    }

    private fun checkPermission(): Boolean {

        if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    // Should go in observer.
    private fun placeMarkerOnMap(location: LatLng) {
        if(tracking){
            val markerOptions = MarkerOptions().position(location)
            if (markerCount < 3) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                markerOptions.position(location)
            }
            else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            }
            markerCount += 1
            val titleStr = getAddress(location)
            markerOptions.title(titleStr)

            map.addMarker(markerOptions)
        }
    }

    private fun getAddress(latLng: LatLng): String {
        // gets addresses from location
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (null != addresses && addresses.isNotEmpty()) {
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex) {
                    addressText += if (i == 0)
                        address.getAddressLine(i)
                    else "\n" + address.getAddressLine(i)
                }
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage!!)
        }

        return addressText
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates)
            startLocationUpdates()
    }
}