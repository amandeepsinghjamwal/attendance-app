package com.android.attendanceapp.database

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.android.attendanceapp.MainActivity
import com.android.attendanceapp.domain.Utils
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpTransport
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File


class DriveHelper(val context: Context) {
    private val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId("YOUR_SERVER_CLIENT_ID_FROM_CLOUD_CONSOLE")
        .build()
    private lateinit var driveService: Drive
    private val authorizationLauncher: ActivityResultLauncher<IntentSenderRequest> =
        (context as MainActivity).registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("TAG", "Authorization successful")
            } else {
                Log.e("TAG", "Authorization failed or was canceled")
            }
        }

    private val request: GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    private val credentialManager = CredentialManager.create(context)

    suspend fun launchSignIn(onComplete: (isSuccess:Boolean, msg: String) -> Unit) {
        if (::driveService.isInitialized){
            onComplete(true,"")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                getGoogleSignInClient(context){isSuccess, msg->
                    onComplete(isSuccess,msg)
                }
                Log.e("Auth Data", result.credential.data.toString())

            } catch (e: Exception) {
                Log.e("Auth exception", e.toString())
            }
        }

    }

    private fun getGoogleSignInClient(context: Context, onComplete: (boolean: Boolean, msg: String) -> Unit) {
        val requestedScopes = listOf(Scope(DriveScopes.DRIVE))
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    try {
                        authorizationLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent!!.intentSender).build()
                        )
                        Log.e("TAG TOKEN FIRST", authorizationResult.accessToken.toString())
                        onComplete(true,"")
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e("TAG TOKEN", "Couldn't start Authorization UI: ${e.localizedMessage}")
                        onComplete(false, "Failed to authorize, please try again later.")
                    }

                } else {
                    Log.e("TAG TOKEN", authorizationResult.accessToken.toString())
                    val httpTransport = com.google.api.client.http.javanet.NetHttpTransport()
                    val jsonFactory =
                        com.google.api.client.json.gson.GsonFactory.getDefaultInstance()

                    val requestInitializer =
                        com.google.api.client.http.HttpRequestInitializer { request ->
                            request.headers["Authorization"] =
                                "Bearer ${authorizationResult.accessToken}"
                        }

                    driveService = Drive.Builder(
                        httpTransport,
                        jsonFactory,
                        requestInitializer
                    )
                        .setApplicationName("AttendanceApp")
                        .build()
                    onComplete(true,"")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TAG", "Failed to authorize", e)
                onComplete(false, "Failed to authorize, please try again later.")

            }

    }

    fun uploadFile(fileUri: Uri, onComplete: (msg:String, url:String?)-> Unit)
    {
        CoroutineScope(Dispatchers.IO).launch {
            val folderId: String? = createFolder()
            try {
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = "database_export-${Utils.getDateTime(System.currentTimeMillis())}.zip"
                    parents = folderId?.let { listOf(it) }
                }
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val mediaContent = com.google.api.client.http.InputStreamContent(
                    context.contentResolver.getType(fileUri),
                    inputStream
                )
                val uploadRequest = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink")
                val uploadedFile = uploadRequest.execute()
                Log.d(
                    "DriveUpload", """
                                File uploaded successfully:
                                Name: ${uploadedFile.name}
                                ID: ${uploadedFile.id}
                                Link: ${uploadedFile.webViewLink}
                            """.trimIndent()
                )
                inputStream?.close()
                onComplete("File uploaded successfully", uploadedFile.webViewLink)
            } catch (e: Exception) {
                Log.e("DriveUpload", "Error uploading file", e)
                onComplete("Failed to upload file", null)
            }
        }
    }

    private suspend fun createFolder(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val existingFolder = driveService.files().list()
                    .setQ("name='Attendance app backup' and mimeType='application/vnd.google-apps.folder'")
                    .execute()

                if (existingFolder.files.isNotEmpty()) {
                    return@withContext existingFolder.files.first().id
                }
                val folderMetadata = com.google.api.services.drive.model.File().apply {
                    name = "Attendance app backup"
                    mimeType = "application/vnd.google-apps.folder"
                }

                val folder = driveService.files().create(folderMetadata)
                    .setFields("id, name, webViewLink")
                    .execute()
                Log.d(
                    "FolderCreation", """
                                Folder created successfully:
                                Name: ${folder.name}
                                ID: ${folder.id}
                                Link: ${folder.webViewLink}
                            """.trimIndent()
                )
                return@withContext folder.id

            } catch (e: Exception) {
                Log.e("FolderCreation", "Error creating folder", e)
                e.printStackTrace()
                return@withContext null
            }
        }

    }

    suspend fun importLatestFile(fileUri: (Uri) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val query =
                    "'${createFolder()}' in parents and mimeType != 'application/vnd.google-apps.folder'"
                val result = driveService.files().list()
                    .setQ(query)
                    .setOrderBy("createdTime desc")
                    .setFields("files(id, name, mimeType, createdTime)")
                    .setPageSize(1)
                    .execute()

                val files = result.files
                if (!files.isNullOrEmpty()) {
                    val latestFile = files[0]
                    Log.d(
                        "ImportFile",
                        "Latest file found: Name: ${latestFile.name}, ID: ${latestFile.id}, Created Time: ${latestFile.createdTime}"
                    )

                    downloadFile(latestFile.id, latestFile.name) {
                        fileUri(it)
                    }

                } else {
                    Log.d("ImportFile", "No files found in the folder.")
                }
            } catch (e: Exception) {
                Log.e("ImportFile", "Error importing latest file", e)
            }
        }
    }

    private suspend fun downloadFile(fileId: String, fileName: String, fileUri: (Uri) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

                val fileBytes = outputStream.toByteArray()
                outputStream.close()
                val sanitizedFileName = fileName.replace(Regex("[/:]"), "_")
                // Save the file locally
                val file = File(context.cacheDir, sanitizedFileName)
                file.writeBytes(fileBytes)
                fileUri(file.toUri())
                Log.d("DownloadFile", "File downloaded successfully: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("DownloadFile", "Error downloading file", e)
            }
        }
    }
}