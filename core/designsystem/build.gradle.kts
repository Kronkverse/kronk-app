// :core:designsystem — Compose theme + primitives.
//
// Owns KronkTheme (colours + typography + shapes mirroring the web's
// tokens.yaml) and the platform primitives (Stage, SpaceBadge,
// MembraneNav, KornerGlyph, StatusKornerCard, ComposeShell). Every
// feature module depends on this and nothing else UI-wise.
//
// Empty scaffold in this PR; theme + primitives land in follow-up PRs
// (Phase 1 tasks #66 + #67 in docs/rebuild/plan.md).

plugins {
    // Plugins are on the classpath via root build.gradle; version-less
    // id() is required (a version conflicts with the classpath entry).
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "info.kronk.core.designsystem"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
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
    // Compose BOM aligns every androidx.compose.* artifact version.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
