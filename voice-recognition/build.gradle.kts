plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "solutions.s4y.whisper"
    compileSdk = 34

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation(project(":audio"))
    implementation(libs.pytorch.android.lite)

    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.gpu.delegate.plugin)
    //implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.gpu.api)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}