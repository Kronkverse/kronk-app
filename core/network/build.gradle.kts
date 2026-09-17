// :core:network — Retrofit + kotlinx-serialization stack for the 2.0
// rewrite. Owns wire DTOs, API interfaces, and the Retrofit factory.
// Domain types live in :core:model; DTOs map to domain types via
// small .toDomain() extensions kept inside this module (the domain
// layer never sees wire types).
//
// Kotlin-only (:core:network is JVM, no Android APIs). If a future
// consumer needs Android-specific network features (WorkManager
// scheduling, ConnectivityManager awareness), that's a separate
// :core:network-android sibling.

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
}
