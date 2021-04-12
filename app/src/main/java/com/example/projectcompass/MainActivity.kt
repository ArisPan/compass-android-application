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
import com.google.android.gms.maps.model.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var requestingLocationUpdates = false
    private var tracking = false

    private lateinit var trackingButton: Button
    private lateinit var clearMapButton: Button

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
                (application as CompassApplication).repository,
                (application as CompassApplication)
        )
    }
    private var currentUnpublishedMeasurementsListSize: Int = 0
    private var allMeasurementsIndexOnClear: Int = 0

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
            /*
             * Remember at which measurement the user cleared the map.
             * This way, in case of Activity's destruction, we can only repaint
             * the measurements obtained from that point on.
             */
            allMeasurementsIndexOnClear = viewModel.allMeasurements.value!!.size
            map.clear()
        }

        viewModel.allMeasurements.observe(this, Observer { measurements ->
            measurements?.let {

                if (it.isNotEmpty() && tracking) {

                    val measurement = it[it.lastIndex]
                    /*
                     * LiveData observer is notified with every change in allMeasurements.
                     * That means that whether a new measurement has been obtained
                     * or a field value has been modified, the code in this let{} block will be executed.
                     * We want to notify the UI when a new measurement arrives but not when it's
                     * 'hasBeenPublished' field is updated. That's the use of the following if statement.
                     * We choose to update the UI with measurement.hasBeenPublished == false
                     * because that's it's state when the measurement is obtained.
                     * 'hasBeenPublished' becomes true when the measurement is sent to the backend
                     * which requires an internet connection. That's irrelevant to the UI.
                     */
                    if (!measurement.hasBeenPublished) {
                        placeMarkerOnMap(measurement)
                        if (it.size > 1) {
                            /*
                             * If the clear button has been pressed, only draw a line from that point on.
                             * If not, allMeasurementsIndexOnClear is 0, thus the sublist is the whole list.
                             */
                            addPolyline(it.subList(allMeasurementsIndexOnClear, (it.size)))
                        }
                    }
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
        outState.putInt(INDEX_ON_CLEAR, allMeasurementsIndexOnClear)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY)
            tracking = savedInstanceState.getBoolean(
                    TRACKING_KEY)
            allMeasurementsIndexOnClear = savedInstanceState.getInt(
                    INDEX_ON_CLEAR
            )
        }

        if(requestingLocationUpdates && tracking) {
            trackingButton.text = getString(R.string.stop_button)
            trackingButton.setBackgroundColor(Color.RED)
            createLocationRequest()
        }
        else {
            trackingButton.setBackgroundColor(Color.GREEN)
        }
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
            repaintMap()
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
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 21f))
            }
        }
    }

    private fun repaintMap() {

        if (checkPermission())
            map.isMyLocationEnabled = true

        val measurements = viewModel.allMeasurements.value!!.subList(
                allMeasurementsIndexOnClear,
                viewModel.allMeasurements.value!!.size)

        for (measurement in measurements) {
            placeMarkerOnMap(measurement)
        }
        addPolyline(measurements)
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

    private fun initializeLocationCallback() {

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {

                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation

                writeMeasurementToDB(currentLocation)
            }
        }
    }

    private fun writeMeasurementToDB(location: Location) {

        val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val measurement = Measurement(
                0,
                location.latitude,
                location.longitude,
                getSpeed(location),
                location.accuracy,
                date,
                location.time,
                0,
                false
        )
        viewModel.insert(measurement)
    }

    private fun getSpeed(currentLocation: Location): Float {

        if (currentLocation.hasSpeed() && currentLocation.speed != 0.0f)
            return currentLocation.speed

        val numberOfMeasurements = viewModel.allMeasurements.value!!.size
        if (numberOfMeasurements > 0) {

            val previousMeasurement = viewModel.allMeasurements.value!![numberOfMeasurements - 1]
            val previousLocation = Location("")
            previousLocation.latitude = previousMeasurement.latitude
            previousLocation.longitude = previousMeasurement.longitude
            previousLocation.time = previousMeasurement.time

            val distanceInMeters = previousLocation.distanceTo(currentLocation)
            val elapsedTimeInSeconds = (currentLocation.time - previousLocation.time) / 1000

            return distanceInMeters / elapsedTimeInSeconds
        }
        return currentLocation.speed
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

    private fun addPolyline(measurements: List<Measurement>) {

        val list = mutableListOf<LatLng>()
        for (measurement in measurements) {
            list.add(LatLng(measurement.latitude, measurement.longitude))
        }
        val polylineOptions = PolylineOptions()
        polylineOptions.addAll(list)
        polylineOptions
                .width(10f)
                .color(Color.RED)

        map.addPolyline(polylineOptions)
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