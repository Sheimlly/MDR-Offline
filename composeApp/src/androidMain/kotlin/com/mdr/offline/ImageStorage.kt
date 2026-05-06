package com.mdr.offline

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.BitmapFactory
import android.os.StrictMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ImageStorage {
    private fun sanitizeFilename(fileName: String): String {
        return fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
    }

    actual suspend fun getImageBitmapFromUrl(url: String): ImageBitmap = withContext(Dispatchers.IO) {
        val image = URL(url).readBytes()
        BitmapFactory.decodeByteArray(image, 0, image.size).asImageBitmap()
    }

    actual fun saveImage(url: String, fileName: String) {
        // Create URL object
        val imageUrl = URL(url)
        val connection = imageUrl.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()

        // Get the input stream
        val input: InputStream = connection.inputStream

        // Sanitize the filename
//        val sanitizedFilename = sanitizeFilename(fileName)
        val file = File(applicationContext.filesDir, fileName)

        // Ensure the directory exists
        file.parentFile?.mkdirs()

        // Write to the file
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }

    }


    actual fun getImage(fileName: String): ByteArray {
//        return File(applicationContext.filesDir, fileName).readBytes()
        val sanitizedFilename = sanitizeFilename(fileName)
        val file = File(applicationContext.filesDir, fileName)
        return if (file.exists()) {
            file.readBytes()
        } else {
            throw IllegalArgumentException("Image file not found: $fileName")
        }
    }

    actual fun checkIfImageExists(fileName: String): Boolean {
        val file = File(applicationContext.filesDir, fileName)
        return file.exists()
    }

    actual fun deleteImage(fileName: String): Boolean {
        val sanitizedFilename = sanitizeFilename(fileName)
        val file = File(applicationContext.filesDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    actual fun convertToImageBitmap(image: ByteArray): ImageBitmap {
        return BitmapFactory.decodeByteArray(image, 0, image.size).asImageBitmap()
    }

}