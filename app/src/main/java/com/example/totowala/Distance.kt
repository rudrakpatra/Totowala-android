package com.example.totowala

import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

object Haversine {
    fun distance(loc1: LatLng, loc2: LatLng): String {
        val R = 6371.0 // Radius of Earth in km
        val lat1 = loc1.latitude.toRadians()
        val lon1 = loc1.longitude.toRadians()
        val lat2 = loc2.latitude.toRadians()
        val lon2 = loc2.longitude.toRadians()

        val dlat = lat2 - lat1
        val dlon = lon2 - lon1

        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distance = R * c * 1000  // Convert to meters
        return if (distance > 1000) {
            "${"%.1f".format(distance / 1000)} km"
        } else {
            "${distance.toInt()} m"
        }
    }

    private fun Double.toRadians() = this * Math.PI / 180
}
