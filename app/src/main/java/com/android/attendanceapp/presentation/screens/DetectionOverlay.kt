package com.android.attendanceapp.presentation.screens

import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.google.mlkit.vision.face.Face

@Composable
fun FaceDetectionOverlay(
    faces: List<Face>,
    previewWidth: Int,
    previewHeight: Int,
    name: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / previewWidth.toFloat()
        val scaleY = size.height / previewHeight.toFloat()

        for (face in faces) {
            val boundingBox = face.boundingBox
            val left = size.width - (boundingBox.right * scaleX)
            val top = boundingBox.top * scaleY
            val right = size.width - (boundingBox.left * scaleX)
            val bottom = boundingBox.bottom * scaleY
            val textPaint = Paint().apply {
                color = android.graphics.Color.RED
                textSize = 24f
                style = Paint.Style.FILL
            }
            val textWidth = textPaint.measureText(name)
            val textHeight = textPaint.ascent() + textPaint.descent()

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                name,
                left + (right - left) / 2 - textWidth / 2,
                top - textHeight, // Position the text above the bounding box
                textPaint
            )
        }
    }
}
