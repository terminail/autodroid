// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
    }
    dependencies {
        // Use Android Gradle Plugin 8.13.0 which matches Gradle 8.13
        classpath("com.android.tools.build:gradle:8.13.1")
        
        // Kotlin plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
        
        // Navigation Safe Args plugin
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7")
        
        // Explicitly declare lint dependency in single-string format to avoid deprecated notation warning
        classpath("com.android.tools.lint:lint-gradle:31.13.0") {
            // Exclude kotlin-compiler-embeddable to avoid conflict with Kotlin Gradle plugin
            exclude(group = "com.android.tools.external.com-intellij", module = "kotlin-compiler")
        }
        
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}