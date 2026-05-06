package com.mdr.offline

import androidx.compose.ui.graphics.ImageBitmap

expect class ImageStorage() {
    suspend fun getImageBitmapFromUrl(url: String): ImageBitmap
    fun saveImage(url: String, fileName: String)
    fun getImage(fileName: String): ByteArray

    fun checkIfImageExists(fileName: String): Boolean
    fun deleteImage(fileName: String): Boolean // Returns true if deletion was successful
    fun convertToImageBitmap(image: ByteArray): ImageBitmap
}