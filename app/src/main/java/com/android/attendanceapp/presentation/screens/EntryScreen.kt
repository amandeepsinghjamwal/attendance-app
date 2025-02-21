package com.android.attendanceapp.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.android.attendanceapp.MainViewModel
import com.android.attendanceapp.presentation.navigation.NavigationScreens
import kotlinx.coroutines.launch

@Composable
fun EntryScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                if (viewModel.hasPermission(context)) {
                    navController.navigate(NavigationScreens.DetectionScreen(registerMode = true)) {
                        launchSingleTop = true
                    }
                } else {
                    viewModel.showSnackbar()
                }
            }) {
                Text(text = "Register")
            }
            Button(onClick = {
                if (viewModel.hasPermission(context)) {
                    navController.navigate(NavigationScreens.DetectionScreen(registerMode = false)) {
                        this.launchSingleTop = true
                    }
                } else {
                    viewModel.showSnackbar()
                }

            }) {
                Text(text = "Attendance Mode")
            }

            Button(onClick = {

                navController.navigate(NavigationScreens.ReportScreen) {
                    launchSingleTop = true
                }
            }

            ) {
                Text(text = "Report")
            }

            Button(onClick = {
                viewModel.importDatabase(context) { msg->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }) {
                Text(text = "Import")
            }

            Button(onClick = {
                viewModel.exportDatabase(context) { msg, url ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = msg,
                            actionLabel = if (url != null) "SHOW" else "",
                            duration = SnackbarDuration.Short
                        ).let { result ->
                            if (result == SnackbarResult.ActionPerformed) {
                                if (url != null) {
                                    openUrlInChrome(context, url)
                                }
                            }
                        }
                    }
                }
            }) {
                Text(text = "Export")
            }
        }
    }
}

fun openUrlInChrome(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.android.chrome")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(fallbackIntent)
    }
}

