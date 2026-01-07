plugins {
    id("com.android.application")
}

android {
    namespace = "com.autodroid.trader.ime"
      compileSdk = 36


    defaultConfig {
        applicationId = "com.autodroid.trader.ime"
        minSdk = 26

        versionCode = 1
        versionName = "1.0"
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

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.core:core:1.12.0")
}
