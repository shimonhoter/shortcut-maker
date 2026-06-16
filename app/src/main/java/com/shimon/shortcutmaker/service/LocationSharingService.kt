package com.shimon.shortcutmaker.service

// [START: LocationService]

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationSharingService : Service() {

    companion object {
        const val TAG                 = "LocationSharingService"
        const val EXTRA_PHONE         = "extra_phone"
        const val EXTRA_CONTACT_NAME  = "extra_contact_name"
        const val CHANNEL_ID          = "location_sharing_channel"
        const val NOTIF_ID            = 1001
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phone       = intent?.getStringExtra(EXTRA_PHONE) ?: ""
        val contactName = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: "איש קשר"

        startForeground(NOTIF_ID, buildNotification("מאתר מיקום עבור $contactName..."))

        if (!hasLocationPermission()) {
            Log.e(TAG, "Missing location permission")
            stopSelf(); return START_NOT_STICKY
        }

        scope.launch {
            fetchAndShare(phone, contactName)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun fetchAndShare(phone: String, contactName: String) {
        withContext(Dispatchers.Main) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val lat = location.latitude
                            val lng = location.longitude
                            Log.d(TAG, "Got location: $lat, $lng")
                            shareLocationWhatsApp(phone, lat, lng)
                        } else {
                            Log.w(TAG, "Last location is null, requesting fresh location")
                            requestFreshLocation(phone)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Location failed: ${e.message}")
                        scope.launch { stopSelf() }
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception: ${e.message}")
                stopSelf()
            }
        }
    }

    private fun requestFreshLocation(phone: String) {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMaxUpdates(1).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val loc = result.lastLocation ?: return
                shareLocationWhatsApp(phone, loc.latitude, loc.longitude)
                scope.launch { stopSelf() }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request, callback, mainLooper
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Fresh location failed: ${e.message}")
            stopSelf()
        }
    }

    /**
     * Shares a Google Maps link to the given coordinates via WhatsApp.
     */
    private fun shareLocationWhatsApp(phone: String, lat: Double, lng: Double) {
        val mapsLink = "https://maps.google.com/?q=$lat,$lng"
        val message  = "המיקום הנוכחי שלי: $mapsLink"

        val cleanPhone = phone.trimStart('+').replace(" ", "").replace("-", "")
        val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedMsg")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
            Log.d(TAG, "Opened WhatsApp for location sharing")
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp not available: ${e.message}")
        }
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "שיתוף מיקום",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "שירות שיתוף המיקום האוטומטי"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ShortcutMaker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

// [END: LocationService]
