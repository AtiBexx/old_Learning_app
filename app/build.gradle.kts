plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //Json-höz || for Json
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.attibexx.old_learning_app"
    compileSdk = 36
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.attibexx.old_learning_app"
        minSdk = 21
        targetSdk = 36
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
            /*
            // A "release" buildhez is létrehozzuk az értéket, 'false' tartalommal
            resValue("bool", "is_debug_mode", "false")
        }*/

            /*debug {
            // Manuálisan definiáljuk a DEBUG mezőt a BuildConfig-ban.
            // Ez megoldja azokat a problémákat, amikor az IDE nem találja.
            buildConfigField("boolean", "DEBUG", "true")
        }*/
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1" // Használhatsz újabb verziót is, ha van telepítve
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        /*
        //Enable buildConfig
        buildConfig = true*/
        //Enable viewBinding
        viewBinding = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    //json függösék || json dependency
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    //
    implementation(libs.amrdeveloper.codeview)
    //
    implementation(libs.androidx.legacy.support.v4)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}