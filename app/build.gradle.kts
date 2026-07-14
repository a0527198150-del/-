plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.app" // וודא שזה תואם לשם החבילה שלך
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    // ספריות בסיס ואנדרואיד
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.converter.moshi)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.recaptcha)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.androidx.webkit)
    implementation("androidx.appcompat:appcompat:1.6.1")

    // הגדרת ה-KSP בצורה בטוחה
    ksp(libs.moshi.kotlin.codegen)

    // ספריות בדיקה
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    // בדיקות UI
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)

    // ספריות פיתוח
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
