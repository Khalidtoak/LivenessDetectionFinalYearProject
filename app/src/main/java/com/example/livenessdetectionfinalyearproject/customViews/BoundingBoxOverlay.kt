package com.example.livenessdetectionfinalyearproject.customViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.toRectF

// Defines an overlay on which the boxes and text will be drawn.
class BoundingBoxOverlay( context: Context , attributeSet: AttributeSet )
    : SurfaceView( context , attributeSet ) , SurfaceHolder.Callback {

    // Variables used to compute output2overlay transformation matrix
    // These are assigned in FrameAnalyser.kt
    var areDimsInit = false
    var frameHeight = 0
    var frameWidth = 0

    // This var is assigned in FrameAnalyser.kt
    var faceBoundingBoxes: ArrayList<Prediction>? = null

    // Determines whether or not "mask" or "no mask" should be displayed.
    var drawMaskLabel = true

    private var output2OverlayTransform: Matrix = Matrix()

    // Paint for boxes and text
    private val boxPaint = Paint().apply {
        color = Color.parseColor("#4D90caf9")
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        strokeWidth = 2.0f
        textSize = 32f
        color = Color.WHITE
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }


    override fun onDraw(canvas: Canvas?) {
        if (faceBoundingBoxes != null) {
            if (!areDimsInit) {
                val viewWidth = width.toFloat()
                val viewHeight = height.toFloat()
                val xFactor: Float = viewWidth / frameWidth.toFloat()
                val yFactor: Float = viewHeight / frameHeight.toFloat()
                // Scale and mirror the coordinates ( required for front lens )
                output2OverlayTransform.preScale(xFactor, yFactor)
                output2OverlayTransform.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
                areDimsInit = true
            }
            else {
                for (face in faceBoundingBoxes!!) {
                    val boundingBox = face.bbox.toRectF()
                    output2OverlayTransform.mapRect(boundingBox)
                    canvas?.drawRoundRect(boundingBox, 16f, 16f, boxPaint)
                    canvas?.drawText(
                        face.label,
                        boundingBox.centerX(),
                        boundingBox.centerY(),
                        textPaint
                    )
                    if ( drawMaskLabel ) {
                        canvas?.drawText(
                            face.maskLabel,
                            boundingBox.centerX(),
                            boundingBox.centerY() + 32,
                            textPaint
                        )
                    }
                }
            }
        }
    }
}
