package com.android.attendanceapp.presentation.navigation

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.android.attendanceapp.MainViewModel
import com.android.attendanceapp.presentation.screens.AddUserScreen
import com.android.attendanceapp.presentation.screens.AttendanceScreen
import com.android.attendanceapp.presentation.screens.EntryScreen
import com.android.attendanceapp.presentation.screens.FaceDetectionScreen
import com.android.attendanceapp.presentation.screens.ReportScreen

@OptIn(ExperimentalGetImage::class)
@Composable
fun SetupNavigation(
    viewModel: MainViewModel,
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(navController = navController, startDestination = NavigationScreens.MainScreen) {
        composable<NavigationScreens.MainScreen> {
            EntryScreen(paddingValues = paddingValues, navController = navController, viewModel = viewModel)
        }
        composable<NavigationScreens.RegisterScreen>(
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500))}
        ) {
            AddUserScreen(navController = navController, viewModel = viewModel, padding = paddingValues)
        }

        composable<NavigationScreens.ReportScreen>(
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500))}
        ) {
            ReportScreen(navController = navController, viewModel = viewModel, padding = paddingValues)
        }

        composable<NavigationScreens.DetectionScreen> {backStackEntry->
            val registerMode:NavigationScreens.DetectionScreen = backStackEntry.toRoute()
            FaceDetectionScreen(
                viewModel= viewModel,
                navController = navController,
                padding = paddingValues,
                registerMode = registerMode.registerMode
            )
        }
        composable<NavigationScreens.AttendanceScreen>(
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) }
        ) {
            AttendanceScreen(viewModel = viewModel, navController = navController)
        }
    }
}