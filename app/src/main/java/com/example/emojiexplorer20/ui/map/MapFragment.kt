package com.example.emojiexplorer20.ui.map

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.emojiexplorer20.R
import com.example.emojiexplorer20.TeamEntryActivity
import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.SpawnConfig
import com.example.emojiexplorer20.data.repository.FirebaseRepository
import com.example.emojiexplorer20.ui.ar.ArCaptureFragment
import com.example.emojiexplorer20.ui.leaderboard.LeaderboardFragment
import com.example.emojiexplorer20.utils.DynamicSpawnManager
import com.example.emojiexplorer20.utils.GpsUtils
import com.example.emojiexplorer20.utils.RadarOverlay
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
    private var arrowContainer: LinearLayout? = null
    private var tvDirectionArrow: TextView? = null
    private var tvDirectionDist: TextView? = null
    private var tvCaptureStats: TextView? = null
    private var tvDetectedCount: TextView? = null
    private var tvClosestDist: TextView? = null

    // Banner views
    private var bannerOverlay: FrameLayout? = null
    private var ivMapBanner: ImageView? = null
    private val bannerHandler = Handler(Looper.getMainLooper())

    private var currentScore = 0
    private var teamName = "Team Alpha"
    private var teamId = ""
    private var nearestObject: EmojiObject? = null
    private var lastLocation: Location? = null
    private var capturedEmojis = 0
    private val capturedEmojiList = mutableListOf<EmojiObject>()

    private val dynamicMarkers = mutableMapOf<String, Marker>()
    private var firstLocationReceived = false

    private val repository = FirebaseRepository()
    private val syncHandler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())
    private val radarHandler = Handler(Looper.getMainLooper())
    private var lastSyncedScore = 0

    private var sessionTimeLeftMs = 30 * 60 * 1000L
    private var timerRunning = false

    private val spawnMarkers = mutableListOf<Marker>()
    private val radarOverlay = RadarOverlay()
    private var radarRadius1 = 0f
    private var radarRadius2 = 50f
    private var radarAlpha1 = 200
    private var radarAlpha2 = 200
    private var compassModeOn = false
    private val localCapturedIds = mutableSetOf<String>()

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
    ): View? = inflater.inflate(R.layout.fragment_map, container, false)

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
        tvDetectedCount = view.findViewById(R.id.tv_detected_count)
        tvClosestDist = view.findViewById(R.id.tv_closest_dist)
        bannerOverlay = view.findViewById(R.id.banner_overlay)
        ivMapBanner = view.findViewById(R.id.iv_map_banner)

        tvTeamName?.text = teamName
        activity?.window?.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        DynamicSpawnManager.reset()

        setupMap()
        // No location yet on first create — markers will appear on first GPS fix via updateProximity
        startLocationUpdates()
        startScoreSync()
        startSessionTimer()
        startRadarAnimation()
        updateCaptureStats()

        // ── LOGOUT BUTTON ──────────────────────────────────────────────────────
        view.findViewById<Button?>(R.id.btn_logout)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Leave the game? Your score is saved.")
                .setPositiveButton("Log Out") { _, _ ->
                    requireContext()
                        .getSharedPreferences("team_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    timerRunning = false
                    syncHandler.removeCallbacksAndMessages(null)
                    timerHandler.removeCallbacksAndMessages(null)
                    radarHandler.removeCallbacksAndMessages(null)
                    val intent = Intent(requireContext(), TeamEntryActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnOpenAr?.setOnClickListener {
            nearestObject?.let { obj ->
                if (obj.isCapturedByTeam(teamId)) {
                    nearestObject = null; btnOpenAr?.visibility = View.GONE; return@setOnClickListener
                }
                if (DynamicSpawnManager.isCaptured(obj.id)) {
                    nearestObject = null; btnOpenAr?.visibility = View.GONE; return@setOnClickListener
                }
                if (localCapturedIds.contains(obj.id)) {
                    nearestObject = null; btnOpenAr?.visibility = View.GONE; return@setOnClickListener
                }

                val arFragment = ArCaptureFragment.newInstance(obj.id, teamId)
                arFragment.onCaptureSuccess = { capturedObj ->
                    capturedObj.captureForTeam(teamId)
                    localCapturedIds.add(capturedObj.id)
                    capturedEmojis++
                    capturedEmojiList.add(capturedObj)
                    addPoints(capturedObj.rarity.points)
                    updateCaptureStats()
                    nearestObject = null
                    activity?.runOnUiThread {
                        btnOpenAr?.visibility = View.GONE
                        if (capturedObj.id.startsWith("dyn_") || capturedObj.id.startsWith("welcome_")) {
                            DynamicSpawnManager.removeCaptured(capturedObj.id)
                            dynamicMarkers[capturedObj.id]?.let { mapView.overlays.remove(it) }
                            dynamicMarkers.remove(capturedObj.id)
                        } else {
                            spawnMarkers.firstOrNull { it.id == capturedObj.id }?.let {
                                mapView.overlays.remove(it); spawnMarkers.remove(it)
                            }
                        }
                        mapView.invalidate()
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, arFragment)
                    .addToBackStack("ar_capture")
                    .commit()
            }
        }

        view.findViewById<Button>(R.id.btn_leaderboard).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LeaderboardFragment.newInstance(teamId, teamName))
                .addToBackStack("leaderboard")
                .commit()
        }

        view.findViewById<View>(R.id.btn_compass)?.setOnClickListener {
            compassModeOn = !compassModeOn
            Toast.makeText(
                requireContext(),
                if (compassModeOn) "Compass mode ON" else "North-up restored",
                Toast.LENGTH_SHORT
            ).show()
            if (!compassModeOn) mapView.mapOrientation = 0f
        }

        view.findViewById<View>(R.id.btn_recenter)?.setOnClickListener {
            lastLocation?.let { loc ->
                mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                mapView.controller.setZoom(18.0)
                myLocationOverlay.enableFollowLocation()
            }
        }
    }

    // ── RED BULL BANNER — shown every time the map becomes visible ────────────
    private fun showBanner() {
        val overlay = bannerOverlay ?: return
        val img = ivMapBanner ?: return

        bannerHandler.removeCallbacksAndMessages(null)

        overlay.alpha = 1f
        img.scaleX = 1f
        img.scaleY = 1f
        overlay.visibility = View.VISIBLE

        val scaleUp   = ObjectAnimator.ofFloat(img, "scaleX", 1f, 1.08f).apply { duration = 400 }
        val scaleUpY  = ObjectAnimator.ofFloat(img, "scaleY", 1f, 1.08f).apply { duration = 400 }
        val scaleDown = ObjectAnimator.ofFloat(img, "scaleX", 1.08f, 1f).apply { duration = 400 }
        val scaleDownY= ObjectAnimator.ofFloat(img, "scaleY", 1.08f, 1f).apply { duration = 400 }

        val pulse = AnimatorSet().apply {
            play(scaleUp).with(scaleUpY)
            play(scaleDown).with(scaleDownY).after(scaleUp)
        }

        var pulseCount = 0
        fun schedulePulse() {
            if (!isAdded || overlay.visibility != View.VISIBLE) return
            pulse.start()
            pulseCount++
            if (pulseCount < 3) bannerHandler.postDelayed({ schedulePulse() }, 800L)
        }
        schedulePulse()

        bannerHandler.postDelayed({
            if (!isAdded) return@postDelayed
            ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f).apply {
                duration = 400
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        overlay.visibility = View.GONE
                        overlay.alpha = 1f
                    }
                })
                start()
            }
        }, 2000L)
    }

    // ── Dynamic spawn handling ────────────────────────────────────────────────
    private fun handleDynamicSpawns(lat: Double, lng: Double) {
        val newSpawns = DynamicSpawnManager.onLocationUpdate(lat, lng)
        if (newSpawns.isEmpty()) return
        activity?.runOnUiThread { newSpawns.forEach { addDynamicMarker(it) } }
    }

    private fun addDynamicMarker(obj: EmojiObject) {
        if (!::mapView.isInitialized) return
        if (localCapturedIds.contains(obj.id)) return
        if (DynamicSpawnManager.isCaptured(obj.id)) return

        // Only show marker if player is within visibility radius
        val loc = lastLocation ?: return
        val dist = GpsUtils.distanceMetres(loc.latitude, loc.longitude, obj.lat, obj.lng)
        if (dist > SpawnConfig.VISIBILITY_RADIUS_M) return

        dynamicMarkers[obj.id]?.let { mapView.overlays.remove(it) }
        val marker = Marker(mapView).apply {
            position = GeoPoint(obj.lat, obj.lng)
            title = "Red Bull ${obj.rarity.label}"
            snippet = "${obj.rarity.points} pts"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = createEmojiMarker(obj)
        }
        mapView.overlays.add(marker)
        dynamicMarkers[obj.id] = marker
        mapView.invalidate()
    }

    // ── Called on resume and after captures — re-evaluates which static
    //    markers should be visible given the current known location.
    //    If no location fix yet, clears stale markers and waits for GPS. ───────
    private fun refreshVisibleMarkers(location: Location? = lastLocation) {
        if (!::mapView.isInitialized) return

        // Always clear old static markers first
        spawnMarkers.forEach { mapView.overlays.remove(it) }
        spawnMarkers.clear()

        // No GPS fix yet — nothing to show, markers appear on first location update
        if (location == null) {
            mapView.invalidate()
            return
        }

        SpawnConfig.SPAWN_POINTS.forEach { obj ->
            if (obj.isCapturedByTeam(teamId) || localCapturedIds.contains(obj.id)) return@forEach
            val dist = GpsUtils.distanceMetres(location.latitude, location.longitude, obj.lat, obj.lng)
            if (dist > SpawnConfig.VISIBILITY_RADIUS_M) return@forEach

            val marker = Marker(mapView).apply {
                id = obj.id
                position = GeoPoint(obj.lat, obj.lng)
                title = "Red Bull ${obj.rarity.label}"
                snippet = "${obj.rarity.points} pts"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = createEmojiMarker(obj)
            }
            mapView.overlays.add(marker)
            spawnMarkers.add(marker)
        }
        mapView.invalidate()
    }

    private fun updateCaptureStats() {
        val staticTotal = SpawnConfig.SPAWN_POINTS.size
        val dynamicCaptured = capturedEmojiList.count {
            it.id.startsWith("dyn_") || it.id.startsWith("welcome_")
        }
        tvCaptureStats?.text = "${capturedEmojis - dynamicCaptured} / $staticTotal"
    }

    private fun startScoreSync() {
        syncHandler.post(object : Runnable {
            override fun run() {
                activity?.runOnUiThread { tvScore?.text = "$currentScore pts" }
                if (currentScore != lastSyncedScore && teamId.isNotEmpty()) {
                    lastSyncedScore = currentScore
                    lifecycleScope.launch { repository.updateScore(teamId, currentScore) }
                }
                syncHandler.postDelayed(this, SpawnConfig.LEADERBOARD_SYNC_MS)
            }
        })
    }

    private fun startSessionTimer() {
        if (timerRunning) return
        timerRunning = true
        timerHandler.post(object : Runnable {
            override fun run() {
                if (!timerRunning || !isAdded) return
                sessionTimeLeftMs -= 1000
                if (sessionTimeLeftMs <= 0) { timerRunning = false; showSessionEnd(); return }
                val m = sessionTimeLeftMs / 60000
                val s = (sessionTimeLeftMs % 60000) / 1000
                tvTeamName?.text = "$teamName  |  %02d:%02d".format(m, s)
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
                    .replace(R.id.fragment_container, LeaderboardFragment.newInstance(teamId, teamName))
                    .commit()
            }.show()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)
        mapView.controller.setCenter(GeoPoint(28.2468, 76.8128))
        mapView.overlayManager.tilesOverlay.setColorFilter(getF1ColorFilter())
        mapView.overlays.add(radarOverlay)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()

        val spriteBitmap = try {
            android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_player_custom)
                ?.let { android.graphics.Bitmap.createScaledBitmap(it, 80, 80, true) }
                ?: createPlayerSpriteBitmap()
        } catch (e: Exception) { createPlayerSpriteBitmap() }

        myLocationOverlay.setPersonIcon(spriteBitmap)
        myLocationOverlay.setDirectionIcon(createPlayerSpriteBitmap())
        mapView.overlays.add(myLocationOverlay)
    }

    private fun getF1ColorFilter(): android.graphics.ColorMatrixColorFilter {
        val matrix = android.graphics.ColorMatrix(floatArrayOf(
            0.25f, 0f, 0f, 0f, 10f,
            0f, 0.28f, 0f, 0f, 10f,
            0f, 0f, 0.40f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        ))
        return android.graphics.ColorMatrixColorFilter(matrix)
    }

    private fun createPlayerSpriteBitmap(): android.graphics.Bitmap {
        val size = 120
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        listOf("#2200FFCC", "#4400FFCC", "#6600FFCC", "#CC00FFCC").forEachIndexed { i, c ->
            paint.color = android.graphics.Color.parseColor(c)
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - i * 12f, paint)
        }
        paint.color = android.graphics.Color.WHITE
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 36f, paint)
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, 7f, paint)
        val path = android.graphics.Path().apply {
            moveTo(size / 2f, 4f); lineTo(size / 2f - 10f, 22f)
            lineTo(size / 2f, 16f); lineTo(size / 2f + 10f, 22f); close()
        }
        canvas.drawPath(path, paint)
        return bitmap
    }

    private fun startRadarAnimation() {
        radarHandler.post(object : Runnable {
            override fun run() {
                if (!isAdded) return
                radarRadius1 = (radarRadius1 + 3f) % 160f
                radarAlpha1 = (200 * (1f - radarRadius1 / 160f)).toInt().coerceIn(0, 200)
                radarRadius2 = (radarRadius2 + 3f) % 160f
                radarAlpha2 = (200 * (1f - radarRadius2 / 160f)).toInt().coerceIn(0, 200)
                myLocationOverlay.myLocation?.let {
                    val pt = mapView.projection.toPixels(it, null)
                    radarOverlay.updatePosition(pt.x.toFloat(), pt.y.toFloat())
                }
                radarOverlay.updateAnimation(radarRadius1, radarAlpha1, radarRadius2, radarAlpha2)
                mapView.invalidate()
                radarHandler.postDelayed(this, 32L)
            }
        })
    }

    private fun getCanDrawableRes(canColor: String) = when (canColor) {
        "blue"   -> R.drawable.blue_can_redbull
        "yellow" -> R.drawable.yellow_can_redbull
        "red"    -> R.drawable.red_can_redbull
        "pink"   -> R.drawable.pink_can_redbull
        "neon"   -> R.drawable.neon_can_redbull
        else     -> R.drawable.blue_can_redbull
    }

    private fun createEmojiMarker(obj: EmojiObject): android.graphics.drawable.Drawable {
        if (obj.isEmojiType()) return createEmojiTextMarker(obj.emoji)
        val canSize = 100
        return try {
            val src = android.graphics.BitmapFactory.decodeResource(resources, getCanDrawableRes(obj.emoji))
                ?: throw Exception("null bitmap")
            android.graphics.drawable.BitmapDrawable(
                resources, android.graphics.Bitmap.createScaledBitmap(src, canSize, canSize, true)
            )
        } catch (e: Exception) { createColorCircleMarker(obj.emoji, canSize) }
    }

    private fun createEmojiTextMarker(emoji: String): android.graphics.drawable.BitmapDrawable {
        val size = 90
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#CC1A1A2E")
            })
        val tp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 42f; textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(emoji, size / 2f, size / 2f - (tp.descent() + tp.ascent()) / 2f, tp)
        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    private fun createColorCircleMarker(canColor: String, size: Int): android.graphics.drawable.BitmapDrawable {
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(when (canColor) {
                    "blue"   -> "#378ADD"
                    "yellow" -> "#EF9F27"
                    "red"    -> "#E8002D"
                    "pink"   -> "#D4537E"
                    "neon"   -> "#53E0CD"
                    else     -> "#888888"
                })
            })
        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { raw ->
                    if (GpsUtils.isLocationSuspicious(raw)) return
                    val location = GpsUtils.smoothLocation(raw)
                    lastLocation = location

                    if (!firstLocationReceived) {
                        firstLocationReceived = true
                        val welcome = DynamicSpawnManager.spawnWelcomeCan(location.latitude, location.longitude)
                        activity?.runOnUiThread {
                            addDynamicMarker(welcome)
                            Toast.makeText(requireContext(), "A Red Bull appeared nearby!", Toast.LENGTH_LONG).show()
                        }
                    }

                    handleDynamicSpawns(location.latitude, location.longitude)
                    updateProximity(location)

                    if (compassModeOn && raw.hasBearing()) {
                        mapView.mapOrientation = -raw.bearing
                        view?.findViewById<TextView>(R.id.tv_compass_arrow)?.rotation = raw.bearing
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
    }

    private fun updateProximity(location: Location) {
        var closestDist = Double.MAX_VALUE
        var closestObj: EmojiObject? = null
        var detectedCount = 0

        (SpawnConfig.ALL_OBJECTS + DynamicSpawnManager.dynamicSpawns).forEach { obj ->
            if (obj.isCapturedByTeam(teamId)) return@forEach
            if (DynamicSpawnManager.isCaptured(obj.id)) return@forEach
            if (localCapturedIds.contains(obj.id)) return@forEach
            val dist = GpsUtils.distanceMetres(location.latitude, location.longitude, obj.lat, obj.lng)
            if (dist <= 100.0) detectedCount++
            if (dist < closestDist) { closestDist = dist; closestObj = obj }
        }

        nearestObject = closestObj
        tvDetectedCount?.text = "DETECTED: $detectedCount"
        tvClosestDist?.text = if (closestDist < Double.MAX_VALUE) "Closest: ${closestDist.toInt()}m" else "Closest: --"

        // Refresh which static markers are visible on this location tick
        refreshVisibleMarkers(location)

        when {
            closestDist <= SpawnConfig.AR_TRIGGER_RADIUS_M -> {
                tvNearestHint?.text = "Red Bull in range — tap CAPTURE!"
                btnOpenAr?.visibility = View.VISIBLE
                showProximityPulse()
                arrowContainer?.visibility = View.GONE
            }
            closestDist <= SpawnConfig.PROXIMITY_RADIUS_M -> {
                tvNearestHint?.text = "Can nearby — ${closestDist.toInt()}m — keep moving!"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.clearAnimation(); proximityRing?.visibility = View.GONE
                closestObj?.let { updateDirectionArrow(location, it) }
            }
            closestDist <= 100.0 -> {
                tvNearestHint?.text = "Getting warmer... ${closestDist.toInt()}m"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.clearAnimation(); proximityRing?.visibility = View.GONE
                closestObj?.let { updateDirectionArrow(location, it) }
            }
            else -> {
                tvNearestHint?.text = "Walk to find Red Bull cans"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.clearAnimation(); proximityRing?.visibility = View.GONE
                arrowContainer?.visibility = View.GONE
            }
        }
    }

    private fun updateDirectionArrow(location: Location, target: EmojiObject) {
        val bearing = GpsUtils.bearingTo(location.latitude, location.longitude, target.lat, target.lng)
        val dist = GpsUtils.distanceMetres(location.latitude, location.longitude, target.lat, target.lng).toInt()
        arrowContainer?.visibility = View.VISIBLE
        tvDirectionArrow?.rotation = bearing
        tvDirectionDist?.text = if (dist >= 1000) "${dist / 1000}.${(dist % 1000) / 100}km" else "${dist}m"
    }

    private fun showProximityPulse() {
        val ring = proximityRing ?: return
        if (ring.animation != null) return
        ring.visibility = View.VISIBLE
        ring.startAnimation(AlphaAnimation(1.0f, 0.2f).apply {
            duration = 800; repeatMode = Animation.REVERSE; repeatCount = Animation.INFINITE
        })
    }

    fun addPoints(points: Int) {
        currentScore += points
        activity?.runOnUiThread { tvScore?.text = "$currentScore pts" }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        nearestObject = null
        btnOpenAr?.visibility = View.GONE
        // Refresh markers with current location (null-safe — no-ops if no fix yet)
        refreshVisibleMarkers()
        DynamicSpawnManager.dynamicSpawns
            .filter { !localCapturedIds.contains(it.id) && !DynamicSpawnManager.isCaptured(it.id) }
            .forEach { addDynamicMarker(it) }
        if (sessionTimeLeftMs > 0) startSessionTimer()
        lastLocation?.let { updateProximity(it) }
        showBanner()
    }

    override fun onPause() {
        super.onPause()
        timerRunning = false
        mapView.onPause()
        bannerHandler.removeCallbacksAndMessages(null)
        bannerOverlay?.visibility = View.GONE
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        syncHandler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)
        radarHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroyView()
        GpsUtils.clearHistory()
        DynamicSpawnManager.reset()
        SpawnConfig.resetCaptureState()
    }
}