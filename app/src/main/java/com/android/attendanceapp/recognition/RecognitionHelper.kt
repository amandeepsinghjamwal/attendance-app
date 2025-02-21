package com.android.attendanceapp.recognition

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.android.attendanceapp.AppClass
import com.android.attendanceapp.database.UserEntity
import com.android.attendanceapp.domain.ModelInfo
import com.android.attendanceapp.domain.Utils
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import kotlin.math.abs
import kotlin.math.sqrt

class RecognitionHelper(private val context: Context) {
    private var application: AppClass = context.applicationContext as AppClass
    private var employeeList = mutableListOf<UserEntity>()
    private var embeddingCount = 0

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadEmployees()
        }
    }

    private val model = ModelInfo(
        "FaceNet",
        "facenet.tflite",
        0.7f,
        128,
        160
    )
    private var recognizer = Recognizer(
        context = context,
        model = model,
        useGpu = false,
        useXNNPack = true
    )

    private val nameScoreHashmap: HashMap<String, ArrayList<Float>> = hashMapOf()

    suspend fun recognize(
        faces: List<Face>,
        cameraFrameBitmap: Bitmap,
        result: (Bitmap, UserEntity) -> Unit
    ) {
        withContext(Dispatchers.Default) {
            faces.forEach { face ->
                Log.e("Angles", "${abs(face.headEulerAngleX)} ${abs(face.headEulerAngleY)}")

                val croppedBitmap : Bitmap
                try{
                    croppedBitmap = Utils.cropRectFromBitmap(cameraFrameBitmap, face.boundingBox)
                }catch (e:Exception){
                    return@forEach
                }
                val embedding = recognizer.getFaceEmbedding(croppedBitmap)
                saveEmbeddingAndBitmap(context,embedding)


                for (i in 0 until employeeList.size) {
                    if (nameScoreHashmap[employeeList[i].userId!!.toString()] == null) {
                        val p = ArrayList<Float>()
                        p.add(
                            cosineSimilarity(
                                embedding,
                                Json.decodeFromString<FloatArray>(employeeList[i].embeddings)
                            )
                        )
                        nameScoreHashmap[employeeList[i].userId!!.toString()] = p
                    } else {
                        nameScoreHashmap[employeeList[i].userId!!.toString()]?.add(
                            cosineSimilarity(
                                embedding,
                                Json.decodeFromString<FloatArray>(employeeList[i].embeddings)
                            )
                        )
                    }
                }

                val avgScores =
                    nameScoreHashmap.values.map { scores -> scores.toFloatArray().average() }
                nameScoreHashmap.clear()

                if ((avgScores.maxOrNull() ?: 0.0) > model.cosineThreshold) {
                    Log.e("avgScore", avgScores.maxOrNull().toString())
                    result(
                        cameraFrameBitmap,
                        employeeList[avgScores.indexOf(avgScores.maxOrNull()!!)]
                    )
                    employeeList[avgScores.indexOf(avgScores.maxOrNull()!!)].name
                    Log.e("sub", embedding.contentToString())
                }
            }
        }
    }

    fun saveEmbeddingAndBitmap(
        context: Context,
        embedding: FloatArray
    ) {
        embeddingCount+=1
        if(embeddingCount>50){
            Log.e("FileSave", "Embeddings full")
            return
        }
        try {
            val embeddingFile = File(context.filesDir, "embedding.txt")
            if (!embeddingFile.exists()) {
                embeddingFile.createNewFile()
            }
            FileWriter(embeddingFile,true).use { writer ->
                writer.append(embedding.joinToString(
                    prefix = "[", postfix = "]", separator = ", ") { it.toString() }
                )
                writer.appendLine()
                writer.appendLine()
            }
            Log.d("FileSave", "Embedding saved at: ${embeddingFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("FileSave", "Error saving files", e)
        }
    }


    suspend fun register(
        faces: List<Face>,
        cameraFrameBitmap: Bitmap,
        exists: (UserEntity)-> Unit,
        name: (Bitmap, FloatArray) -> Unit
    ) {

        withContext(Dispatchers.Default) {
            faces.forEach { face ->
                Log.e("Angles", "${abs(face.headEulerAngleX)} ${abs(face.headEulerAngleY)}")
                if (abs(face.headEulerAngleX) > 10 || abs(face.headEulerAngleY) > 10 || abs(face.headEulerAngleZ) > 10) {
                    return@forEach
                }
                val croppedBitmap : Bitmap
                try{
                    croppedBitmap = Utils.cropRectFromBitmap(cameraFrameBitmap, face.boundingBox)
                }catch (e:Exception){
                    return@forEach
                }
                val sub = recognizer.getFaceEmbedding(croppedBitmap)

                for (i in 0 until employeeList.size) {
                    if (nameScoreHashmap[employeeList[i].userId!!.toString()] == null) {
                        val p = ArrayList<Float>()
                        p.add(cosineSimilarity(sub,
                            Json.decodeFromString<FloatArray>(employeeList[i].embeddings)
                        ))
                        nameScoreHashmap[employeeList[i].userId!!.toString()] = p
                    } else {
                        nameScoreHashmap[employeeList[i].userId!!.toString()]?.add(
                            cosineSimilarity(
                                sub,
                                Json.decodeFromString<FloatArray>(employeeList[i].embeddings)
                            )
                        )
                    }
                }

                val avgScores =
                    nameScoreHashmap.values.map { scores -> scores.toFloatArray().average() }
                nameScoreHashmap.clear()
                if ((avgScores.maxOrNull() ?: 0.0) > model.cosineThreshold) {
                    Log.e("avgScore", avgScores.maxOrNull().toString())
                    exists(employeeList[avgScores.indexOf(avgScores.maxOrNull()!!)])
                } else {
                    name(cameraFrameBitmap, sub)
                }
                Log.e("sub", sub.contentToString())
            }
        }
    }

    private fun cosineSimilarity(x1: FloatArray, x2: FloatArray): Float {
        val mag1 = sqrt(x1.map { it * it }.sum())
        val mag2 = sqrt(x2.map { it * it }.sum())
        val dot = x1.mapIndexed { i, xi -> xi * x2[i] }.sum()
        return dot / (mag1 * mag2)
    }

    suspend fun loadEmployees() {
        employeeList = application.database.userDao().getAllUsers().toMutableList()
    }
}