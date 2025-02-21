package com.android.attendanceapp.domain
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.jtransforms.fft.DoubleFFT_2D
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

object BlurDetector {

    fun perform2DFFT(grayscaleArray: DoubleArray, width: Int, height: Int): Array<DoubleArray> {
        // Calculate the next power of two for both dimensions
        val paddedWidth = 2.0.pow(ceil(log2(width.toDouble()))).toInt()
        val paddedHeight = 2.0.pow(ceil(log2(height.toDouble()))).toInt()

        // Create a padded array
        val paddedArray = DoubleArray(paddedWidth * paddedHeight * 2) // Double the size for complex FFT

        // Copy the original grayscale data into the padded array
        for (y in 0 until height) {
            for (x in 0 until width) {
                paddedArray[y * paddedWidth * 2 + x * 2] = grayscaleArray[y * width + x]
            }
        }

        // Perform FFT on the padded array
        val fft = DoubleFFT_2D(paddedHeight.toLong(), paddedWidth.toLong())
        fft.realForward(paddedArray)

        // Reshape the result into a 2D array for easier analysis
        val transformedArray = Array(paddedHeight) { DoubleArray(paddedWidth * 2) }
        for (y in 0 until paddedHeight) {
            for (x in 0 until paddedWidth * 2) {
                transformedArray[y][x] = paddedArray[y * paddedWidth * 2 + x]
            }
        }
        return transformedArray
    }

    fun convertToGrayscale(bitmap: Bitmap): DoubleArray {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleArray = DoubleArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3.0
                grayscaleArray[y * width + x] = gray
            }
        }
        return grayscaleArray
    }

    fun detectMotionBlur(fftArray: Array<DoubleArray>, width: Int, height: Int): Boolean {
        var highFreqSum = 0.0
        var lowFreqSum = 0.0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val real = fftArray[y][x * 2]
                val imaginary = fftArray[y][x * 2 + 1]
                val magnitude = sqrt(real * real + imaginary * imaginary)

                if (x > width / 4 && y > height / 4 && x < 3 * width / 4 && y < 3 * height / 4) {
                    // Low-frequency region (center of the FFT image)
                    lowFreqSum += magnitude
                } else {
                    // High-frequency region (edges of the FFT image)
                    highFreqSum += magnitude
                }
            }
        }
        val firstDigitInt = Character.getNumericValue(highFreqSum.toString()[0])
        Log.e("Frequency sum", "$highFreqSum $lowFreqSum")
        Log.e("Frequency", "${firstDigitInt}")

        return firstDigitInt < 6
    }
}
