plugins {
    id("elmslie.kotlin-multiplatform-lib")
    id("elmslie.publishing")
    alias(libs.plugins.binaryCompatibilityValidator)
}

elmsliePublishing {
    pom {
        name = "Wisdom core"
        description = "Write into server, disk or memory"
    }
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutinesCore)
                implementation(libs.stately.concurrent.collections)
                implementation(libs.atomicfu)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutinesTest)
                implementation(libs.kotlin.test)
            }
        }
    }
}
