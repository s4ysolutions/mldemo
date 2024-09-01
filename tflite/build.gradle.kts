plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "solutions.s4y.tflite"
    compileSdk = libs.versions.sdkCompile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.sdkMin.get().toInt()

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
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    useJUnitPlatform()
}


dependencies {
    kapt(libs.dagger.hilt.android.compiler)

    implementation(project(":firebase"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.dagger.hilt.android)
    // implementation(libs.androidx.appcompat)
    //implementation(libs.google.material)
    implementation(libs.kotlinx.coroutines.core)

    // Tensorflow Lite dependencies for Google Play services
    // implementation(libs.play.services.tflite.java)
    // implementation(libs.play.services.tflite.support)
    // implementation(libs.play.services.tflite.gpu)

    // Interpreter, use obsolete version is skipped
    implementation(libs.tensorflow.lite)
    // InterpreterApi
    implementation(libs.tensorflow.lite.api)
    // TensorBuffer
    implementation(libs.tensorflow.lite.support)
    // GpuDelegate,CompatibilityList
    implementation(libs.tensorflow.lite.gpu)
    // GpuDelegate,GpuDelegateFactory
    implementation(libs.tensorflow.lite.gpu.api)
    implementation(libs.tensorflow.lite.gpu.delegate.plugin)
    implementation(libs.tensorflow.lite.select.tf.ops)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.dagger.hilt.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

kapt {
    correctErrorTypes = true
}
