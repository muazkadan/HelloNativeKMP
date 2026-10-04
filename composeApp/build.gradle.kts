import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "dev.muazkadan.hellonative.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false
        }

        iosTarget.compilations["main"].cinterops.create("native_greeting") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/native_greeting.def"))
            includeDirs.headerFilterOnly(rootProject.file("native/src/"))
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(projects.native)
            implementation("net.java.dev.jna:jna:5.17.0@aar")
        }
        jvmMain.dependencies {
            implementation("net.java.dev.jna:jna:5.17.0")
            implementation("org.scijava:native-lib-loader:2.5.0")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
