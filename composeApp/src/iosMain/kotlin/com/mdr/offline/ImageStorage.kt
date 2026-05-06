package com.mdr.offline

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

actual class ImageStorage {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getImageBitmapFromUrl(url: String): ImageBitmap {
        val image = NSData.dataWithContentsOfURL(NSURL(string = url)) ?: throw IllegalArgumentException("Failed to download image from URL: $url")
        val bytes = ByteArray(image.length.toInt())

        bytes.usePinned { pinnedBytes ->
            image.getBytes(pinnedBytes.addressOf(0), image.length)
        }
        return Bitmap.makeFromImage(Image.makeFromEncoded(bytes)).asComposeImageBitmap()
    }
    actual fun saveImage(url: String, fileName: String) {
        val urlObj = NSURL(string = url)
        val data = NSData.dataWithContentsOfURL(urlObj)  // Synchronous data fetch
            ?: throw IllegalArgumentException("Failed to download image from URL: $url")

        val documentsDirectory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0]
        val filePath = "$documentsDirectory/$fileName"
        val fileURL = NSURL(fileURLWithPath = filePath)

        val success = data.writeToURL(fileURL, atomically = true)
        if (!success) {
            throw IllegalArgumentException("Failed to save image to file: $fileName")
        }

    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun getImage(fileName: String): ByteArray {
        val documentsDirectory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0]
        val filePath = "$documentsDirectory/$fileName"
        val fileURL = NSURL(fileURLWithPath = filePath)
        val data = NSData.dataWithContentsOfURL(fileURL)

        if (data != null) {
            val bytes = ByteArray(data.length.toInt())
            // Use a pointer to the allocated ByteArray
            bytes.usePinned { pinnedBytes ->
                data.getBytes(pinnedBytes.addressOf(0), data.length)
            }
            return bytes
        } else {
            throw IllegalArgumentException("Image file not found: $fileName")
        }
    }

    actual fun checkIfImageExists(fileName: String): Boolean {
        val documentsDirectory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0]
        val filePath = "$documentsDirectory/$fileName"
        val fileURL = NSURL(fileURLWithPath = filePath)
        val data = NSData.dataWithContentsOfURL(fileURL)

        return data != null
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun deleteImage(fileName: String): Boolean {
        val documentsDirectory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0]
        val filePath = "$documentsDirectory/$fileName"
        val fileURL = NSURL(fileURLWithPath = filePath)

        val fileManager = NSFileManager.defaultManager
        return fileManager.removeItemAtURL(fileURL, null)

    }

    actual fun convertToImageBitmap(image: ByteArray): ImageBitmap {
        return Bitmap.makeFromImage(Image.makeFromEncoded(image)).asComposeImageBitmap()
    }

}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }

}