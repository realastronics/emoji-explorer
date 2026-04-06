package com.example.emojiexplorer20.ui.ar

import android.animation.ValueAnimator
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArCaptureFragment : Fragment(), GLSurfaceView.Renderer {

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var btnCapture: Button
    private lateinit var tvArHint: TextView
    private lateinit var tvCaptureStatus: TextView
    private lateinit var tvArPoints: TextView
    private lateinit var tvSuccessEmoji: TextView
    private lateinit var tvSuccessPoints: TextView
    private lateinit var captureSuccessOverlay: LinearLayout
    private lateinit var reticleFill: View

    private var arSession: Session? = null
    private var targetObject: EmojiObject? = null
    private var isCapturing = false
    private var captureAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isArResumed = false

    var onCaptureSuccess: ((EmojiObject) -> Unit)? = null

    companion object {
        fun newInstance(objectId: String): ArCaptureFragment {
            val fragment = ArCaptureFragment()
            val args = Bundle()
            args.putString("object_id", objectId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find target object
        val objectId = arguments?.getString("object_id")
        targetObject = SpawnConfig.SPAWN_POINTS.find { it.id == objectId }

        // Bind views
        btnCapture = view.findViewById(R.id.btn_capture)
        tvArHint = view.findViewById(R.id.tv_ar_hint)
        tvCaptureStatus = view.findViewById(R.id.tv_capture_status)
        tvArPoints = view.findViewById(R.id.tv_ar_points)
        tvSuccessEmoji = view.findViewById(R.id.tv_success_emoji)
        tvSuccessPoints = view.findViewById(R.id.tv_success_points)
        captureSuccessOverlay = view.findViewById(R.id.capture_success_overlay)
        reticleFill = view.findViewById(R.id.reticle_fill)

        // Replace ArSceneView in layout with a plain GLSurfaceView for raw ARCore
        surfaceView = view.findViewById(R.id.arSceneView) as? GLSurfaceView
            ?: createFallbackSurfaceView(view)

        targetObject?.let { obj ->
            tvArPoints.text = "${obj.rarity.points} pts"
            tvSuccessEmoji.text = obj.emoji
            tvSuccessPoints.text = "+${obj.rarity.points} pts!"
            tvArHint.text = "Find the ${obj.emoji} and hold CAPTURE!"
        }

        setupArSession()
        setupCaptureButton()

        view.findViewById<Button>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun createFallbackSurfaceView(root: View): GLSurfaceView {
        val container = root as ViewGroup
        val sv = GLSurfaceView(requireContext())
        sv.id = R.id.arSceneView
        sv.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.addView(sv, 0)
        return sv
    }

    private fun setupArSession() {
        try {
            // Check ARCore is installed and up to date
            val availability = ArCoreApk.getInstance().checkAvailability(requireContext())
            if (!availability.isSupported) {
                Toast.makeText(requireContext(), "ARCore not supported", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
                return
            }

            arSession = Session(requireContext()).also { session ->
                val config = Config(session).apply {
                    lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                session.configure(config)
            }

            // Setup GLSurfaceView
            surfaceView.apply {
                preserveEGLContextOnPause = true
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                setRenderer(this@ArCaptureFragment)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "AR setup failed: ${e.message}", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }

    // GLSurfaceView.Renderer callbacks
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        try {
            arSession?.setDisplayGeometry(
                requireActivity().windowManager.defaultDisplay.rotation,
                width, height
            )
        } catch (e: Exception) { /* ignore */ }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val session = arSession ?: return
        try {
            session.setCameraTextureName(0)
            val frame = session.update()
            val trackingState = frame.camera.trackingState

            handler.post {
                if (!isAdded) return@post
                when (trackingState) {
                    TrackingState.TRACKING -> {
                        tvArHint.text = "Find the ${targetObject?.emoji} — hold CAPTURE!"
                        btnCapture.isEnabled = true
                    }
                    TrackingState.PAUSED -> {
                        tvArHint.text = "Move phone slowly to start AR..."
                        btnCapture.isEnabled = false
                    }
                    else -> {}
                }
            }
        } catch (e: CameraNotAvailableException) {
            handler.post {
                if (isAdded) Toast.makeText(
                    requireContext(), "Camera unavailable", Toast.LENGTH_SHORT
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

    override fun onResume() {
        super.onResume()
        try {
            arSession?.resume()
            surfaceView.onResume()
            isArResumed = true
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(requireContext(), "Camera not available", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isArResumed) {
            surfaceView.onPause()
            arSession?.pause()
            isArResumed = false
        }
        cancelCapture()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        captureAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        arSession?.close()
        arSession = null
    }
}