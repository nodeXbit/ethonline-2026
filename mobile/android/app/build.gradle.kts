import java.util.Properties

plugins {
    id("com.android.application")
}

val privyLocalProperties = Properties().apply {
    val localFile = rootProject.file("privy.local.properties")
    if (localFile.isFile) {
        localFile.inputStream().use(::load)
    }
}

fun localBuildConfigString(name: String): String {
    val value = privyLocalProperties.getProperty(name, "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$value\""
}

android {
    namespace = "io.github.nodexbit.ethonline2026"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "io.github.nodexbit.ethonline2026"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "PRIVY_APP_ID", localBuildConfigString("PRIVY_APP_ID"))
        buildConfigField("String", "PRIVY_APP_CLIENT_ID", localBuildConfigString("PRIVY_APP_CLIENT_ID"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("io.privy:privy-core:0.14.0")
}
