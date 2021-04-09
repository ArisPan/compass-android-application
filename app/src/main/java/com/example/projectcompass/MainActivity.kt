package com.example.projectcompass

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
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

    private lateinit var trackingButton: Button
    private lateinit var clearMapButton: Button

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

        trackingButton = findViewById(R.id.button_tracking)
        trackingButton.setOnClickListener {
            // On click, reverse the value of tracking signaling the change in state.
            tracking = !tracking
            if (tracking) {
                initializeLocationCallback()
                createLocationRequest()
                trackingButton.text = getString(R.string.stop_button)
                trackingButton.setBackgroundColor(Color.RED)
            }
            else {
                // Stop location updates.
                fusedLocationClient.removeLocationUpdates(locationCallback)
                requestingLocationUpdates = false
                trackingButton.text = getString(R.string.start_button)
                trackingButton.setBackgroundColor(Color.GREEN)
            }
        }
        clearMapButton = findViewById(R.id.button_mapClear)
        clearMapButton.setOnClickListener {
            map.clear()
        }

        viewModel.allMeasurements.observe(this, Observer { measurements ->
            measurements?.let {

                if (it.isNotEmpty() && tracking) {

                    val measurement = it[it.lastIndex]
                    println("--------------------New Measurement--------------------")
                    println("Placing marker on map for measurement: $measurement")
                    placeMarkerOnMap(measurement)
                }
            }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        outState.putBoolean(TRACKING_KEY, tracking)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY)
            tracking = savedInstanceState.getBoolean(
                    TRACKING_KEY)
        }

        if(requestingLocationUpdates && tracking)
            trackingButton.text = getString(R.string.stop_button)
            trackingButton.setBackgroundColor(Color.RED)
            createLocationRequest()
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
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.setOnMarkerClickListener(this)
        setUpMap()
    }

    private fun setUpMap() {
        /*
         * allMeasurements is null when no location data has been obtained.
         * This is our first location reading.
         * If allMeasurements is not null but setUpMap() is called, we are dealing with Activity's
         * reconstruction after it has been destroyed by change in orientation (or something else).
         * In this case, repaint already obtained measurements with markers on recreated map.
         */
        if (viewModel.allMeasurements.value == null)
            getLastKnownLocation()
        else
            repaintMarkers()
    }

    /*
     * The last known location of the device provides a handy base from which to start,
     * ensuring that the app has a known location before starting the periodic location updates.
     */
    private fun getLastKnownLocation() {

        if (!checkPermission())
            return

        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                writeMeasurementToDB(location)

                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 21f))
            }
        }
    }

    private fun repaintMarkers() {

        for (measurement in viewModel.allMeasurements.value!!) {
            placeMarkerOnMap(measurement)
        }
    }

    private fun initializeLocationCallback() {

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {

                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation

                writeMeasurementToDB(currentLocation)
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

    private fun writeMeasurementToDB(location: Location) {

        val measurement = Measurement(
                0,
                location.latitude,
                location.longitude,
                0.1f,
                30f,
                "PLACEHOLDER",
                1000L,
                0,
                false
        )
        viewModel.insert(measurement)
    }

    private fun checkPermission(): Boolean {

        if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    private fun placeMarkerOnMap(measurement: Measurement) {

        val coordinates = LatLng(measurement.latitude, measurement.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
        markerOptions.position(coordinates)
        markerOptions.title(getAddress(coordinates))

        map.addMarker(markerOptions)
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

        /*
         * Case: Activity is destroyed but location updates continue.
         * For example, a change in device's orientation may destroy the current activity.
         * We want the location updates to continue from where they were left off.
         * Upon Activity's reconstruction, a new instance of locationCallback is created
         * but is not initialized (locationCallback is initialized upon pressing the Start button
         * when the user chooses to start the route). In order to keep the updates coming,
         * we need to initialize the new locationCallback instance.
         */
        if (!this::locationCallback.isInitialized)
            initializeLocationCallback()
        if (requestingLocationUpdates)
            startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()

        /*
         * Upon Activity's reconstruction, a new instance of fusedLocationClient is created
         * and is held responsible for new location updates. Thus, we have to remove updates from
         * current listener.
         */
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}