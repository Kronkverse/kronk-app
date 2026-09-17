// :core:model — Domain types (Account, Status, Reach, Krew, Nudge,
// Proposal, KornerManifest). Kotlin-only, no Android or Compose deps
// (these types travel from network layer through data layer to UI
// unchanged; keeping them pure means every layer can depend on them).
//
// Empty scaffold; fills as domain modelling lands.

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}
