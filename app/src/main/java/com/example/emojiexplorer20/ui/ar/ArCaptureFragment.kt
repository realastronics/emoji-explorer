package com.example.emojiexplorer20.ui.ar

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.emojiexplorer20.R
import com.example.emojiexplorer20.data.model.EmojiObject
import com.example.emojiexplorer20.data.model.SpawnConfig

class ArCaptureFragment : Fragment() {

    // UI
    private lateinit var textureView: TextureView
    private lateinit var tvEmojiOverlay: TextView
    private lateinit var btnCapture: Button
    private lateinit var tvArHint: TextView
    private lateinit var tvCaptureStatus: TextView
    private lateinit var tvArPoints: TextView
    private lateinit var tvSuccessEmoji: TextView
    private lateinit var tvSuccessPoints: TextView
    private lateinit var captureSuccessOverlay: LinearLayout
    private lateinit var reticleFill: View

    // Camera
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val cameraHandler = Handler(Looper.getMainLooper())

    // Game state
    private var targetObject: EmojiObject? = null
    private var isCapturing = false
    private var captureAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        textureView = view.findViewById(R.id.arSceneView)
        tvEmojiOverlay = view.findViewById(R.id.tv_emoji_overlay)
        btnCapture = view.findViewById(R.id.btn_capture)
        tvArHint = view.findViewById(R.id.tv_ar_hint)
        tvCaptureStatus = view.findViewById(R.id.tv_capture_status)
        tvArPoints = view.findViewById(R.id.tv_ar_points)
        tvSuccessEmoji = view.findViewById(R.id.tv_success_emoji)
        tvSuccessPoints = view.findViewById(R.id.tv_success_points)
        captureSuccessOverlay = view.findViewById(R.id.capture_success_overlay)
        reticleFill = view.findViewById(R.id.reticle_fill)

        // Load target object
        val objectId = arguments?.getString("object_id")
        targetObject = SpawnConfig.SPAWN_POINTS.find { it.id == objectId }
        targetObject?.let { obj ->
            tvArPoints.text = "${obj.rarity.points} pts"
            tvSuccessEmoji.text = obj.emoji
            tvSuccessPoints.text = "+${obj.rarity.points} pts!"
            tvEmojiOverlay.text = obj.emoji
            tvArHint.text = "Find the ${obj.emoji} — hold CAPTURE!"
        }

        // Start camera when texture is ready
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture, width: Int, height: Int
            ) {
                openCamera()
            }
            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture, width: Int, height: Int
            ) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        setupCaptureButton()

        view.findViewById<Button>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val manager = requireContext()
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Find back-facing camera
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
                camera.close()
                cameraDevice = null
            }
            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
                handler.post {
                    if (isAdded) Toast.makeText(
                        requireContext(),
                        "Camera error $error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, cameraHandler)
    }

    private fun startCameraPreview(camera: CameraDevice) {
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(1280, 720)
        val surface = Surface(surfaceTexture)

        try {
            val previewRequestBuilder = camera
                .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                .apply { addTarget(surface) }

            // Use OutputConfiguration for Android 16 compatibility
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val outputConfig = android.hardware.camera2.params.OutputConfiguration(surface)
                val sessionConfig = android.hardware.camera2.params.SessionConfiguration(
                    android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR,
                    listOf(outputConfig),
                    { runnable -> cameraHandler.post(runnable) },
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            try {
                                session.setRepeatingRequest(
                                    previewRequestBuilder.build(), null, cameraHandler
                                )
                            } catch (e: Exception) {
                                handler.post {
                                    if (isAdded) Toast.makeText(
                                        requireContext(), "Preview failed", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            handler.post {
                                if (isAdded) Toast.makeText(
                                    requireContext(), "Camera config failed", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
                camera.createCaptureSession(sessionConfig)
            } else {
                // Fallback for older Android versions
                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    listOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            session.setRepeatingRequest(
                                previewRequestBuilder.build(), null, cameraHandler
                            )
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    },
                    cameraHandler
                )
            }
        } catch (e: Exception) {
            handler.post {
                if (isAdded) Toast.makeText(
                    requireContext(), "Camera error: ${e.message}", Toast.LENGTH_SHORT
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
        captureAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SpawnConfig.CAPTURE_HOLD_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                reticleFill.scaleX = p
                reticleFill.scaleY = p
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
        isCapturing = false
        captureAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        val obj = targetObject ?: return
        obj.isCaptured = true
        captureSuccessOverlay.visibility = View.VISIBLE
        onCaptureSuccess?.invoke(obj)
        handler.postDelayed({
            if (isAdded) parentFragmentManager.popBackStack()
        }, 2000L)
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        captureAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        closeCamera()
    }
}