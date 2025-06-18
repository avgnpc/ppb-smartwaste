plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.smartwaste.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartwaste.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Lifecycle and ViewModel
    implementation(libs.lifecycle.common)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Google Play Services Location
    implementation(libs.play.services.location)

    // Facebook Login
    implementation(libs.facebook.login)
    implementation(libs.camera.view)
    implementation(libs.androidx.lifecycle.viewmodel.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation (libs.androidx.appcompat.appcompat.v161.x2)
    implementation (libs.com.google.android.material.material.v190.x5)
    implementation (libs.androidx.constraintlayout.constraintlayout.v214.x4)

    // 2) Core CameraX modules (no versions needed—BOM supplies them)
    implementation(libs.androidx.camera.camera.core)
    implementation (libs.camera.camera2)        // for Camera2 support
    implementation (libs.androidx.camera.camera.lifecycle)      // ties camera lifecycle to ProcessCameraProvider
    implementation (libs.androidx.camera.camera.view2)           // for PreviewView

    // 3) Optional: for built-in extensions (Portrait, Night, HDR, etc.)
    implementation (libs.androidx.camera.camera.extensions)

    // 4) Guava (for ListenableFuture used by CameraX)
    implementation (libs.guava.v311android)

    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.task.vision)

    implementation (libs.okhttp)
    implementation (libs.logging.interceptor)
    implementation (libs.gson)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    implementation (libs.volley)
    implementation (libs.json)

    implementation (libs.imagekit.android)

    implementation(libs.okhttp)

    implementation (libs.glide)
    annotationProcessor (libs.compiler)

    implementation (libs.play.services.maps)
}
