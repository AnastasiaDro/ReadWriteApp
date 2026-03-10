import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose)
            implementation(libs.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.serialization.json)
            implementation(project(":data:database"))
            implementation(project(":feature:game_screen"))
            implementation(project(":data:flashcards"))
            implementation(project(":data:decks"))
            implementation(project(":data:student"))
            implementation(project(":data:student-deck"))
            implementation(project(":data:preferences"))
            implementation(project(":data:study_progress"))
            implementation(project(":core:utils"))
            implementation(project(":core:game_engine"))
            implementation(project(":core:deck_package"))
            implementation(project(":core:ui"))
            implementation(project(":feature:create_screen"))
            implementation(project(":feature:student"))
            implementation(project(":feature:session_settings"))
            implementation(libs.coil)
            implementation(libs.compose.icons)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.kmp.zip)
        }
    }
}

android {
    namespace = "com.cerebus.readwrite"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.cerebus.readwrite"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    debugImplementation(libs.compose.uiTooling)
}

val featureStudentResourcesOutput = layout.buildDirectory.dir("generated/featureStudentComposeResources")
val featureSessionSettingsResourcesOutput = layout.buildDirectory.dir("generated/featureSessionSettingsComposeResources")
val featureGameScreenResourcesOutput = layout.buildDirectory.dir("generated/featureGameScreenComposeResources")

val copyFeatureStudentComposeResources by tasks.registering(Copy::class) {
    dependsOn(":feature:student:prepareComposeResourcesTaskForCommonMain")
    from(
        project(":feature:student")
            .layout
            .buildDirectory
            .dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")
    )
    into(featureStudentResourcesOutput.map {
        it.dir("composeResources/readwriteapp.feature.student.generated.resources")
    })
}

val copyFeatureSessionSettingsComposeResources by tasks.registering(Copy::class) {
    dependsOn(":feature:session_settings:prepareComposeResourcesTaskForCommonMain")
    from(
        project(":feature:session_settings")
            .layout
            .buildDirectory
            .dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")
    )
    into(featureSessionSettingsResourcesOutput.map {
        it.dir("composeResources/readwriteapp.feature.session_settings.generated.resources")
    })
}

val copyFeatureGameScreenComposeResources by tasks.registering(Copy::class) {
    dependsOn(":feature:game_screen:prepareComposeResourcesTaskForCommonMain")
    from(
        project(":feature:game_screen")
            .layout
            .buildDirectory
            .dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")
    )
    into(featureGameScreenResourcesOutput.map {
        it.dir("composeResources/readwriteapp.feature.game_screen.generated.resources")
    })
}

android.sourceSets.getByName("main").assets.srcDir(featureStudentResourcesOutput)
android.sourceSets.getByName("main").assets.srcDir(featureSessionSettingsResourcesOutput)
android.sourceSets.getByName("main").assets.srcDir(featureGameScreenResourcesOutput)

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(copyFeatureStudentComposeResources)
    dependsOn(copyFeatureSessionSettingsComposeResources)
    dependsOn(copyFeatureGameScreenComposeResources)
    dependsOn(":feature:student:convertXmlValueResourcesForCommonMain")
    dependsOn(":feature:student:copyNonXmlValueResourcesForCommonMain")
    dependsOn(":feature:student:prepareComposeResourcesTaskForCommonMain")
    dependsOn(":feature:session_settings:convertXmlValueResourcesForCommonMain")
    dependsOn(":feature:session_settings:copyNonXmlValueResourcesForCommonMain")
    dependsOn(":feature:session_settings:prepareComposeResourcesTaskForCommonMain")
    dependsOn(":feature:game_screen:convertXmlValueResourcesForCommonMain")
    dependsOn(":feature:game_screen:copyNonXmlValueResourcesForCommonMain")
    dependsOn(":feature:game_screen:prepareComposeResourcesTaskForCommonMain")
}
