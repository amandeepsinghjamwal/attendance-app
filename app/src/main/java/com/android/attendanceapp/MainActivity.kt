package com.android.attendanceapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.android.attendanceapp.presentation.navigation.SetupNavigation
import com.android.attendanceapp.ui.theme.AttendanceAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel by viewModels<MainViewModel>()
        viewModel.setLifeCycleComp(this)
        enableEdgeToEdge()
        setContent {
            AttendanceAppTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    viewModel.setPermissionInfo(isGranted)
                }

                val navController = rememberNavController()
                LaunchedEffect(key1 = Unit) {
                    viewModel.setRecognitionHelper(this@MainActivity)
                    if (!viewModel.hasPermission(this@MainActivity)) {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }
                val showSnack by viewModel.showSnack.collectAsState()
                LaunchedEffect(key1 = showSnack) {
                    if (showSnack) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Application requires camera permission to function.",
                                actionLabel = "GRANT",
                                duration = SnackbarDuration.Short
                            ).let { result ->
                                if (result == SnackbarResult.ActionPerformed) {
                                    openAppSettings(this@MainActivity)
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
                    SetupNavigation(
                        viewModel = viewModel,
                        navController = navController,
                        paddingValues = innerPadding
                    )
                }
            }
        }
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}