package com.example.emojiexplorer20.utils

import android.location.Location
import com.example.emojiexplorer20.data.model.SpawnConfig
import kotlin.math.*

object GpsUtils {

    // --- Haversine distance (metres between two lat/lng points) ---
    fun distanceMetres(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r = 6_371_000.0 // Earth radius in metres
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // --- GPS smoothing: weighted average of last N readings ---

    const val GPS_SMOOTH_WINDOW = 5  // for smoothening

    // Most recent reading gets highest weight
    private val locationHistory = ArrayDeque<Location>(SpawnConfig.GPS_SMOOTH_WINDOW)

    fun smoothLocation(newLocation: Location): Location {
        // Reject noisy readings immediately
        if (newLocation.accuracy > SpawnConfig.GPS_ACCURACY_GATE_M) return (
                locationHistory.lastOrNull() ?: newLocation
                )

        locationHistory.addLast(newLocation)
        if (locationHistory.size > SpawnConfig.GPS_SMOOTH_WINDOW) {
            locationHistory.removeFirst()
        }

        if (locationHistory.size == 1) return newLocation

        // Weighted average — index 0 = oldest (weight 1), last = newest (highest weight)
        var totalWeight = 0.0
        var weightedLat = 0.0
        var weightedLng = 0.0

        locationHistory.forEachIndexed { index, loc ->
            val weight = (index + 1).toDouble()
            weightedLat += loc.latitude * weight
            weightedLng += loc.longitude * weight
            totalWeight += weight
        }

        val smoothed = Location(newLocation.provider)
        smoothed.latitude = weightedLat / totalWeight
        smoothed.longitude = weightedLng / totalWeight
        smoothed.accuracy = newLocation.accuracy
        smoothed.time = newLocation.time
        return smoothed
    }

    // --- Anti-cheat: detect impossible GPS jumps ---
    private var lastCheckedLocation: Location? = null
    private var lastCheckedTime: Long = 0L

    fun isLocationSuspicious(location: Location): Boolean {
        // Reject poor accuracy readings outright — this is the main jitter fix
        if (!location.hasAccuracy() || location.accuracy > 35f) return true

        val now = System.currentTimeMillis()
        val last = lastCheckedLocation

        return if (last == null) {
            lastCheckedLocation = location
            lastCheckedTime = now
            false
        } else {
            val elapsedSec = (now - lastCheckedTime) / 1000.0
            val jumpMetres = distanceMetres(
                last.latitude, last.longitude,
                location.latitude, location.longitude
            )
            val suspicious = elapsedSec < 2.0 && jumpMetres > SpawnConfig.GPS_JUMP_THRESHOLD_M
            if (!suspicious) {          // only update baseline if reading is clean
                lastCheckedLocation = location
                lastCheckedTime = now
            }
            suspicious
        }
    }

    // --- Bearing from current position to target (for directional arrow on map) ---
    fun bearingTo(
        fromLat: Double, fromLng: Double,
        toLat: Double, toLng: Double
    ): Float {
        val dLng = Math.toRadians(toLng - fromLng)
        val fromLatR = Math.toRadians(fromLat)
        val toLatR = Math.toRadians(toLat)
        val x = sin(dLng) * cos(toLatR)
        val y = cos(fromLatR) * sin(toLatR) -
                sin(fromLatR) * cos(toLatR) * cos(dLng)
        return ((Math.toDegrees(atan2(x, y)) + 360) % 360).toFloat()
    }

    // --- AR world offset: converts GPS delta to ARCore XZ metres ---
    // Call this when spawning an AR anchor relative to player position
    fun gpsToArOffset(
        playerLat: Double, playerLng: Double,
        targetLat: Double, targetLng: Double
    ): Pair<Float, Float> {
        val latMetres = distanceMetres(playerLat, playerLng, targetLat, playerLng)
        val lngMetres = distanceMetres(playerLat, playerLng, playerLat, targetLng)
        val x = if (targetLng > playerLng) lngMetres else -lngMetres
        val z = if (targetLat < playerLat) latMetres else -latMetres
        return Pair(x.toFloat(), z.toFloat())
    }

    fun clearHistory() {
        locationHistory.clear()
        lastCheckedLocation = null
        lastCheckedTime = 0L
    }
}