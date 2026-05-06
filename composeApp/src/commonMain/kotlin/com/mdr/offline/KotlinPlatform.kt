package com.mdr.offline

enum class KotlinPlatform {
    Android, IOS
}

expect val currentPlatform: KotlinPlatform