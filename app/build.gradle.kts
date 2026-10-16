import java.io.File
import java.io.FileOutputStream
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.cateatmars_dev.yoedodurian"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cateatmars_dev.yoedodurian"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    androidResources {
        noCompress += "tflite"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // CameraX
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // TFLite
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.5.0")

    // Chart and JSON
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.register("downloadFonts") {
    doLast {
        val fontDir = file("src/main/res/font")
        fontDir.mkdirs()
        
        val regularFile = File(fontDir, "sarabun_regular.ttf")
        if (!regularFile.exists()) {
            URI("https://github.com/google/fonts/raw/main/ofl/sarabun/Sarabun-Regular.ttf").toURL().openStream().use { input ->
                FileOutputStream(regularFile).use { output -> input.copyTo(output) }
            }
        }
        
        val boldFile = File(fontDir, "sarabun_bold.ttf")
        if (!boldFile.exists()) {
            URI("https://github.com/google/fonts/raw/main/ofl/sarabun/Sarabun-Bold.ttf").toURL().openStream().use { input ->
                FileOutputStream(boldFile).use { output -> input.copyTo(output) }
            }
        }

        val drawableDir = file("src/main/res/drawable")
        val mwitLogo = File(drawableDir, "logo_mwit.png")
        if (!mwitLogo.exists()) {
            try {
                val conn = URI("https://upload.wikimedia.org/wikipedia/th/4/47/Mwit-logo.png").toURL().openConnection()
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.getInputStream().use { input ->
                    FileOutputStream(mwitLogo).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                // Ignore if offline
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("downloadFonts")
}