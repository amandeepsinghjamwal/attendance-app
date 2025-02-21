package com.android.attendanceapp.presentation.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.android.attendanceapp.detection.FaceDetector
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    onFacesDetected: (List<Face>, Int, ImageProxy) -> Unit,
    onPreviewSizeChanged: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.cancel(true)
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val previewUseCase = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            ContextCompat.getMainExecutor(context)
                        ) { imageProxy ->
                            val format = imageProxy.format
                            Log.d("ImageFormat", "Image format: $format")
                            processImageProxy(imageProxy) { faces, rotationDegrees ->
                                onFacesDetected(faces, rotationDegrees, imageProxy) // Forward to the new callback
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        previewUseCase,
                        imageAnalysisUseCase
                    )

                    val outputSizes = imageAnalysisUseCase.resolutionInfo
                    val resolution = outputSizes?.resolution
                    Log.e("resolution",resolution.toString())

                    if (outputSizes != null) {
                        onPreviewSizeChanged(outputSizes.resolution.width, outputSizes.resolution.height)
                    }

                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        }
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onFacesDetected: (List<Face>, Int) -> Unit
) {
    val mediaImage = imageProxy.image ?: return
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

    FaceDetector.detectFaces(image) { faces ->
        onFacesDetected(faces, rotationDegrees)
        imageProxy.close()
    }
}
