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
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
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
            // Release builds target production (cutover-flipped in
            // Kronkverse/kronk#1859). Reads via BuildConfig.KRONK_HOST
            // through :core:common's KronkHost.
            buildConfigField("String", "KRONK_HOST", "\"kronk.info\"")
        }
        debug {
            isDebuggable = true
            // ".next.debug" — side-loads alongside the shipping app
            // during Phase 1–6 without touching real user installs.
            applicationIdSuffix = ".next.debug"
            versionNameSuffix = "-debug"
            // Debug builds target shadow so testers see the rebuild
            // line (Hub grid, Kommons, reach ladder) that production
            // doesn't ship until cutover.
            buildConfigField("String", "KRONK_HOST", "\"shadow.kronk.info\"")
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
        // KronkApplication reads BuildConfig.DEBUG to toggle
        // WebView.setWebContentsDebuggingEnabled — enables
        // chrome://inspect for debugging the in-app WebViews.
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":feature:auth"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.browser)
    // Direct OkHttp for the push-subscription POST (KronkRetrofit is
    // Hilt-scoped and heavyweight for a single fire-and-forget call).
    implementation(libs.okhttp)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.unifiedpush.connector)

    // Hilt: :app is the composition root, so it applies the plugin
    // and hosts the SingletonComponent via KronkApplication.
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
