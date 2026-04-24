import groovy.json.JsonSlurper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// 加载签名配置
val keystoreProperties = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) {
        load(keystoreFile.inputStream())
    }
}

// 版本号管理：从 version.json 读取并自动递增 patch 版本
data class VersionInfo(var major: Int, var minor: Int, var patch: Int)

fun loadVersionInfo(): VersionInfo {
    val versionFile = rootProject.file("version.json")
    return if (versionFile.exists()) {
        val json = JsonSlurper().parseText(versionFile.readText()) as Map<*, *>
        VersionInfo(
            major = (json["major"] as Number).toInt(),
            minor = (json["minor"] as Number).toInt(),
            patch = (json["patch"] as Number).toInt()
        )
    } else {
        VersionInfo(1, 0, 0)  // 默认初始版本
    }
}

fun saveVersionInfo(info: VersionInfo) {
    val versionFile = rootProject.file("version.json")
    versionFile.writeText("""
        {
            "major": ${info.major},
            "minor": ${info.minor},
            "patch": ${info.patch}
        }
    """.trimIndent())
}

// 每次 release 打包时自动递增 patch 版本
fun incrementPatchVersion() {
    val versionInfo = loadVersionInfo()
    versionInfo.patch += 1
    // 当 patch 达到 10 时，进位到 minor
    if (versionInfo.patch >= 10) {
        versionInfo.patch = 0
        versionInfo.minor += 1
    }
    // 当 minor 达到 10 时，进位到 major
    if (versionInfo.minor >= 10) {
        versionInfo.minor = 0
        versionInfo.major += 1
    }
    saveVersionInfo(versionInfo)
}

android {
    namespace = "com.qrcodekit.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qrcodekit.app"
        minSdk = 35
        targetSdk = 35

        // 从 version.json 读取版本信息
        val versionInfo = loadVersionInfo()
        versionCode = versionInfo.major * 10000 + versionInfo.minor * 100 + versionInfo.patch
        versionName = "${versionInfo.major}.${versionInfo.minor}.${versionInfo.patch}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 只保留arm64架构，兼容主流设备，减小包体积
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            // 从 keystore.properties 读取签名信息
            storeFile = file(keystoreProperties["keystore.path"] as String? ?: "keystore.jks")
            storePassword = keystoreProperties["keystore.password"] as String? ?: ""
            keyAlias = keystoreProperties["key.alias"] as String? ?: ""
            keyPassword = keystoreProperties["key.password"] as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // 签名配置（需要配置 keystore.properties）
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// 配置APK输出文件名
android.applicationVariants.all {
    val appName = "Qraft"
    val dateFormat = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
    val timestamp = dateFormat.format(Date())

    outputs.all {
        if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
            outputFileName = "${appName}_${versionName}_${timestamp}.apk"
        }
    }
}

// release 打包前自动递增版本号
tasks.register("incrementVersion") {
    doLast {
        incrementPatchVersion()
    }
}

// 获取应用的所有变体
android.applicationVariants.all {
    // 对于 release 变体，在打包前递增版本号
    if (name == "release") {
        assembleProvider.get().dependsOn(tasks.named("incrementVersion"))
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room for history
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // QR Code - ZXing
    implementation("com.google.zxing:core:3.5.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Image zoomable for Compose (PhotoView-like functionality)
    implementation("net.engawapg.lib:zoomable:1.6.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.2.0")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("androidx.compose.ui:ui-test-junit4") {
        exclude(group = "androidx.compose.ui", module = "ui")
        exclude(group = "androidx.compose.ui", module = "ui-graphics")
        exclude(group = "androidx.compose.ui", module = "ui-tooling")
    }
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
