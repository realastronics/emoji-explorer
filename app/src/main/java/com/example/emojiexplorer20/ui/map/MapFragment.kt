package com.example.emojiexplorer20.ui.map

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import com.example.emojiexplorer20.R
import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.SpawnConfig
import com.example.emojiexplorer20.utils.GpsUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.example.emojiexplorer20.ui.ar.ArCaptureFragment
import android.widget.Button
import android.widget.TextView
import com.example.emojiexplorer20.ui.leaderboard.LeaderboardFragment
import com.example.emojiexplorer20.data.repository.FirebaseRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.emojiexplorer20.data.model.PowerUpType
import com.example.emojiexplorer20.data.model.Team

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    private var tvNearestHint: TextView? = null
    private var tvScore: TextView? = null
    private var tvTeamName: TextView? = null
    private var btnOpenAr: Button? = null
    private var proximityRing: View? = null

    private var currentScore = 0
    private var teamName = "Team Alpha"
    private var nearestObject: EmojiObject? = null
    private var lastLocation: Location? = null

    private val repository = FirebaseRepository()
    private val syncHandler = Handler(Looper.getMainLooper())
    private var lastSyncedScore = 0

    private var teamId: String = ""

    // Spawn point markers on map
    private val spawnMarkers = mutableListOf<Marker>()

    private var sessionTimeLeftMs = 30 * 60 * 1000L  // 30 minutes
    private val timerHandler = Handler(Looper.getMainLooper())

    companion object {
        fun newInstance(objectId: String, teamId: String): ArCaptureFragment {
            val fragment = ArCaptureFragment()
            val args = Bundle()
            args.putString("object_id", objectId)
            args.putString("team_id", teamId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        teamId = arguments?.getString("team_id") ?: ""
        teamName = arguments?.getString("team_name") ?: "Team Alpha"

        // Required: set OSMDroid user agent
        Configuration.getInstance().userAgentValue = requireContext().packageName
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        tvNearestHint = view.findViewById(R.id.tv_nearest_hint)
        tvScore = view.findViewById(R.id.tv_score)
        tvTeamName = view.findViewById(R.id.tv_team_name)
        btnOpenAr = view.findViewById(R.id.btn_open_ar)
        proximityRing = view.findViewById(R.id.proximity_ring)

        tvTeamName?.text = teamName

        setupMap()
        setupSpawnMarkers()
        startLocationUpdates()

        btnOpenAr?.setOnClickListener {
            nearestObject?.let { obj ->
                val arFragment = ArCaptureFragment.newInstance(obj.id, teamId)
                arFragment.onCaptureSuccess = { capturedObj ->
                    val multiplier = if (repository.isDebuffActive(
                            Team(id = teamId), PowerUpType.DOUBLE_POINTS))
                        2 else 1
                    addPoints(capturedObj.rarity.points * multiplier)
                    Handler(Looper.getMainLooper()).postDelayed({
                        capturedObj.isCaptured = false
                    }, capturedObj.respawnDelayMs)
                }
                
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, arFragment)
                    .addToBackStack("ar")
                    .commit()

            }
        }

        view.findViewById<Button>(R.id.btn_leaderboard).setOnClickListener {
            view.findViewById<Button>(R.id.btn_leaderboard).setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container,
                        LeaderboardFragment.newInstance(teamId))
                    .addToBackStack("leaderboard")
                    .commit()
            }
        }
        startScoreSync()
        startSessionTimer()
    }

    private fun startSessionTimer() {
        timerHandler.post(object : Runnable {
            override fun run() {
                sessionTimeLeftMs -= 1000
                if (sessionTimeLeftMs <= 0) {
                    showSessionEnd()
                    return
                }
                val minutes = sessionTimeLeftMs / 60000
                val seconds = (sessionTimeLeftMs % 60000) / 1000
                // Update timer in HUD
                tvTeamName?.text = "$teamName  |  %02d:%02d".format(minutes, seconds)
                timerHandler.postDelayed(this, 1000L)
            }
        })
    }

    private fun showSessionEnd() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Time's Up!")
            .setMessage("Final Score: $currentScore pts\n\nReturn to the booth!")
            .setCancelable(false)
            .setPositiveButton("See Leaderboard") { _, _ ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container,
                        LeaderboardFragment.newInstance(teamId))
                    .commit()
            }
            .show()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(19.0)

        // Start map centered on BML campus
        val bmlCenter = GeoPoint(28.9130, 76.5840)
        mapView.controller.setCenter(bmlCenter)

        // Show player location on map
        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()), mapView
        )
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        mapView.overlays.add(myLocationOverlay)
    }

    private fun setupSpawnMarkers() {
        SpawnConfig.SPAWN_POINTS.forEach { obj ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(obj.lat, obj.lng)
            marker.title = "${obj.emoji} ${obj.rarity.label} — ${obj.rarity.points}pts"
            marker.snippet = "Get closer to capture"
            // Use default marker — we'll add custom icons in next iteration
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
            spawnMarkers.add(marker)
        }
        mapView.invalidate()
    }

    private fun startScoreSync() {
        syncHandler.post(object : Runnable {
            override fun run() {
                // Only push if score actually changed
                if (currentScore != lastSyncedScore && teamId.isNotEmpty()) {
                    lastSyncedScore = currentScore
                    lifecycleScope.launch {
                        repository.updateScore(teamId, currentScore)
                    }
                }
                syncHandler.postDelayed(this, SpawnConfig.LEADERBOARD_SYNC_MS)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).setMinUpdateIntervalMillis(1000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { raw ->
                    // Anti-cheat check
                    if (GpsUtils.isLocationSuspicious(raw)) return

                    // Smooth the GPS signal
                    val location = GpsUtils.smoothLocation(raw)
                    lastLocation = location

                    // Check proximity to all spawn points
                    updateProximity(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun updateProximity(location: Location) {
        var closestDist = Double.MAX_VALUE
        var closestObj: EmojiObject? = null

        SpawnConfig.SPAWN_POINTS.forEach { obj ->
            if (obj.isCaptured) return@forEach
            val dist = GpsUtils.distanceMetres(
                location.latitude, location.longitude,
                obj.lat, obj.lng
            )
            if (dist < closestDist) {
                closestDist = dist
                closestObj = obj
            }
        }

        nearestObject = closestObj

        when {
            closestDist <= SpawnConfig.AR_TRIGGER_RADIUS_M -> {
                // In capture range
                tvNearestHint?.text = "${closestObj?.emoji} RIGHT HERE — tap CAPTURE!"
                btnOpenAr?.visibility = View.VISIBLE
                showProximityPulse()
            }
            closestDist <= SpawnConfig.PROXIMITY_RADIUS_M -> {
                // Close — show hint
                val dist = closestDist.toInt()
                tvNearestHint?.text = "${closestObj?.emoji} ${dist}m away — keep moving!"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.visibility = View.GONE
            }
            closestDist <= 50.0 -> {
                tvNearestHint?.text = "Getting warmer... ${closestDist.toInt()}m"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.visibility = View.GONE
            }
            else -> {
                tvNearestHint?.text = "Move to find emojis"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.visibility = View.GONE
            }
        }
    }

    private fun showProximityPulse() {
        val ring = proximityRing ?: return
        ring.visibility = View.VISIBLE
        val pulse = AlphaAnimation(1.0f, 0.2f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        ring.startAnimation(pulse)
    }

    fun addPoints(points: Int) {
        currentScore += points
        tvScore?.text = "$currentScore pts"
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        syncHandler.removeCallbacksAndMessages(null)  // add this line
        timerHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        GpsUtils.clearHistory()
    }
}