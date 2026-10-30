plugins {
    id("com.android.library") version "9.1.1"
    id("maven-publish")
}

group = providers.gradleProperty("group").orElse("test.test.test").get()
version = providers.gradleProperty("version").orElse("1.0.0").get()

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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.activity:activity:1.8.2")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "neostore-iar"
            version = project.version.toString()
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
