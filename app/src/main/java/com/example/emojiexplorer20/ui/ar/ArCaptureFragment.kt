package com.example.emojiexplorer20.ui.ar

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.emojiexplorer20.R
import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.SpawnConfig
import com.example.emojiexplorer20.utils.DynamicSpawnManager

class ArCaptureFragment : Fragment() {

    private lateinit var textureView: TextureView
    private lateinit var ivCanOverlay: android.widget.ImageView
    private lateinit var btnCapture: Button
    private lateinit var btnPhoto: Button
    private lateinit var tvArHint: TextView
    private lateinit var tvCaptureStatus: TextView
    private lateinit var tvScanStatus: TextView
    private lateinit var tvArPoints: TextView
    private lateinit var tvSuccessPoints: TextView
    private lateinit var captureSuccessOverlay: LinearLayout
    private lateinit var reticleFill: View
    private lateinit var scanLine: View

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val cameraHandler = Handler(Looper.getMainLooper())

    private var targetObject: EmojiObject? = null
    private var isCapturing = false
    private var captureAnimator: ValueAnimator? = null
    private var scanAnimator: ObjectAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var captureCompleted = false

    var onCaptureSuccess: ((EmojiObject) -> Unit)? = null


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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_ar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        if (targetObject == null) {
            val objectId = arguments?.getString("object_id")
            val teamId   = arguments?.getString("team_id") ?: ""
            targetObject = objectId?.let { id ->
                // Check static spawns first
                SpawnConfig.ALL_OBJECTS.firstOrNull { it.id == id }
                // Then dynamic spawns
                    ?: DynamicSpawnManager.dynamicSpawns.firstOrNull { it.id == id }
            }
        }

        textureView = view.findViewById(R.id.arSceneView)
        ivCanOverlay = view.findViewById(R.id.iv_can_overlay)
        btnCapture = view.findViewById(R.id.btn_capture)
        btnPhoto = view.findViewById(R.id.btn_photo)
        tvArHint = view.findViewById(R.id.tv_ar_hint)
        tvCaptureStatus = view.findViewById(R.id.tv_capture_status)
        tvScanStatus = view.findViewById(R.id.tv_scan_status)
        tvArPoints = view.findViewById(R.id.tv_ar_points)
        tvSuccessPoints = view.findViewById(R.id.tv_success_points)
        captureSuccessOverlay = view.findViewById(R.id.capture_success_overlay)
        reticleFill = view.findViewById(R.id.reticle_fill)
        scanLine = view.findViewById(R.id.scan_line)

        targetObject?.let { obj ->
            tvArPoints.text = "${obj.rarity.points} pts"
            tvSuccessPoints.text = "+${obj.rarity.points} pts!"
            tvArHint.text = "Red Bull ${obj.rarity.label} — hold CAPTURE!"

            // Set can image
            val drawableRes = when (obj.emoji) {
                "blue"   -> R.drawable.blue_can_redbull
                "yellow" -> R.drawable.yellow_can_redbull
                "red"    -> R.drawable.red_can_redbull
                "pink"   -> R.drawable.pink_can_redbull
                "neon"   -> R.drawable.neon_can_redbull
                else     -> R.drawable.blue_can_redbull
            }
            ivCanOverlay.setImageResource(drawableRes)

            // Success overlay — show can image too
            view.findViewById<android.widget.ImageView?>(R.id.iv_success_can)
                ?.setImageResource(drawableRes)
        }

        startEmojiFloatAnimation()
        startScanLineAnimation()
        startScanStatusBlink()

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) {
                openCamera()
            }
            override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(s: SurfaceTexture) = true
            override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
        }

        setupCaptureButton()

        btnPhoto.setOnClickListener { capturePhoto() }

        view.findViewById<Button>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    // Emoji gently bobs up and down
    private fun startEmojiFloatAnimation() {
        android.animation.ObjectAnimator.ofFloat(ivCanOverlay, "translationY", -20f, 20f).apply {
            duration = 1500
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            start()
        }
    }

    // Green scan line sweeps down the screen
    private fun startScanLineAnimation() {
        scanLine.visibility = View.VISIBLE
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        scanAnimator = ObjectAnimator.ofFloat(
            scanLine, "translationY", 0f, screenHeight
        ).apply {
            duration = 2000
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    // Blinking SCANNING text
    private fun startScanStatusBlink() {
        val blink = ObjectAnimator.ofFloat(tvScanStatus, "alpha", 1f, 0.2f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val manager = requireContext()
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
        } ?: return

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startCameraPreview(camera)
            }
            override fun onDisconnected(camera: CameraDevice) {
                camera.close(); cameraDevice = null
            }
            override fun onError(camera: CameraDevice, error: Int) {
                camera.close(); cameraDevice = null
            }
        }, cameraHandler)
    }

    private fun startCameraPreview(camera: CameraDevice) {
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(1280, 720)
        val surface = Surface(surfaceTexture)
        try {
            val previewRequest = camera
                .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                .apply { addTarget(surface) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val outputConfig = android.hardware.camera2.params.OutputConfiguration(surface)
                val sessionConfig = android.hardware.camera2.params.SessionConfiguration(
                    android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR,
                    listOf(outputConfig),
                    { r -> cameraHandler.post(r) },
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            try {
                                session.setRepeatingRequest(
                                    previewRequest.build(), null, cameraHandler
                                )
                            } catch (e: Exception) { /* ignore */ }
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    }
                )
                camera.createCaptureSession(sessionConfig)
            } else {
                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    listOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            session.setRepeatingRequest(
                                previewRequest.build(), null, cameraHandler
                            )
                        }
                        override fun onConfigureFailed(s: CameraCaptureSession) {}
                    },
                    cameraHandler
                )
            }
        } catch (e: Exception) {
            handler.post {
                if (isAdded) Toast.makeText(
                    requireContext(), "Camera error", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupCaptureButton() {
        btnCapture.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startCapture(); true }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> { cancelCapture(); true }
                else -> false
            }
        }
    }

    private fun startCapture() {
        if (isCapturing) return
        isCapturing = true
        tvCaptureStatus.text = "Hold steady..."

        // Light haptic buzz on start
        vibrate(50)

        captureAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SpawnConfig.CAPTURE_HOLD_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                reticleFill.scaleX = p
                // Progress haptic ticks
                if (p > 0.5f && p < 0.52f) vibrate(30)
                if (p > 0.8f && p < 0.82f) vibrate(30)
            }
            start()
        }
        handler.postDelayed({
            if (isCapturing) completeCapture()
        }, SpawnConfig.CAPTURE_HOLD_MS)
    }

    private fun cancelCapture() {
        if (!isCapturing) return
        isCapturing = false
        captureAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        reticleFill.scaleX = 0f
        reticleFill.scaleY = 0f
        tvCaptureStatus.text = "Hold CAPTURE to catch it!"
    }

    private fun completeCapture() {

        if (captureCompleted) return       // hard stop — can't fire twice
        captureCompleted = true

        isCapturing = false
        captureAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        val obj = targetObject ?: return

        // For dynamic spawns, double-check it hasn't been captured already
        val isDynamic = obj.id.startsWith("dyn_") || obj.id.startsWith("welcome_")
        if (isDynamic && DynamicSpawnManager.isCaptured(obj.id)) {
            Toast.makeText(requireContext(), "Already captured!", Toast.LENGTH_SHORT).show()
            handler.postDelayed({ parentFragmentManager.popBackStack() }, 1000L)
            return
        }

        obj.isCaptured = true

        // Strong success haptic pattern
        vibrate(longArrayOf(0, 100, 50, 200))

        captureSuccessOverlay.visibility = View.VISIBLE
        onCaptureSuccess?.invoke(obj)

        handler.postDelayed({
            if (isAdded) parentFragmentManager.popBackStack()
        }, 500L)
    }

    // Capture photo to gallery
    private fun capturePhoto() {
        val bitmap = textureView.bitmap ?: run {
            Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val filename = "EmojiExplorer_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EmojiExplorer")
                }
            }
            val uri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            )
            uri?.let { u ->
                requireContext().contentResolver.openOutputStream(u)?.use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                }
                vibrate(60)
                Toast.makeText(requireContext(), "Photo saved!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not save photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = requireContext()
                    .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs, VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val v = requireContext()
                    .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createOneShot(
                            durationMs, VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(durationMs)
                }
            }
        } catch (e: Exception) { /* ignore if vibrator unavailable */ }
    }
    private fun vibrate(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = requireContext()
                    .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = requireContext()
                    .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun closeCamera() {
        captureSession?.close(); captureSession = null
        cameraDevice?.close(); cameraDevice = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scanAnimator?.cancel()
        captureAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        closeCamera()
    }
}