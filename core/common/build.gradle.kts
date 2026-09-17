// :core:common — Kotlin-only utilities, Result types, dispatchers.
// No Android APIs allowed. See docs/rebuild/plan.md for the module map.
//
// Empty scaffold; fills as later phases need it.

plugins {
    // Plugin is on the classpath via root build.gradle; version-less
    // id() is required (a version conflicts with the classpath entry).
    // A plugins-DSL / pluginManagement migration is a separate refactor.
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}
