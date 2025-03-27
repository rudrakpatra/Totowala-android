package com.example.totowala

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore

class LocationShareService : Service() {
    private val debugTag = "LocationShareService"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateSharedLocationStatus(true) // Mark as active
        startLocationUpdates()
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        val notificationId = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sharing Location")
            .setContentText("Your location is being shared.")
            .setSmallIcon(R.drawable.iconbg) // Replace with your icon
            .build()

        startForeground(notificationId, notification)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    shareLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun shareLocation(location: Location) {
        val user = UserManager.getUserFromLocalStorage(this)
        if (user != null) {
            val sharedLocation = SharedLocation(true,location.latitude,location.longitude, System.currentTimeMillis(), user.name, user.phone, user.type)

            FirebaseFirestore.getInstance().collection("sharedLocations")
                .document(user.phone)
                .set(sharedLocation)
                .addOnSuccessListener {
                    Log.d(debugTag, "Location uploaded successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(debugTag, "Failed to upload location", e)
                }
        }
    }
    private fun updateSharedLocationStatus(isActive: Boolean) {
        val user = UserManager.getUserFromLocalStorage(this)
        user?.let {
            val db = FirebaseFirestore.getInstance()
            db.collection("sharedLocations").document(it.phone)
                .update("active", isActive)
                .addOnSuccessListener {
                    Log.d(debugTag, "User location status updated: active = $isActive")
                }
                .addOnFailureListener { e ->
                    Log.e(debugTag, "Failed to update location status", e)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        updateSharedLocationStatus(false) // Mark as inactive
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("MyService", "App removed from recent tasks, stopping service")

        // Stop the service
        stopSelf()
    }

    data class SharedLocation(
        var active: Boolean = true,
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var timestamp: Long = 0L,
        var name: String = "",
        var phone: String = "",
        var type: String = ""
    )


    companion object {
        fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (LocationShareService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        fun addOnSharedLocationChangeListener(callback: (sharedLocation:SharedLocation) -> Unit) {
            FirebaseFirestore.getInstance().collection("sharedLocations")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("listenForNearbyUsers", "Error fetching users: ${e.message}")
                    return@addSnapshotListener
                }
                snapshots?.documents?.forEach { doc ->
                    val data = doc.toObject(LocationShareService.SharedLocation::class.java)
                    if(data !== null) callback(data)
                }
            }
        }
    }
}
