package com.example.totowala

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import kotlin.random.Random

class AuctionsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private lateinit var updateButton: Button
    private lateinit var fromInput: EditText
    private lateinit var toInput: EditText
    private lateinit var expectedFareInput: EditText
    private lateinit var auctionId:String

    private var userLatLng: LatLng? = null
    private val markers = mutableMapOf<String, Marker>()

    private var activeAuction: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auction)

        auctionId=Random.nextInt(100000, 999999).toString()

        supportActionBar?.title = "Auction #$auctionId"

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fromInput = findViewById(R.id.from_input)
        toInput = findViewById(R.id.to_input)
        expectedFareInput = findViewById(R.id.expected_fare_input)
        updateButton = findViewById(R.id.update_button)

        checkLocationPermission()
        checkGPSEnabled()
        setupMap()
        updateButton.text = "Create Auction";
        updateButton.setOnClickListener {
            createAuction();
            updateButton.text = "Update Auction";
            updateButton.setOnClickListener { updateAuctionDetails() }
        }
        loadAuctionDetails()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.cancelAuction()
    }
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun checkGPSEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
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
                userLatLng=LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f))
                if(updateButton.text === "Update Auction")
                db.collection("sharedAuctions").document(auctionId)
                    .update("host.location",userLatLng)
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        loadBids()
    }

    private fun loadAuctionDetails() {
        db.collection("sharedAuctions").document(auctionId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    fromInput.setText(document.get("host.from")?.toString() ?: "")
                    toInput.setText(document.get("host.to")?.toString() ?: "")
                    expectedFareInput.setText(document.get("host.expectedFare")?.toString() ?: "")

                    activeAuction = true
                    setAuctionActive(true)
                }
            }
    }
    private fun createAuction() {
        val from = fromInput.text.toString().takeIf { it.isNotBlank() }
        val to = toInput.text.toString().takeIf { it.isNotBlank() }
        val expectedFare = expectedFareInput.text.toString().takeIf { it.isNotBlank() }?.toDoubleOrNull()

        val user = UserManager.getUserFromLocalStorage(this)

        if(user == null){
            Toast.makeText(this, "User Not found", Toast.LENGTH_SHORT).show()
            return
        }

        val auctionData = mapOf(
            "host" to mapOf(
                "name" to user.name,
                "phone" to user.phone,
                "type" to user.type,
                "location" to userLatLng,
                "time" to System.currentTimeMillis(),
                "pickup" to if (from != null) mapOf("lat" to 0.0, "long" to 0.0) else null,
                "destination" to if (to != null) mapOf("lat" to 0.0, "long" to 0.0) else null,
                "from" to from,
                "to" to to,
                "expectedFare" to expectedFare
            ),
            "bids" to emptyList<Map<String, Any>>()
        )

        db.collection("sharedAuctions").document(auctionId)
            .set(auctionData)
            .addOnSuccessListener {
//                updateButton.isEnabled = false
                Toast.makeText(this, "Auction created", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create auction", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelAuction(){
        db.collection("sharedAuctions").document(auctionId)
            .delete()
            .addOnSuccessListener {
//                updateButton.isEnabled = false
            }
    }


    private fun updateAuctionDetails() {
        val from = fromInput.text.toString()
        val to = toInput.text.toString()
        val expectedFare = expectedFareInput.text.toString()

        val updateData = mapOf(
            "host.from" to from,
            "host.to" to to,
            "host.expectedFare" to expectedFare
        )

        db.collection("sharedAuctions").document(auctionId)
            .update(updateData)
            .addOnSuccessListener {
//                updateButton.isEnabled = false
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setAuctionActive(isActive: Boolean) {
        db.collection("sharedAuctions").document(auctionId)
            .update("host.active", isActive)
    }

    override fun onResume() {
        super.onResume()
        if (activeAuction) setAuctionActive(true)
    }

    override fun onPause() {
        super.onPause()
        if (activeAuction) setAuctionActive(false)
    }

    private fun loadBids() {

        db.collection("sharedAuctions").document(auctionId)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    val bids = document.get("bids") as? List<Map<String, Any>> ?: emptyList()

                    // Clear existing markers
                    markers.values.forEach { it.remove() }
                    markers.clear()

                    for (bid in bids) {
                        val name = bid["name"] as String
                        val phone = bid["phone"] as String
                        val proposedFare = bid["proposedFare"] as String
                        val lat = (bid["location"] as Map<String, Any>)["lat"] as Double
                        val long = (bid["location"] as Map<String, Any>)["long"] as Double
                        val time = bid["time"] as Long

                        val latLng = LatLng(lat, long)
                        val markerOptions = MarkerOptions()
                            .position(latLng)
                            .title("$name: $proposedFare")
                        val marker = mMap.addMarker(markerOptions)
                        marker?.tag = bid

                        if (marker != null) markers[phone] = marker
                    }
                }
            }

        mMap.setOnMarkerClickListener { marker ->
            val bid = marker.tag as? Map<String, Any> ?: return@setOnMarkerClickListener false
            val name = bid["name"] as String
            val phone = bid["phone"] as String
            val proposedFare = bid["proposedFare"] as String
            val lat = (bid["location"] as Map<String, Any>)["lat"] as Double
            val long = (bid["location"] as Map<String, Any>)["long"] as Double
            val time = bid["time"] as Long

            val bidLatLng = LatLng(lat, long)
            val distance = Haversine.distance(userLatLng!!, bidLatLng)
            val timeAgo = System.currentTimeMillis() - time

            AlertDialog.Builder(this)
                .setTitle("$name | $proposedFare")
                .setMessage("Distance: ${distance}km\nTime: ${timeAgo / 1000}s ago\n Phone: $phone")
                .setPositiveButton("End Auction") { _, _ ->
                    acceptBid(bid)
                }
                .show()

            true
        }
    }

    private fun acceptBid(bid: Map<String, Any>) {

        db.collection("endedAuctions").document(auctionId)
            .set(mapOf("acceptedBid" to bid))
            .addOnSuccessListener {
                db.collection("sharedAuctions").document(auctionId).delete()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.auction_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_go_to_bids -> {
                val intent = Intent(this, BidActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_nearby_users -> {
                val intent = Intent(this, NearbyUsersActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
