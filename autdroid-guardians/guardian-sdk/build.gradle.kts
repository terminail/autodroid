plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-kapt")
}

android {
    namespace = "com.autodroid.guardiansdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
        dataBinding = true
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.appcompat:appcompat:1.7.0")
        force("androidx.lifecycle:lifecycle-livedata:2.8.1")
        force("androidx.lifecycle:lifecycle-livedata-core:2.8.1")
        force("androidx.lifecycle:lifecycle-livedata-core-ktx:2.8.1")
        force("androidx.lifecycle:lifecycle-livedata-ktx:2.8.1")
        force("androidx.lifecycle:lifecycle-runtime:2.8.1")
        force("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
        force("androidx.lifecycle:lifecycle-viewmodel:2.8.1")
        force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")
        force("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.1")
        force("androidx.lifecycle:lifecycle-common:2.8.1")
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.recyclerview)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    kapt("androidx.room:room-compiler:2.7.0")
}