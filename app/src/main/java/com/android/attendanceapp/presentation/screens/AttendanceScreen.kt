package com.android.attendanceapp.presentation.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.android.attendanceapp.MainViewModel
import com.android.attendanceapp.domain.Constants
import com.android.attendanceapp.domain.Utils
import com.android.attendanceapp.presentation.navigation.NavigationScreens
import kotlinx.coroutines.delay

@Composable
fun AttendanceScreen(viewModel: MainViewModel, navController: NavController) {
    var showDialog by remember {
        mutableStateOf(false)
    }
    BackHandler(enabled = showDialog) {
        Log.e("SOmething","Dismiss request")
        showDialog = false
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (showDialog) {
            Dialog(onDismissRequest = {
                Log.e("SOmething","Dismiss request")
                showDialog = false
            }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false, dismissOnBackPress = true)) {
                ClockCard(
                    title = if (viewModel.clockedIn) Constants.CLOCKED_OUT else Constants.CLOCKED_IN,
                    name = viewModel.attendanceEmployee?.name ?: "",
                    time = System.currentTimeMillis(),
                    viewModel.attendanceEmployee?.bitmap?.asImageBitmap()?:ImageBitmap(10,10)
                ){
                    showDialog = false
                    navController.navigate(NavigationScreens.MainScreen) {
                        launchSingleTop = true
                        popUpTo<NavigationScreens.MainScreen> { inclusive = false }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BackHandler(enabled = true) {
                navController.navigate(NavigationScreens.MainScreen) {
                    launchSingleTop = true
                    popUpTo<NavigationScreens.MainScreen> { inclusive = false }
                }
            }
            val name by remember {
                mutableStateOf("Good morning, ${viewModel.attendanceEmployee?.name}!")
            }

            Text(
                text = name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Image(
                bitmap = viewModel.attendanceEmployee?.bitmap?.asImageBitmap() ?: ImageBitmap(
                    10,
                    10
                ),
                contentScale = ContentScale.Crop,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Button(enabled = !viewModel.clockedIn, onClick = {
                    viewModel.clockEmployee(true) {
                        showDialog = true
                    }
                }) {
                    Text(text = "Clock in")
                }

                Button(enabled = viewModel.clockedIn, onClick = {
                    viewModel.clockEmployee(false) {
                        showDialog = true
                    }
                }) {
                    Text(text = "Clock out")
                }
            }
        }
    }
}

@Composable
fun ClockCard(title: String, name: String, time: Long, image : ImageBitmap, hideDialog:()->Unit) {
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
            Text(text = title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier
                .padding(top = 20.dp))
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
                Text(text = name, style = MaterialTheme.typography.headlineSmall.copy(), modifier = Modifier.padding(bottom = 5.dp))
                Text(text = Utils.getDateTime(time), style = MaterialTheme.typography.bodyLarge.copy(), modifier = Modifier.padding(bottom = 30.dp))
            }
        }
    }
}
