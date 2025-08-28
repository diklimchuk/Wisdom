@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("../Build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs.create("libs").from(files("../Build-logic/libs.versions.toml"))
}

rootProject.name = "Wisdom"

include(":wisdom-core")