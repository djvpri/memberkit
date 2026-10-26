plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.memberkit.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.memberkit.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 7
        versionName = "1.3.1"

        // diisi dari local.properties / gradle.properties: BACKEND_URL & BACKEND_TOKEN
        val backendUrl = (project.findProperty("MEMBERKIT_BACKEND_URL") as String?) ?: ""
        val backendToken = (project.findProperty("MEMBERKIT_BACKEND_TOKEN") as String?) ?: ""
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
        buildConfigField("String", "BACKEND_TOKEN", "\"$backendToken\"")
    }

    signingConfigs {
        create("release") {
            val ksFile = File(rootProject.rootDir, "keystore/memberkit-release.jks")
            val pwFile = File(rootProject.rootDir, "keystore/password.txt")
            if (ksFile.exists() && pwFile.exists()) {
                storeFile = ksFile
                storePassword = pwFile.readText().trim()
                keyAlias = "memberkit"
                keyPassword = pwFile.readText().trim()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (signingConfigs.getByName("release").storeFile != null)
                signingConfigs.getByName("release") else null
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    testImplementation("junit:junit:4.13.2")
}
