package com.android.attendanceapp.presentation.screens

import android.content.res.Configuration
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.android.attendanceapp.MainViewModel
import com.android.attendanceapp.domain.Utils
import com.android.attendanceapp.domain.Utils.yuvToBitmap
import com.android.attendanceapp.presentation.navigation.NavigationScreens
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.delay

@ExperimentalGetImage
@Composable
fun FaceDetectionScreen(
    viewModel: MainViewModel,
    navController: NavController,
    padding: PaddingValues,
    registerMode: Boolean
) {
    var detectedFaces by remember { mutableStateOf(emptyList<Face>()) }
    var previewSize by remember { mutableStateOf(Size.Zero) }
    var rotationDeg by remember { mutableIntStateOf(0) }
    var isCompleted by remember {
        mutableStateOf(false)
    }
    var existingPersonList by remember {
        mutableStateOf(listOf<String>())
    }

    var showDialog by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.padding(padding)
    ) {
        if (showDialog) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                AlreadyExistCard(
                    name = viewModel.attendanceEmployee?.name.toString(),
                    time = System.currentTimeMillis(),
                    image = viewModel.attendanceEmployee?.bitmap?.asImageBitmap() ?: ImageBitmap(
                        10,
                        10
                    )
                ) {
                    showDialog = false
                }
            }
        }

        CameraPreview(
            lifecycleOwner = LocalLifecycleOwner.current,
            onFacesDetected = { faces, rotation, image ->
                detectedFaces = faces
                rotationDeg = rotation

                var frameBitmap = yuvToBitmap(image.image!!)
                frameBitmap =
                    Utils.rotateBitmap(frameBitmap, image.imageInfo.rotationDegrees.toFloat())

                if (!isCompleted && !registerMode && faces.isNotEmpty()) {
                    val faceList = listOf(faces[0])
                    viewModel.recognize(faceList, frameBitmap) {
                        Log.e("Width height", "${frameBitmap.width} ${frameBitmap.height}")
                        isCompleted = true
                        navController.navigate(NavigationScreens.AttendanceScreen) {
                            launchSingleTop = true
                            popUpTo<NavigationScreens.MainScreen> { inclusive = false }
                        }
                    }
                }
                if (!isCompleted && registerMode && !showDialog) {
                    val roiFaces = Utils.getROIFaces(frameBitmap, rotationDeg, faces)
                    viewModel.register(roiFaces, frameBitmap) { exists, id ->
                        if (!exists) {
                            isCompleted = true
                            navController.navigate(NavigationScreens.RegisterScreen) {
                                launchSingleTop = true
                                popUpTo<NavigationScreens.MainScreen> { inclusive = false }
                            }
                        } else {
                            if (!existingPersonList.contains(id.toString())) {
                                showDialog = true
                                existingPersonList += id!!
                            }

                        }
                    }
                }
            },
            onPreviewSizeChanged = { width, height ->
                previewSize = Size(width.toFloat(), height.toFloat())
            }
        )

//        FaceDetectionOverlay(
//            faces = detectedFaces,
//            previewWidth = previewSize.width.toInt(),
//            previewHeight = previewSize.height.toInt(),
//            name = name
//        )
    }
}

@Composable
fun AlreadyExistCard(name: String, time: Long, image: ImageBitmap, hideDialog: () -> Unit) {
    val duration = 2
    var progress by remember { mutableFloatStateOf(1f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing), label = ""
    )
    LaunchedEffect(Unit) {
        for (i in duration downTo 1) {
            progress -= 0.50f
            delay(1000)
        }
        hideDialog()
    }


    ElevatedCard(
        modifier = Modifier.cardModifier(),
        onClick = {},
        shape = RoundedCornerShape(5)
    ) {
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = CardDefaults.elevatedCardColors().containerColor
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Already Exist",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .padding(top = 20.dp)
            )
            Image(
                bitmap = image,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(),
                    modifier = Modifier.padding(bottom = 5.dp)
                )
                Text(
                    text = "This employee already exist.",
                    style = MaterialTheme.typography.bodyLarge.copy(),
                    modifier = Modifier.padding(bottom = 30.dp)
                )
            }

        }
    }
}

@Composable
fun Modifier.cardModifier(): Modifier {
    val configuration = LocalConfiguration.current
    return this.then(
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.5f)
        } else {
            Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(0.9f)
        }
    )
}


