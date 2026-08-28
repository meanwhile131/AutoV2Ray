import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.dpdns.meanwhile131.autov2ray"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.dpdns.meanwhile131.autov2ray"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

val libXrayDir = layout.buildDirectory.dir("lib")
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(
        fileTree(
            mapOf(
                "dir" to libXrayDir.map { it.dir("libxray-android") },
                "include" to listOf("*.aar", "*.jar")
            )
        )
    )
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
val libXrayDownload = tasks.register<Download>("downloadLibXray") {
    description = "Download libXray"
    src("https://github.com/XTLS/libXray/releases/latest/download/libxray-android.zip")
    dest(layout.buildDirectory.file("libxray-android.zip"))
    onlyIfModified(true)
}
val libXray = tasks.register<Copy>("extractLibXray") {
    description = "Extract libXray archive"
    dependsOn(libXrayDownload)
    from(zipTree(libXrayDownload.get().dest))
    into(libXrayDir)
}
tasks.named("preBuild") {
    dependsOn(libXray)
}