package com.android.attendanceapp.domain

data class ModelInfo(
    val name : String ,
    val assetsFilename : String ,
    val cosineThreshold : Float ,
    val outputDims : Int ,
    val inputDims : Int
)
