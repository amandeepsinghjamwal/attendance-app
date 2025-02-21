package com.android.attendanceapp.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import com.android.attendanceapp.database.UserEntity
import com.google.mlkit.vision.face.Face
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utils {

    fun getByteArrayFromBitmap(bitmap: Bitmap?): ByteArray {
        if (bitmap == null) {
            return ByteArray(0)
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }


    fun getDateTime(time: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val date = Date(time)
        return dateFormat.format(date).uppercase(Locale.getDefault())
    }

    fun getDate(time: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = Date(time)
        return dateFormat.format(date).uppercase(Locale.getDefault())
    }

    fun getTime(time: Long): String {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = Date(time)
        return dateFormat.format(date).uppercase(Locale.getDefault())
    }

    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun cropRectFromBitmap(source: Bitmap, rect: Rect): Bitmap {
        var width = rect.width()
        var height = rect.height()
        if ((rect.left + width) > source.width) {
            width = source.width - rect.left
        }
        if ((rect.top + height) > source.height) {
            height = source.height - rect.top
        }
        val croppedBitmap = Bitmap.createBitmap(source, rect.left, rect.top, width, height)
        return croppedBitmap
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, false)
    }

    fun yuvToBitmap(image: Image): Bitmap {
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun getROIFaces(image: Bitmap, rotation: Int, faces: List<Face>): List<Face> {
        val imageWidth = image.width
        val imageHeight = image.height

        val imageCenterX = imageWidth / 2
        val imageCenterY = imageHeight / 2

        val centeredFaces = mutableListOf<Face>()

        faces.forEach { face ->
            val boundingBox = face.boundingBox
            val faceCenterX = boundingBox.centerX()
            val faceCenterY = boundingBox.centerY()
            Log.e("distance", "${faceCenterX - imageCenterX} ${faceCenterY - imageCenterY}")
            if (kotlin.math.abs(faceCenterX - imageCenterX) <= 80 &&
                kotlin.math.abs(faceCenterY - imageCenterY) <= 40
            ) {
                centeredFaces.add(face)
            }
        }

        return centeredFaces
    }

    fun getEmptyEmployee(): UserEntity {
        return UserEntity(null, "", imageData = byteArrayOf(), embeddings = "", -1, "", "", "")
    }

    fun getDateRange(startDate: Long = System.currentTimeMillis(), endDate: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val calendar2 = Calendar.getInstance()
        calendar2.timeInMillis = endDate
        calendar2.set(Calendar.HOUR_OF_DAY, 23)
        calendar2.set(Calendar.MINUTE, 59)
        calendar2.set(Calendar.SECOND, 59)
        calendar2.set(Calendar.MILLISECOND, 999)

        Log.e("test", getDateTime(calendar.timeInMillis))
        return Pair(calendar.timeInMillis, calendar2.timeInMillis)
    }
}