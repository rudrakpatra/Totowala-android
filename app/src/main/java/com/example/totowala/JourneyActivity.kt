package com.example.totowala

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.example.totowala.databinding.ActivityJourneyBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.type.LatLng as ProtoLatLng

class JourneyActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityJourneyBinding
    private lateinit var journey: Journey

    private lateinit var fromLatLng: LatLng
    private lateinit var toLatLng: LatLng

    private lateinit var driverName: String
    private lateinit var driverPhone: String
    private lateinit var vehicleInfo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJourneyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Extract driver and location data from Intent
        driverName = intent.getStringExtra("driverName") ?: "Unknown"
        driverPhone = intent.getStringExtra("driverPhone") ?: "Unknown"
        vehicleInfo = intent.getStringExtra("vehicleInfo") ?: "Unknown"

        val fromLat = intent.getDoubleExtra("fromLat", 0.0)
        val fromLng = intent.getDoubleExtra("fromLng", 0.0)
        val toLat = intent.getDoubleExtra("toLat", 0.0)
        val toLng = intent.getDoubleExtra("toLng", 0.0)

        fromLatLng = LatLng(fromLat, fromLng)
        toLatLng = LatLng(toLat, toLng)

        // Initialize journey object
        journey = Journey(
            from = ProtoLatLng.newBuilder().setLatitude(fromLat).setLongitude(fromLng).build(),
            to = ProtoLatLng.newBuilder().setLatitude(toLat).setLongitude(toLng).build(),
            startedAt = Timestamp.now(),
            endedAt = null,
            fare = Fare(0.0, "", "pending", Timestamp.now())
        )

        Log.i("JourneyActivity", "Journey initialized: $journey")

        // Setup Google Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Display driver details
        binding.driverName.text = "Driver: $driverName"
        binding.driverVehicle.text = "Vehicle: $vehicleInfo"
        binding.driverPhone.text = "Phone: $driverPhone"

        // Call driver action
        binding.callDriverButton.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$driverPhone")
            }
            startActivity(callIntent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        map.addMarker(MarkerOptions().position(fromLatLng).title("Pickup"))
        map.addMarker(MarkerOptions().position(toLatLng).title("Drop-off"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(fromLatLng, 12f))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.maps_menu, menu)
        return true
    }
}
