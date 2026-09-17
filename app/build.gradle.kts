// :app — Kronk 2.0 Android app (Kotlin/Compose rewrite).
//
// Direction (per docs/rebuild/plan.md): at ship time this replaces
// the current app via the self-updater — one app icon on-device, no
// side-by-side install for end users. During development, debug
// builds carry the ".next" applicationIdSuffix so they DON'T collide
// with the shipping app on test devices (side-load-friendly for
// mangobee + Tal's testing without wiping their real Kronk install).
//
// At cutover (Phase 7), release builds ship under the shipping
// applicationId "info.kronk.app" and the :mastodon module is
// deleted from the build.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "info.kronk.app"
    compileSdk = 35

    defaultConfig {
        // Shipping applicationId. Release builds hit this directly
        // when they cut over; debug builds get the ".next" suffix
        // below to keep test devices unimpacted.
        applicationId = "info.kronk.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isDebuggable = true
            // ".next.debug" — side-loads alongside the shipping app
            // during Phase 1–6 without touching real user installs.
            applicationIdSuffix = ".next.debug"
            versionNameSuffix = "-debug"
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
