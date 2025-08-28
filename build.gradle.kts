plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("org.jetbrains.kotlinx.atomicfu") version "0.29.0"
    kotlin("plugin.serialization") version "2.2.0"
}