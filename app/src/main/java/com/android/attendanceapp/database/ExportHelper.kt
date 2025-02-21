package com.android.attendanceapp.database

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ExportHelper {
    private const val ZIP_FILE_NAME = "database_export.zip"

    fun backupDatabase(
        context: Context,
        dbName: String,
        driveHelper: DriveHelper,
        onComplete: (message: String, url:String?) -> Unit
    ) {
        val dbPath = context.getDatabasePath(dbName).absolutePath
        val dbFile = File(dbPath)
        val walFile = File("$dbPath-wal")
        val shmFile = File("$dbPath-shm")

        try {
            if (!dbFile.exists()) {
                println("Database file not found at $dbPath")
                onComplete("Something went wrong, please restart app.", null)
                return
            }

            val exportFiles = listOf(dbFile, walFile, shmFile)
            val zipFile = File(context.cacheDir, ZIP_FILE_NAME)

            // Create the ZIP file
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                exportFiles.forEach { file ->
                    if (file.exists()) {
                        FileInputStream(file).use { fis ->
                            val entry = ZipEntry(file.name)
                            zipOut.putNextEntry(entry)
                            fis.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            driveHelper.uploadFile(zipFile.toUri()) { msg, url ->
                onComplete(msg, url)
            }
            println("Database exported as $ZIP_FILE_NAME")
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete("Failed to export database", null)
            println("Failed to export database: ${e.message}")
        }
    }

    suspend fun importDatabaseFromZip(
        context: Context,
        dbName: String,
        zipUri: Uri,
        onComplete: (message: String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val dbPath = context.getDatabasePath(dbName).absolutePath
                val walPath = "$dbPath-wal"
                val shmPath = "$dbPath-shm"
                val importPaths = listOf(dbPath, walPath, shmPath)

                val tempDir = File(context.cacheDir, "temp_db_import")
                if (!tempDir.exists()) tempDir.mkdirs()

                val filesMap = mutableMapOf<String, File>()
                context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry: ZipEntry?
                        while (zis.nextEntry.also { entry = it } != null) {
                            val fileName = entry!!.name
                            val outputFile = File(tempDir, fileName)
                            FileOutputStream(outputFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            filesMap[fileName] = outputFile
                        }
                    }
                }

                if (filesMap.size != 3) {
                    println("Error: Expected 3 files in the ZIP but got ${filesMap.size}")
                    onComplete("Incorrect file")
                    return@withContext
                }

                importPaths.forEachIndexed { _, path ->
                    val file = filesMap[File(path).name]
                    file?.let {
                        FileInputStream(it).use { input ->
                            FileOutputStream(File(path)).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
                onComplete("Database imported successfully!")
                println("Database imported successfully!")
                return@withContext
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete("Failed to import data")
                println("Failed to import database: ${e.message}")
                return@withContext
            }
        }
    }
}
