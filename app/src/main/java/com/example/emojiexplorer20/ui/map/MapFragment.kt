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
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.emojiexplorer20.R
import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.SpawnConfig
import com.example.emojiexplorer20.data.repository.FirebaseRepository
import com.example.emojiexplorer20.ui.ar.ArCaptureFragment
import com.example.emojiexplorer20.ui.leaderboard.LeaderboardFragment
import com.example.emojiexplorer20.utils.GpsUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.core.content.ContextCompat
import android.widget.LinearLayout
import com.example.emojiexplorer20.utils.RadarOverlay
import android.widget.FrameLayout

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
    private var teamId = ""
    private var nearestObject: EmojiObject? = null
    private var lastLocation: Location? = null

    private val repository = FirebaseRepository()
    private val syncHandler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())
    private var lastSyncedScore = 0
    private var sessionTimeLeftMs = 30 * 60 * 1000L

    private val spawnMarkers = mutableListOf<Marker>()
    private var capturedEmojis = 0
    private var arrowContainer: LinearLayout? = null
    private var tvDirectionArrow: TextView? = null
    private var tvDirectionDist: TextView? = null
    private var tvCaptureStats: TextView? = null

    private var blackoutOverlay: FrameLayout? = null
    private var tvBlackoutMessage: TextView? = null
    private var tvBlackoutAttacker: TextView? = null
    private var tvBlackoutTimer: TextView? = null
    private val blackoutHandler = Handler(Looper.getMainLooper())

    private val radarOverlay = RadarOverlay()
    private var radarRadius1 = 0f
    private var radarRadius2 = 50f
    private var radarAlpha1 = 200
    private var radarAlpha2 = 200
    private val radarHandler = Handler(Looper.getMainLooper())

    companion object {
        fun newInstance(teamName: String, teamId: String): MapFragment {
            val fragment = MapFragment()
            val args = Bundle()
            args.putString("team_name", teamName)
            args.putString("team_id", teamId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        teamName = arguments?.getString("team_name") ?: "Team Alpha"
        teamId = arguments?.getString("team_id") ?: ""
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
        arrowContainer = view.findViewById(R.id.arrow_container)
        tvDirectionArrow = view.findViewById(R.id.tv_direction_arrow)
        tvDirectionDist = view.findViewById(R.id.tv_direction_dist)
        tvCaptureStats = view.findViewById(R.id.tv_capture_stats)
        blackoutOverlay = view.findViewById(R.id.blackout_overlay)
        tvBlackoutMessage = view.findViewById(R.id.tv_blackout_message)
        tvBlackoutAttacker = view.findViewById(R.id.tv_blackout_attacker)
        tvBlackoutTimer = view.findViewById(R.id.tv_blackout_timer)
        startBlackoutListener()

        tvTeamName?.text = teamName

        setupMap()
        setupSpawnMarkers()
        startLocationUpdates()
        startScoreSync()
        startSessionTimer()
        startRadarAnimation()
        updateCaptureStats()

        btnOpenAr?.setOnClickListener {
            nearestObject?.let { obj ->
                val arFragment = ArCaptureFragment.newInstance(obj.id, teamId)
                arFragment.onCaptureSuccess = { capturedObj ->
                    // Mark as captured for THIS team only
                    capturedObj.captureForTeam(teamId)
                    capturedEmojis++
                    addPoints(capturedObj.rarity.points)
                    updateCaptureStats()
                    // Object respawns for OTHER teams — no global isCaptured needed
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, arFragment)
                    .addToBackStack("ar")
                    .commit()
            }
        }
        activity?.window?.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        view.findViewById<Button>(R.id.btn_leaderboard).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    LeaderboardFragment.newInstance(teamId)
                )
                .addToBackStack("leaderboard")
                .commit()
        }
    }

    private fun updateCaptureStats() {
        val total = SpawnConfig.SPAWN_POINTS.size
        tvCaptureStats?.text = "$capturedEmojis / $total"
    }

    private fun startScoreSync() {
        syncHandler.post(object : Runnable {
            override fun run() {
                // Always refresh display
                activity?.runOnUiThread {
                    tvScore?.text = "$currentScore pts"
                }
                // Only push to Firebase if changed
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
                tvTeamName?.text = "$teamName  |  %02d:%02d".format(minutes, seconds)
                timerHandler.postDelayed(this, 1000L)
            }
        })
    }

    private fun showSessionEnd() {
        if (!isAdded) return
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Time's Up!")
            .setMessage("Final Score: $currentScore pts\n\nReturn to the booth!")
            .setCancelable(false)
            .setPositiveButton("See Leaderboard") { _, _ ->
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        LeaderboardFragment.newInstance(teamId)
                    )
                    .commit()
            }
            .show()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.overlays.add(radarOverlay)
        mapView.controller.setZoom(18.0)
        val bmlCenter = GeoPoint(28.2468, 76.8128)
        mapView.controller.setCenter(bmlCenter)
        mapView.overlayManager.tilesOverlay.setColorFilter(getF1ColorFilter())

        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()), mapView
        )
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()

        // Set custom player sprite
        val spriteBitmap = createPlayerSpriteBitmap()
        myLocationOverlay.setPersonIcon(spriteBitmap)
        myLocationOverlay.setDirectionIcon(spriteBitmap)

        mapView.overlays.add(myLocationOverlay)
    }

    private fun createPlayerSpriteBitmap(): android.graphics.Bitmap {
        val size = 80
        val bitmap = android.graphics.Bitmap.createBitmap(
            size, size, android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Outer glow ring
        paint.color = android.graphics.Color.parseColor("#4400FFCC")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Middle ring
        paint.color = android.graphics.Color.parseColor("#8800FFCC")
        canvas.drawCircle(size / 2f, size / 2f, size / 2.8f, paint)

        // Inner solid circle
        paint.color = android.graphics.Color.parseColor("#00FFCC")
        canvas.drawCircle(size / 2f, size / 2f, size / 4.5f, paint)

        // White centre dot
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 9f, paint)

        // Direction triangle on top
        val triPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        triPaint.color = android.graphics.Color.WHITE
        val path = android.graphics.Path()
        path.moveTo(size / 2f, 2f)
        path.lineTo(size / 2f - 8f, 18f)
        path.lineTo(size / 2f + 8f, 18f)
        path.close()
        canvas.drawPath(path, triPaint)

        return bitmap
    }
    private fun getF1ColorFilter(): android.graphics.ColorMatrixColorFilter {
        val matrix = android.graphics.ColorMatrix(floatArrayOf(
            1.3f, 0f,   0f,   0f,  -20f, // Red channel (boost + slight warmth)
            0f,   1.0f, 0f,   0f,  -30f, // Green channel (slightly reduced)
            0f,   0f,   0.9f, 0f,  -30f, // Blue channel (reduced for warmer tone)
            0f,   0f,   0f,   1f,   0f // Alpha
        ))
        return android.graphics.ColorMatrixColorFilter(matrix)
    }

    private fun startRadarAnimation() {
        radarHandler.post(object : Runnable {
            override fun run() {
                if (!isAdded) return

                // Animate two rings offset by half cycle
                radarRadius1 = (radarRadius1 + 3f) % 160f
                radarAlpha1 = (200 * (1f - radarRadius1 / 160f)).toInt().coerceIn(0, 200)

                radarRadius2 = (radarRadius2 + 3f) % 160f
                radarAlpha2 = (200 * (1f - radarRadius2 / 160f)).toInt().coerceIn(0, 200)

                // Get player screen position from overlay
                val playerLoc = myLocationOverlay.myLocation
                if (playerLoc != null) {
                    val point = mapView.projection.toPixels(playerLoc, null)
                    radarOverlay.updatePosition(point.x.toFloat(), point.y.toFloat())
                }

                radarOverlay.updateAnimation(radarRadius1, radarAlpha1, radarRadius2, radarAlpha2)
                mapView.invalidate()

                radarHandler.postDelayed(this, 32L) // ~30fps
            }
        })
    }
    private fun setupSpawnMarkers() {
        SpawnConfig.SPAWN_POINTS.forEach { obj ->
            if (obj.isCaptured) return@forEach
            val marker = Marker(mapView)
            marker.position = GeoPoint(obj.lat, obj.lng)
            marker.title = "${obj.emoji} ${obj.rarity.label}"
            marker.snippet = "${obj.rarity.points} pts — get within 15m"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marker.icon = createEmojiMarker(obj)
            mapView.overlays.add(marker)
            spawnMarkers.add(marker)
        }
        mapView.invalidate()
    }

    private fun updateDirectionArrow(location: Location, target: EmojiObject) {
        val bearing = GpsUtils.bearingTo(
            location.latitude, location.longitude,
            target.lat, target.lng
        )
        arrowContainer?.visibility = View.VISIBLE
        tvDirectionArrow?.rotation = bearing
        tvDirectionDist?.text = "${GpsUtils.distanceMetres(
            location.latitude, location.longitude,
            target.lat, target.lng
        ).toInt()}m"
    }

    private fun startBlackoutListener() {
        if (teamId.isEmpty()) return
        repository.listenForBlackout(teamId) { attackerName ->
            triggerBlackout(attackerName)
        }
    }

    private fun triggerBlackout(attackerName: String) {
        if (!isAdded) return
        activity?.runOnUiThread {
            tvBlackoutAttacker?.text = "by $attackerName"
            blackoutOverlay?.visibility = View.VISIBLE

            // Countdown 7 seconds
            var secondsLeft = 7
            val countdownRunner = object : Runnable {
                override fun run() {
                    if (secondsLeft <= 0) {
                        blackoutOverlay?.visibility = View.GONE
                        return
                    }
                    tvBlackoutTimer?.text = secondsLeft.toString()
                    secondsLeft--
                    blackoutHandler.postDelayed(this, 1000L)
                }
            }
            blackoutHandler.post(countdownRunner)
        }
    }

    private fun createEmojiMarker(obj: EmojiObject): android.graphics.drawable.Drawable {
        val size = when (obj.rarity) {
            EmojiObject.Rarity.COMMON   -> 80
            EmojiObject.Rarity.UNCOMMON -> 90
            EmojiObject.Rarity.RARE     -> 100
            EmojiObject.Rarity.ULTRA    -> 115
        }
        val bgColor = when (obj.rarity) {
            EmojiObject.Rarity.COMMON   -> android.graphics.Color.parseColor("#AA888780")
            EmojiObject.Rarity.UNCOMMON -> android.graphics.Color.parseColor("#AA378ADD")
            EmojiObject.Rarity.RARE     -> android.graphics.Color.parseColor("#AA7F77DD")
            EmojiObject.Rarity.ULTRA    -> android.graphics.Color.parseColor("#AAEF9F27")
        }

        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Draw circle background
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = bgColor
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw white border
        paint.color = android.graphics.Color.WHITE
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

        // Draw emoji text
        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        textPaint.textSize = size * 0.45f
        textPaint.textAlign = android.graphics.Paint.Align.CENTER
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(obj.emoji, size / 2f, textY, textPaint)

        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).setMinUpdateIntervalMillis(1000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { raw ->
                    if (GpsUtils.isLocationSuspicious(raw)) return
                    val location = GpsUtils.smoothLocation(raw)
                    lastLocation = location
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
            // Skip if THIS team already captured it
            if (obj.isCapturedByTeam(teamId)) return@forEach
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
                tvNearestHint?.text = "${closestObj?.emoji} RIGHT HERE — tap CAPTURE!"
                btnOpenAr?.visibility = View.VISIBLE
                showProximityPulse()
            }
            closestDist <= SpawnConfig.PROXIMITY_RADIUS_M -> {
                tvNearestHint?.text = "${closestObj?.emoji} ${closestDist.toInt()}m away — keep moving!"
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
        // Show direction arrow when not in capture range
        if (closestDist > SpawnConfig.AR_TRIGGER_RADIUS_M && closestObj != null) {
            lastLocation?.let { loc ->
                updateDirectionArrow(loc, closestObj!!)
            }
        } else {
            arrowContainer?.visibility = View.GONE
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
        activity?.runOnUiThread {
            tvScore?.text = "$currentScore pts"
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        syncHandler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)
        blackoutHandler.removeCallbacksAndMessages(null)
        radarHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        activity?.window?.clearFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        super.onDestroyView()
        GpsUtils.clearHistory()
    }
}