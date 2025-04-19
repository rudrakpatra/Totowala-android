package com.example.totowala

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class BidActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private var userLatLng: LatLng? = null
    private val auctionMarkers = mutableMapOf<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bid)
        supportActionBar?.title = "Bid"

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermission()
        setupMap()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                userLatLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 14f))
                loadAuctions()
            }
        }

        mMap.setOnMarkerClickListener { marker ->
            val auction = marker.tag as? Map<String, Any> ?: return@setOnMarkerClickListener false
            showBidDialog(auction)
            true
        }
    }

    private fun loadAuctions() {
        db.collection("sharedAuctions").get().addOnSuccessListener { result ->
            auctionMarkers.values.forEach { it.remove() }
            auctionMarkers.clear()

            for (document in result) {
                val auctionId = document.id
                val host = document.get("host") as? Map<*, *> ?: continue
                val location = host["location"] as? Map<*, *> ?: continue

                val lat = (location["latitude"] ?: location["lat"]) as? Double ?: continue
                val lng = (location["longitude"] ?: location["long"]) as? Double ?: continue
                val pos = LatLng(lat, lng)

                val expectedFare = host["expectedFare"]?.toString() ?: "-"
                val marker = mMap.addMarker(
                    MarkerOptions().position(pos).title("Auction #$auctionId - ₹$expectedFare")
                )
                marker?.tag = mapOf("id" to auctionId, "host" to host)
                if (marker != null) auctionMarkers[auctionId] = marker
            }
        }
    }

    private fun showBidDialog(auction: Map<String, Any>) {
        val host = auction["host"] as? Map<*, *> ?: emptyMap<String, Any>()
        val auctionId = auction["id"] as? String ?: "N/A"

        val from = host["from"]?.toString().takeUnless { it.isNullOrBlank() } ?: "Not Provided"
        val to = host["to"]?.toString().takeUnless { it.isNullOrBlank() } ?: "Not Provided"
        val fare = host["expectedFare"]?.toString().takeUnless { it.isNullOrBlank() } ?: "Not Provided"

//        val loc = host["location"] as? Map<*, *>
//        val lat = (loc?.get("latitude") ?: loc?.get("latitude")) as? Double
//        val lng = (loc?.get("longitude") ?: loc?.get("longitude")) as? Double

//        val dist = if (lat != null && lng != null && userLatLng != null) {
//            Haversine.distance(userLatLng!!, LatLng(lat, lng))
//        } else null

        val styledMessage = SpannableStringBuilder()
            .append("From: ").append(from)
            .append("\nTo: ").append(to)
            .append("\nExpected Fare: ₹").append(fare)


        val input = EditText(this)
        input.hint = "Your proposed fare"

        AlertDialog.Builder(this)
            .setTitle("Bid on Auction #$auctionId")
            .setMessage(styledMessage)
            .setView(input)
            .setPositiveButton("Place Bid") { _, _ ->
                val proposedFare = input.text.toString()
                if (proposedFare.isNotBlank()) {
                    placeBid(auctionId, proposedFare)
                } else {
                    Toast.makeText(this, "Enter a valid fare", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun placeBid(auctionId: String, proposedFare: String) {
        val user = UserManager.getUserFromLocalStorage(this)
        if (user == null || userLatLng == null) {
            Toast.makeText(this, "User not found or location unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val bid = mapOf(
            "name" to user.name,
            "phone" to user.phone,
            "proposedFare" to proposedFare,
            "location" to mapOf("lat" to userLatLng!!.latitude, "long" to userLatLng!!.longitude),
            "time" to System.currentTimeMillis()
        )

        db.collection("sharedAuctions").document(auctionId)
            .update("bids", com.google.firebase.firestore.FieldValue.arrayUnion(bid))
            .addOnSuccessListener {
                Toast.makeText(this, "Bid placed successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to place bid", Toast.LENGTH_SHORT).show()
            }
    }
}
