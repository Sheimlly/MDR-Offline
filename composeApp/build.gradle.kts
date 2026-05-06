import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqlDelight)
    kotlin("plugin.serialization") version "1.9.20"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false // Default is true but it has to be set to false to database work on ios :c
        }
    }
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation("androidx.startup:startup-runtime:1.2.0")
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            implementation(libs.decompose)

            implementation(libs.androidx.core.ktx.v1131)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.core.splashscreen)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(compose.material3)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.core)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation("com.plusmobileapps:konnectivity:0.1-alpha01")
            implementation(libs.stately.common)

            implementation(libs.koin.compose)
            implementation(libs.kamel.image)

            implementation("net.engawapg.lib:zoomable:2.5.0")

            implementation(libs.decompose)
            implementation(libs.decompose.compose)
            implementation("com.arkivanov.decompose:extensions-compose:3.3.0")
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }
    }
}

android {
    namespace = "com.mdr.offline"
    compileSdk = 36

    defaultConfig {
        val CLIENT_ID = project.loadLocalProperty(
            path = "local.properties",
            propertyName = "CLIENT_ID"
        )
        val CLIENT_SECRET = project.loadLocalProperty(
            path = "local.properties",
            propertyName = "CLIENT_SECRET"
        )

        buildConfigField("String", "CLIENT_ID", "\"$CLIENT_ID\"")
        buildConfigField("String", "CLIENT_SECRET", "\"$CLIENT_SECRET\"")

        applicationId = "com.mdr.offline"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.material3.android)
    implementation(libs.protolite.well.known.types)
    implementation(libs.firebase.crashlytics.buildtools)
    debugImplementation(compose.uiTooling)
}

sqldelight {
    databases {
        create(name = "MDROfflineDatabase") {
            packageName.set("com.mdr.offline.db")
        }
    }
}

fun Project.loadLocalProperty(
    path: String,
    propertyName: String,
): String {
    val localProperties = Properties()
    val localPropertiesFile = project.rootProject.file(path)
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
        return localProperties.getProperty(propertyName)
    } else {
        throw GradleException("can not find property : $propertyName")
    }

}