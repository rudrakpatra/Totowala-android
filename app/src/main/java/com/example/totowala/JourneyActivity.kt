package com.example.totowala

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.gms.common.api.Status
import com.google.firebase.Timestamp
import com.google.type.LatLng

class JourneyActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var fromLocation: LatLng? = null
    private var toLocation: LatLng? = null
    private lateinit var journey: Journey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey)

        // Initialize Google Maps
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Places API with the new API method
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext,BuildConfig.PLACES_API_KEY)
        }

        // Setup Autocomplete for "From" location
        val autocompleteFragmentFrom = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment_from) as AutocompleteSupportFragment
        autocompleteFragmentFrom.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragmentFrom.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i("JourneyActivity", "From: ${place.name}, ${place.latLng}")
                place.latLng?.let {
                    fromLocation = LatLng.newBuilder().setLatitude(it.latitude).setLongitude(it.longitude).build()
                    checkIfJourneyReady()
                }
            }

            override fun onError(status: Status) {
                Log.e("JourneyActivity", "Error selecting From location: $status")
            }
        })

        // Setup Autocomplete for "To" location
        val autocompleteFragmentTo = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment_to) as AutocompleteSupportFragment
        autocompleteFragmentTo.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragmentTo.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i("JourneyActivity", "To: ${place.name}, ${place.latLng}")
                place.latLng?.let {
                    toLocation = LatLng.newBuilder().setLatitude(it.latitude).setLongitude(it.longitude).build()
                    checkIfJourneyReady()
                }
            }

            override fun onError(status: Status) {
                Log.e("JourneyActivity", "Error selecting To location: $status")
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.maps_menu, menu)
        return true
    }

    private fun checkIfJourneyReady() {
        if (fromLocation != null && toLocation != null) {
            journey = Journey(
                from = fromLocation!!,
                to = toLocation!!,
                startedAt = Timestamp.now(),
                endedAt = null,
                fare = Fare(0.0, "", "pending", Timestamp.now())
            )
            Log.i("JourneyActivity", "Journey Created: $journey")
        }
    }
}
