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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import android.graphics.drawable.AnimationDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.emojiexplorer20.R
import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.PowerUpType
import com.example.emojiexplorer20.data.model.SpawnConfig
import com.example.emojiexplorer20.data.repository.FirebaseRepository
import com.example.emojiexplorer20.ui.ar.ArCaptureFragment
import com.example.emojiexplorer20.ui.leaderboard.LeaderboardFragment
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
    private var introAnim: ImageView? = null
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
    private var blackoutOverlay: FrameLayout? = null
    private var tvBlackoutAttacker: TextView? = null
    private var tvBlackoutTimer: TextView? = null

    private var currentScore = 0
    private var teamName = "Team Alpha"
    private var teamId = ""
    private var nearestObject: EmojiObject? = null
    private var lastLocation: Location? = null
    private var capturedEmojis = 0
    private var heldPowerUp: PowerUpType? = null
    private val capturedEmojiList = mutableListOf<EmojiObject>()

    private val repository = FirebaseRepository()
    private val syncHandler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())
    private val blackoutHandler = Handler(Looper.getMainLooper())
    private val radarHandler = Handler(Looper.getMainLooper())
    private var lastSyncedScore = 0

    // Timer state — persists across pause/resume
    private var sessionTimeLeftMs = 30 * 60 * 1000L
    private var timerRunning = false

    private val spawnMarkers = mutableListOf<Marker>()
    private val radarOverlay = RadarOverlay()
    private var radarRadius1 = 0f
    private var radarRadius2 = 50f
    private var radarAlpha1 = 200
    private var radarAlpha2 = 200
    private var compassModeOn = false

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

        // Bind all views
        mapView = view.findViewById(R.id.mapView)
        introAnim = view.findViewById(R.id.iv_intro_anim)
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
        blackoutOverlay = view.findViewById(R.id.blackout_overlay)
        tvBlackoutAttacker = view.findViewById(R.id.tv_blackout_attacker)
        tvBlackoutTimer = view.findViewById(R.id.tv_blackout_timer)

        tvTeamName?.text = teamName
        activity?.window?.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setupMap()
        setupSpawnMarkers()
        startLocationUpdates()
        startScoreSync()
        startSessionTimer()
        startRadarAnimation()
        startBlackoutListener()
        updateCaptureStats()

        // INTRO ANIMATION BLOCK
        introAnim?.let { imageView ->
            imageView.setBackgroundResource(R.drawable.intro_animation)
            imageView.visibility = View.VISIBLE

            val anim = imageView.background as AnimationDrawable
            anim.start()

            Handler(Looper.getMainLooper()).postDelayed({
                imageView.visibility = View.GONE
            }, 2700)
        }

        // Capture button
        btnOpenAr?.setOnClickListener {
            nearestObject?.let { obj ->
                if (obj.isCapturedByTeam(teamId)) {
                    nearestObject = null
                    btnOpenAr?.visibility = View.GONE
                    return@setOnClickListener
                }
                val arFragment = ArCaptureFragment.newInstance(obj.id, teamId)
                arFragment.onCaptureSuccess = { capturedObj ->
                    capturedObj.captureForTeam(teamId)
                    capturedEmojis++
                    capturedEmojiList.add(capturedObj)
                    addPoints(capturedObj.rarity.points)
                    updateCaptureStats()
                    nearestObject = null
                    activity?.runOnUiThread {
                        btnOpenAr?.visibility = View.GONE
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, arFragment)
                    .addToBackStack("ar")
                    .commit()
            }
        }

        // Leaderboard button
        view.findViewById<Button>(R.id.btn_leaderboard).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    LeaderboardFragment.newInstance(teamId, teamName, heldPowerUp?.name)
                )
                .addToBackStack("leaderboard")
                .commit()
        }

        // Capture stats — tap to open vault
        tvCaptureStats?.setOnClickListener { showEmojiVault() }

        // Compass button
        view.findViewById<View>(R.id.btn_compass)?.setOnClickListener {
            compassModeOn = !compassModeOn
            if (compassModeOn) {
                Toast.makeText(requireContext(), "Compass mode ON", Toast.LENGTH_SHORT).show()
            } else {
                mapView.mapOrientation = 0f
                Toast.makeText(requireContext(), "North-up restored", Toast.LENGTH_SHORT).show()
            }
        }

        // Re-center button
        view.findViewById<View>(R.id.btn_recenter)?.setOnClickListener {
            lastLocation?.let { loc ->
                mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                mapView.controller.setZoom(18.0)
                myLocationOverlay.enableFollowLocation()
            }
        }
    }

    // --- Emoji Vault ---
    private fun showEmojiVault() {
        if (capturedEmojiList.isEmpty()) {
            Toast.makeText(requireContext(), "No emojis captured yet!", Toast.LENGTH_SHORT).show()
            return
        }
        val sorted = capturedEmojiList.sortedByDescending { it.rarity.points }
        val message = buildString {
            val ultra = sorted.filter { it.rarity == EmojiObject.Rarity.ULTRA }
            val rare = sorted.filter { it.rarity == EmojiObject.Rarity.RARE }
            val uncommon = sorted.filter { it.rarity == EmojiObject.Rarity.UNCOMMON }
            val common = sorted.filter { it.rarity == EmojiObject.Rarity.COMMON }
            if (ultra.isNotEmpty()) {
                append("👑 ULTRA RARE\n")
                ultra.forEach { append("  ${it.emoji}  +${it.rarity.points}pts\n") }
                append("\n")
            }
            if (rare.isNotEmpty()) {
                append("💎 RARE\n")
                rare.forEach { append("  ${it.emoji}  +${it.rarity.points}pts\n") }
                append("\n")
            }
            if (uncommon.isNotEmpty()) {
                append("⭐ UNCOMMON\n")
                uncommon.forEach { append("  ${it.emoji}  +${it.rarity.points}pts\n") }
                append("\n")
            }
            if (common.isNotEmpty()) {
                append("🔥 COMMON\n")
                common.forEach { append("  ${it.emoji}  +${it.rarity.points}pts\n") }
            }
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Your Vault — $capturedEmojis captured")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    // --- Marker management ---
    private fun refreshSpawnMarkers() {
        if (!::mapView.isInitialized) return
        try {
            spawnMarkers.forEach { mapView.overlays.remove(it) }
            spawnMarkers.clear()
            setupSpawnMarkers()
        } catch (e: Exception) {
            // mapView not ready yet — will retry on next resume
        }
    }

    private fun updateCaptureStats() {
        val total = SpawnConfig.SPAWN_POINTS.size
        tvCaptureStats?.text = "$capturedEmojis / $total"
    }

    // --- Score sync ---
    private fun startScoreSync() {
        syncHandler.post(object : Runnable {
            override fun run() {
                activity?.runOnUiThread {
                    tvScore?.text = "$currentScore pts"
                }
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

    // --- Session timer — restartable ---
    private fun startSessionTimer() {
        if (timerRunning) return  // prevent double-starting
        timerRunning = true
        timerHandler.post(object : Runnable {
            override fun run() {
                if (!timerRunning || !isAdded) return
                sessionTimeLeftMs -= 1000
                if (sessionTimeLeftMs <= 0) {
                    timerRunning = false
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
                        LeaderboardFragment.newInstance(teamId, teamName, null)
                    )
                    .commit()
            }
            .show()
    }

    // --- Map setup ---
    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)
        val bmlCenter = GeoPoint(28.2468, 76.8128)
        mapView.controller.setCenter(bmlCenter)
        mapView.overlayManager.tilesOverlay.setColorFilter(getF1ColorFilter())
        mapView.overlays.add(radarOverlay)

        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()), mapView
        )
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        // Replace the createPlayerSpriteBitmap() call with:
        val options = android.graphics.BitmapFactory.Options().apply {
            outWidth = 80
            outHeight = 80
        }
        val spriteBitmap = android.graphics.BitmapFactory.decodeResource(
            resources, R.drawable.ic_player_custom
        )?.let {
            android.graphics.Bitmap.createScaledBitmap(it, 80, 80, true)
        } ?: createPlayerSpriteBitmap() // fallback to programmatic if image not found

        myLocationOverlay.setPersonIcon(spriteBitmap)
        myLocationOverlay.setDirectionIcon(spriteBitmap)
        myLocationOverlay.setDirectionIcon(createPlayerSpriteBitmap())
        mapView.overlays.add(myLocationOverlay)
    }

    private fun getF1ColorFilter(): android.graphics.ColorMatrixColorFilter {
        val matrix = android.graphics.ColorMatrix(floatArrayOf(
            0.25f, 0f,    0f,    0f,  10f,  // Red ↓ darker
            0f,    0.28f, 0f,    0f,  10f,  // Green ↓ muted
            0f,    0f,    0.40f, 0f,  25f,  // Blue ↑ slightly (cool tone)
            0f,    0f,    0f,    1f,   0f   // Alpha unchanged
        ))
        return android.graphics.ColorMatrixColorFilter(matrix)
    }

    private fun createPlayerSpriteBitmap(): android.graphics.Bitmap {
        val size = 80
        val bitmap = android.graphics.Bitmap.createBitmap(
            size, size, android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.parseColor("#4400FFCC")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = android.graphics.Color.parseColor("#8800FFCC")
        canvas.drawCircle(size / 2f, size / 2f, size / 2.8f, paint)
        paint.color = android.graphics.Color.parseColor("#00FFCC")
        canvas.drawCircle(size / 2f, size / 2f, size / 4.5f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 9f, paint)
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

    // --- Radar animation ---
    private fun startRadarAnimation() {
        radarHandler.post(object : Runnable {
            override fun run() {
                if (!isAdded) return
                radarRadius1 = (radarRadius1 + 3f) % 160f
                radarAlpha1 = (200 * (1f - radarRadius1 / 160f)).toInt().coerceIn(0, 200)
                radarRadius2 = (radarRadius2 + 3f) % 160f
                radarAlpha2 = (200 * (1f - radarRadius2 / 160f)).toInt().coerceIn(0, 200)
                val playerLoc = myLocationOverlay.myLocation
                if (playerLoc != null) {
                    val point = mapView.projection.toPixels(playerLoc, null)
                    radarOverlay.updatePosition(point.x.toFloat(), point.y.toFloat())
                }
                radarOverlay.updateAnimation(radarRadius1, radarAlpha1, radarRadius2, radarAlpha2)
                mapView.invalidate()
                radarHandler.postDelayed(this, 32L)
            }
        })
    }

    // --- Spawn markers ---
    private fun setupSpawnMarkers() {
        SpawnConfig.SPAWN_POINTS.forEach { obj ->
            if (obj.isCapturedByTeam(teamId)) return@forEach
            val marker = Marker(mapView)
            marker.position = GeoPoint(obj.lat, obj.lng)
            marker.title = "Red Bull ${obj.rarity.label}"
            marker.snippet = "${obj.rarity.points} pts"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = createEmojiMarker(obj)
            mapView.overlays.add(marker)
            spawnMarkers.add(marker)
        }
        SpawnConfig.POWERUP_POINTS.forEach { obj ->
            if (obj.isCapturedByTeam(teamId)) return@forEach
            val marker = Marker(mapView)
            marker.position = GeoPoint(obj.lat, obj.lng)
            marker.title = "Power-Up Can"
            marker.snippet = "Capture for a weapon!"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = createEmojiMarker(obj)
            mapView.overlays.add(marker)
            spawnMarkers.add(marker)
        }
        mapView.invalidate()
    }

    private fun getCanDrawableRes(canColor: String): Int = when (canColor) {
        "blue"   -> R.drawable.blue_can_redbull
        "yellow" -> R.drawable.yellow_can_redbull
        "red"    -> R.drawable.red_can_redbull
        "pink"   -> R.drawable.pink_can_redbull
        "neon"   -> R.drawable.neon_can_redbull
        else     -> R.drawable.blue_can_redbull
    }

    private fun createEmojiMarker(obj: EmojiObject): android.graphics.drawable.Drawable {
        // Can proportions: real 250ml can is ~66mm wide x 115mm tall = ~1:1.74 ratio
        // On screen: width varies by rarity, height = width * 1.74
        val canWidth = when (obj.rarity) {
            EmojiObject.Rarity.COMMON   -> 110
            EmojiObject.Rarity.UNCOMMON -> 110
            EmojiObject.Rarity.RARE     -> 110
            EmojiObject.Rarity.ULTRA    -> 110
        }
        val canHeight = (canWidth * 1.00f).toInt()

        return try {
            val drawableRes = getCanDrawableRes(obj.emoji)
            val sourceBitmap = android.graphics.BitmapFactory.decodeResource(
                resources, drawableRes
            ) ?: throw Exception("null bitmap")

            android.graphics.drawable.BitmapDrawable(
                resources,
                android.graphics.Bitmap.createScaledBitmap(
                    sourceBitmap, canWidth, canHeight, false
                )
            )
        } catch (e: Exception) {
            // Fallback — plain colored circle if image fails to load
            val fallback = android.graphics.Bitmap.createBitmap(
                canWidth, canWidth, android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(fallback)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.color = when (obj.emoji) {
                "blue"   -> android.graphics.Color.parseColor("#378ADD")
                "yellow" -> android.graphics.Color.parseColor("#EF9F27")
                "red"    -> android.graphics.Color.parseColor("#E8002D")
                "pink"   -> android.graphics.Color.parseColor("#D4537E")
                "neon"   -> android.graphics.Color.parseColor("#53E0CD")
                else     -> android.graphics.Color.GRAY
            }
            canvas.drawCircle(canWidth / 2f, canWidth / 2f, canWidth / 2f, paint)
            android.graphics.drawable.BitmapDrawable(resources, fallback)
        }
    }

    // --- Location updates ---
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
                    if (compassModeOn && raw.hasBearing()) {
                        mapView.mapOrientation = -raw.bearing
                        view?.findViewById<TextView>(R.id.tv_compass_arrow)
                            ?.rotation = raw.bearing
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    // --- Proximity detection ---
    private fun updateProximity(location: Location) {
        var closestDist = Double.MAX_VALUE
        var closestObj: EmojiObject? = null
        var detectedCount = 0

        SpawnConfig.SPAWN_POINTS.forEach { obj ->
            if (obj.isCapturedByTeam(teamId)) return@forEach
            val dist = GpsUtils.distanceMetres(
                location.latitude, location.longitude,
                obj.lat, obj.lng
            )
            if (dist <= 100.0) detectedCount++
            if (dist < closestDist) {
                closestDist = dist
                closestObj = obj
            }
        }

        nearestObject = closestObj

        // Update radar widget
        tvDetectedCount?.text = "DETECTED: $detectedCount"
        tvClosestDist?.text = if (closestDist < Double.MAX_VALUE) {
            "Closest: ${closestDist.toInt()}m"
        } else "Closest: --"

        when {
            closestDist <= SpawnConfig.AR_TRIGGER_RADIUS_M -> {
                tvNearestHint?.text = "${closestObj?.emoji} RIGHT HERE — tap CAPTURE!"
                btnOpenAr?.visibility = View.VISIBLE
                showProximityPulse()
                arrowContainer?.visibility = View.GONE
            }
            closestDist <= SpawnConfig.PROXIMITY_RADIUS_M -> {
                tvNearestHint?.text = "${closestObj?.emoji} ${closestDist.toInt()}m — keep moving!"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.clearAnimation()
                proximityRing?.visibility = View.GONE
                closestObj?.let { updateDirectionArrow(location, it) }
            }
            closestDist <= 100.0 -> {
                tvNearestHint?.text = "Getting warmer... ${closestDist.toInt()}m"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.clearAnimation()
                proximityRing?.visibility = View.GONE
                closestObj?.let { updateDirectionArrow(location, it) }
            }
            else -> {
                tvNearestHint?.text = "Move to find emojis"
                btnOpenAr?.visibility = View.GONE
                proximityRing?.clearAnimation()
                proximityRing?.visibility = View.GONE
                arrowContainer?.visibility = View.GONE
            }
        }
    }

    private fun updateDirectionArrow(location: Location, target: EmojiObject) {
        val bearing = GpsUtils.bearingTo(
            location.latitude, location.longitude,
            target.lat, target.lng
        )
        val dist = GpsUtils.distanceMetres(
            location.latitude, location.longitude,
            target.lat, target.lng
        ).toInt()
        arrowContainer?.visibility = View.VISIBLE
        tvDirectionArrow?.rotation = bearing
        tvDirectionDist?.text = if (dist >= 1000) {
            "${dist / 1000}.${(dist % 1000) / 100}km"
        } else "${dist}m"
    }

    private fun showProximityPulse() {
        val ring = proximityRing ?: return
        if (ring.animation != null) return  // already pulsing
        ring.visibility = View.VISIBLE
        val pulse = AlphaAnimation(1.0f, 0.2f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        ring.startAnimation(pulse)
    }

    // --- Blackout debuff ---
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

    fun addPoints(points: Int) {
        currentScore += points
        activity?.runOnUiThread {
            tvScore?.text = "$currentScore pts"
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        // Refresh markers to remove any newly captured emojis
        refreshSpawnMarkers()
        // Restart timer
        if (sessionTimeLeftMs > 0) {
            startSessionTimer()
        }
        // Force proximity refresh
        lastLocation?.let { updateProximity(it) }
    }

    override fun onPause() {
        super.onPause()
        timerRunning = false  // signal timer to stop
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