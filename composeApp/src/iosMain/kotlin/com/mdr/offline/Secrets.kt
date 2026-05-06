package com.mdr.offline

import platform.Foundation.NSBundle
import platform.Foundation.NSDictionary
import platform.Foundation.dictionaryWithContentsOfFile

actual object Secrets {
    actual val CLIENT_ID: String
        get() = getStringResource(
            filename = "Secrets",
            fileType = "plist",
            valueKey = "CLIENT_ID"
        ) ?: ""
    actual val CLIENT_SECRET: String
        get() = getStringResource(
            filename = "Secrets",
            fileType = "plist",
            valueKey = "CLIENT_SECRET"
        ) ?: ""
}

internal fun getStringResource(
    filename: String,
    fileType: String,
    valueKey: String,
): String? {
    val result = NSBundle.mainBundle.pathForResource(filename, fileType)?.let {
        val map = NSDictionary.dictionaryWithContentsOfFile(it)
        map?.get(valueKey) as? String
    }
    return result
}