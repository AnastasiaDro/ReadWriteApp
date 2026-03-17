plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidkmp)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.cerebus.create_screen"
        compileSdk = 36
        minSdk = 29

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    val xcfName = "feature:create_screenKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.coil)
                implementation(libs.koin.compose.viewmodel)
                implementation(project(":core:ui"))
                implementation(project(":core:utils"))
                implementation(project(":core:game_engine"))
                implementation(project(":core:deck_package"))
                implementation(project(":data:decks"))
                implementation(project(":data:flashcards"))
                implementation(project(":data:preferences"))
                implementation(project(":data:student"))
                implementation(project(":data:student-deck"))
                implementation(project(":feature:customkeyboard"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}
