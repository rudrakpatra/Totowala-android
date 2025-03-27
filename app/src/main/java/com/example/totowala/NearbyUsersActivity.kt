package com.example.totowala

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.totowala.databinding.ActivityNearbyUsersBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class NearbyUsersActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityNearbyUsersBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userLatLng: LatLng
    private val markers = mutableMapOf<String, Marker>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNearbyUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Nearby Users"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        checkGPSEnabled()
        setupMap()
        setupLocationSharingButton()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupLocationSharingButton(){
        binding.btnShareLocation.setOnClickListener { setLocationSharing(!LocationShareService.isServiceRunning(this)) }
        setLocationSharing(LocationShareService.isServiceRunning(this))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setLocationSharing(value: Boolean) {
        val intent = Intent(this, LocationShareService::class.java)
        if (value) {
            binding.btnShareLocation.text = "Sharing location..."
            startForegroundService(intent)
        } else {
            binding.btnShareLocation.text = "Share Location"
            stopService(intent)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun checkGPSEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS for location access", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return
        }

        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.isMyLocationEnabled = true

        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
            if (!success) {
                Log.e("MAP_STYLE", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MAP_STYLE", "Can't find style. Error: ", e)
        }


        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                userLatLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            }
        }
        LocationShareService.addOnSharedLocationChangeListener { sharedLocation ->
            val phone = sharedLocation.phone
            val latLng = LatLng(sharedLocation.latitude, sharedLocation.longitude)
            val type = sharedLocation.type
            val active = sharedLocation.active

            val existingMarker = markers[phone]

            if (active) {
                if (existingMarker != null) {
                    existingMarker.position = latLng
                } else {
                    val markerOptions = MarkerOptions()
                        .position(latLng)
                        .icon(bitmapDescriptorFromVector(this, type))

                    val marker = mMap.addMarker(markerOptions)
                    marker?.tag = sharedLocation

                    if (marker != null) {
                        markers[phone] = marker
                    }
                }
            } else {
                // Remove marker if user is inactive
                existingMarker?.remove()
                markers.remove(phone)
            }
        }


        // Marker click listener to show user details
        mMap.setOnMarkerClickListener { marker ->
            val sharedLocation = marker.tag as? LocationShareService.SharedLocation
            if(sharedLocation !== null) {
                val phone = sharedLocation.phone
                val latLng = LatLng(sharedLocation.latitude, sharedLocation.longitude)
                val name = sharedLocation.name
                val type = sharedLocation.type
                AlertDialog.Builder(this)
                    .setTitle("${name} | ${type}")
                    .setMessage("${Haversine.distance(userLatLng, latLng)} away")
                    .setPositiveButton("Call") { _, _ ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone}"))
                        startActivity(intent)
                    }
                    .show()
            }
            true
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, userType: String): BitmapDescriptor {
        val background = ContextCompat.getDrawable(context, R.drawable.iconbg)!!.mutate()
        val foreground = if (userType == "driver") R.drawable.ic_car else R.drawable.ic_passenger
        val vectorDrawable = ContextCompat.getDrawable(context, foreground)!!.mutate()

        val color = if (userType == "driver") ContextCompat.getColor(context, R.color.purple_700)
        else ContextCompat.getColor(context, R.color.teal_700)

        background.setTint(color)
        background.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        vectorDrawable.setBounds(10, 10, vectorDrawable.intrinsicWidth - 5, vectorDrawable.intrinsicHeight - 5)

        val bitmap = Bitmap.createBitmap(background.intrinsicWidth, background.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background.draw(canvas)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.maps_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Open SettingsActivity when the gear icon is clicked
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}