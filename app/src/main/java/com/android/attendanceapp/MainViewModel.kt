package com.android.attendanceapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.attendanceapp.database.AppDatabase
import com.android.attendanceapp.database.DriveHelper
import com.android.attendanceapp.database.EntriesEntity
import com.android.attendanceapp.database.ExportHelper
import com.android.attendanceapp.database.TodaysEntryWithUser
import com.android.attendanceapp.database.UserEntity
import com.android.attendanceapp.domain.BlurDetector
import com.android.attendanceapp.domain.Utils
import com.android.attendanceapp.recognition.RecognitionHelper
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainViewModel : ViewModel() {
    private var newEmployeeBitmap: Bitmap? = null
    private var embeddings: String? = null
    private var attendanceBitmap: Bitmap? = null
    var attendanceEmployee: UserEntity? = null
    private lateinit var recognitionHelper: RecognitionHelper
    private var isProcessing = false
    private lateinit var database: AppDatabase
    var clockedIn = false
    private lateinit var driveHelper: DriveHelper
    private var _showSnack = MutableStateFlow(false)
    val showSnack: StateFlow<Boolean> get() = _showSnack.asStateFlow()

    private var _showExportSnack = MutableStateFlow(false)
    val showExportSnack: StateFlow<Boolean> get() = _showExportSnack.asStateFlow()

    private var _todayDataList = MutableStateFlow(listOf<TodaysEntryWithUser>())
    val todayDataList: StateFlow<List<TodaysEntryWithUser>> get() = _todayDataList.asStateFlow()

    fun getNewPersonData(): Bitmap? {
        return newEmployeeBitmap
    }

    fun setRecognitionHelper(context: Context) {
        viewModelScope.launch {
            val app = context.applicationContext as AppClass
            database = app.database
            withContext(Dispatchers.Default) {
                recognitionHelper = RecognitionHelper(context)

            }
        }
    }

    fun setLifeCycleComp(context: Context) {
        driveHelper = DriveHelper(context)
    }

    fun getTodayReport(dateTime:Pair<Long,Long>) {
        CoroutineScope(Dispatchers.IO).launch {
            val dataList = database.entryDao().getTodaysEntries(dateTime.first, dateTime.second)
            withContext(Dispatchers.Main) {
                _todayDataList.value = dataList
            }
        }
    }

    fun exportDatabase(context: Context, onComplete: (msg: String, url: String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            driveHelper.launchSignIn {isSuccess, msg->
                if(isSuccess){
                    onComplete("Uploading database....",null)
                    ExportHelper.backupDatabase(
                        context,
                        "app_database",
                        driveHelper
                    ) { backupMsg, url ->
                        viewModelScope.launch {
                            onComplete(backupMsg, url)
                        }
                    }
                }
                else{
                    onComplete(msg,null)
                }
            }
        }
    }

    private suspend fun refreshEmployeeList() {
        recognitionHelper.loadEmployees()
    }

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setNewPersonData(bitmap: Bitmap, embeddings: FloatArray) {
        newEmployeeBitmap = bitmap
        this.embeddings = Json.encodeToString(embeddings)
    }

    fun recognize(faces: List<Face>, frameBitmap: Bitmap, navigate: (UserEntity) -> Unit) {
        if (isProcessing) {
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Main) {
                isProcessing = true
            }
            Log.e("test", faces.toString())
            if (::recognitionHelper.isInitialized) {
                recognitionHelper.recognize(faces, frameBitmap) { bitmap, employee ->
                    setAttendanceData(bitmap, employee)
                    CoroutineScope(Dispatchers.Main).launch {
                        navigate(employee)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                isProcessing = false
            }
        }
    }

    private fun setAttendanceData(bitmap: Bitmap?, employee: UserEntity) {
        attendanceBitmap = bitmap
        employee.bitmap = Utils.byteArrayToBitmap(employee.imageData)
        attendanceEmployee = employee
        getLastEntry(employee.userId!!)
    }

    private fun getLastEntry(userId: Long) {
        viewModelScope.launch {
            var lastEntry: Boolean
            withContext(Dispatchers.IO) {
                lastEntry = database.entryDao().getLastEntryOfUser(userId)?.isEntry ?: false
            }
            Log.e("last entry", lastEntry.toString())
            clockedIn = lastEntry
        }
    }

    fun register(faces: List<Face>, frameBitmap: Bitmap, navigate: (Boolean, String?) -> Unit) {
        if (isProcessing) {
            return
        }
        CoroutineScope(Dispatchers.Default).launch {

            withContext(Dispatchers.Main) {
                isProcessing = true
            }
            Log.e("test", faces.toString())
            if (::recognitionHelper.isInitialized) {
                val bitmapCopy = frameBitmap.copy(frameBitmap.config, true) // Create a mutable copy
                val isImageBlurred = isImageBlurred(bitmapCopy)
                Log.e("Frequ", isImageBlurred.toString())
                if (!isImageBlurred) {
                    recognitionHelper.register(faces, frameBitmap, exists = { employee ->
                        CoroutineScope(Dispatchers.Main).launch {
                            setAttendanceData(null, employee)
                            navigate(true, employee.userId.toString())
                        }
                    }) { bitmap, embeddings ->
                        setNewPersonData(bitmap, embeddings)
                        CoroutineScope(Dispatchers.Main).launch {
                            navigate(false, null)
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Log.e("Frequen", "process False")
                isProcessing = false
            }
        }
    }

    private fun isImageBlurred(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleArray = BlurDetector.convertToGrayscale(bitmap)
        val fftArray = BlurDetector.perform2DFFT(grayscaleArray, width, height)
        return BlurDetector.detectMotionBlur(fftArray, width, height)
    }


    fun registerEmployee(userEntity: UserEntity, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            database.userDao().addUser(
                userEntity.copy(
                    imageData = Utils.getByteArrayFromBitmap(newEmployeeBitmap),
                    embeddings = embeddings!!
                )
            )
            refreshEmployeeList()
            onComplete()
        }
    }

    fun clockEmployee(isEntry: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.entryDao().insertEntry(
                    EntriesEntity(
                        time = System.currentTimeMillis(),
                        userId = attendanceEmployee?.userId!!,
                        isEntry = isEntry
                    )
                )
            }
            onComplete()
        }
    }

    fun setPermissionInfo(granted: Boolean) {
        if (!granted) {
            showSnackbar()
        }
    }


    fun showSnackbar() {
        viewModelScope.launch {
            _showSnack.value = true
            delay(1000)
            _showSnack.value = false
        }

    }


    fun importDatabase(context: Context, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            driveHelper.launchSignIn {isSuccess, msg->
                if(isSuccess){
                    onComplete("Importing database...")
                    CoroutineScope(Dispatchers.IO).launch ioLaunch@{
                        var uri: Uri? = null
                        driveHelper.importLatestFile {
                            uri = it
                        }
                        if (uri == null) {
                            onComplete("Failed to download database, please make sure database exist in your drive.")
                            return@ioLaunch
                        }
                        ExportHelper.importDatabaseFromZip(
                            context,
                            "app_database",
                            uri!!
                        ) {
                            CoroutineScope(Dispatchers.IO).launch {
                                refreshEmployeeList()
                            }
                            onComplete(it)
                        }
                    }
                } else{
                  onComplete(msg)
                }
            }
        }
    }
}