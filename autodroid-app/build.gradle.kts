// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        // Chinese mirrors for better accessibility in mainland China
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        // Fallback to standard repositories
        mavenCentral()
        // Use explicit Google Maven repository URL
        maven { url = uri("https://maven.google.com/") }
    }
    dependencies {
        // Use Android Gradle Plugin 8.13.0 which matches Gradle 8.13
        classpath("com.android.tools.build:gradle:8.13.1")
        
        // Kotlin plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
        
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

allprojects {
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // Chinese mirrors for better accessibility in mainland China
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        // Fallback to standard repositories
        mavenCentral()
        maven { url = uri("https://maven.google.com/") }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}