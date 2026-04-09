package com.example.emojiexplorer20.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class RadarOverlay : Overlay() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var animRadius1 = 0f
    private var animRadius2 = 0f
    private var alpha1 = 200
    private var alpha2 = 200
    private var playerX = 0f
    private var playerY = 0f
    private var hasPosition = false

    fun updatePosition(x: Float, y: Float) {
        playerX = x
        playerY = y
        hasPosition = true
    }

    fun updateAnimation(radius1: Float, alpha1: Int, radius2: Float, alpha2: Int) {
        this.animRadius1 = radius1
        this.alpha1 = alpha1
        this.animRadius2 = radius2
        this.alpha2 = alpha2
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (!hasPosition || shadow) return

        paint.color = Color.parseColor("#00FFCC")

        paint.alpha = alpha1
        if (animRadius1 > 0) canvas.drawCircle(playerX, playerY, animRadius1, paint)

        paint.alpha = alpha2
        if (animRadius2 > 0) canvas.drawCircle(playerX, playerY, animRadius2, paint)
    }
}