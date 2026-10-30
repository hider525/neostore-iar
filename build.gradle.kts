plugins {
    id("com.android.library") version "9.1.1"
}

group = "test.test.test"
version = "1.0.0"

android {
    namespace = "com.neostudio.iar"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.activity:activity:1.8.2")
}
