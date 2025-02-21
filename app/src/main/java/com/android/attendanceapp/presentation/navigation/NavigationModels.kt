package com.android.attendanceapp.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavigationScreens {
    @Serializable
    data object MainScreen:NavigationScreens()
    @Serializable
    data object AttendanceScreen:NavigationScreens()
    @Serializable
    data object RegisterScreen:NavigationScreens()
    @Serializable
    data object ReportScreen:NavigationScreens()
    @Serializable
    data class DetectionScreen(val registerMode : Boolean):NavigationScreens()
}
