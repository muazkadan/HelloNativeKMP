import dev.muazkadan.hellonative.buildlogic.CargoBuild
import dev.muazkadan.hellonative.buildlogic.currentDesktopPlatform
import dev.muazkadan.hellonative.buildlogic.currentRustTarget
import dev.muazkadan.hellonative.buildlogic.isMacOsHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("hellonative.cargo")
}

// Native Rust builds for iOS and desktop. Android builds the same workspace in :native.
val nativeSourceDir = layout.settingsDirectory.dir("native")

fun registerNativeBuild(name: String, configure: CargoBuild.() -> Unit) =
    tasks.register<CargoBuild>(name) {
        group = "native"
        workspaceDir.set(nativeSourceDir)
        sources.from(fileTree(nativeSourceDir) { include("Cargo.*", ".cargo/**", "*/Cargo.toml", "*/src/**") })
        targetDir.set(layout.buildDirectory.dir("cargo/$name/target"))
        outputDir.set(layout.buildDirectory.dir("cargo/$name/out"))
        configure()
    }

val buildNativeDesktop = registerNativeBuild("buildNativeDesktop") {
    description = "Builds the JNI library for the host OS/arch, laid out for Compose Desktop app resources."
    packageName.set("native_greeting_jni")
    libraryFileName.set(System.mapLibraryName("native_greeting_jni"))
    targets.put(currentRustTarget, currentDesktopPlatform)
    if (isMacOsHost) environment.put("MACOSX_DEPLOYMENT_TARGET", "12.0")
}

// Exposes the desktop JNI library to :desktopApp, which bundles it as app resources.
configurations.consumable("desktopNativeLibsElements") {
    attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named("desktop-native-libs")) }
    outgoing.artifact(buildNativeDesktop.flatMap { it.outputDir })
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    // Android and desktop share one JNI binding in jvmCommonMain.
    // withAndroidTarget() doesn't match the Android-KMP library target, so select it by platform type.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmCommon") {
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
                withJvm()
            }
        }
    }

    android {
        namespace = "dev.muazkadan.hellonative.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        withHostTest {}
    }

    listOf(
        iosArm64() to "aarch64-apple-ios",
        iosSimulatorArm64() to "aarch64-apple-ios-sim",
    ).forEach { (iosTarget, rustTarget) ->
        val nativeBuildName = "buildNative${iosTarget.name.replaceFirstChar(Char::uppercase)}"
        val buildNativeIos = registerNativeBuild(nativeBuildName) {
            description = "Builds the static Rust library for ${iosTarget.name}."
            packageName.set("native_greeting")
            libraryFileName.set("libnative_greeting.a")
            targets.put(rustTarget, ".")
            // Kotlin/Native's minimum iOS version, so linking never warns about newer objects.
            environment.put("IPHONEOS_DEPLOYMENT_TARGET", "15.0")
        }

        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false
        }

        val nativeGreeting = iosTarget.compilations["main"].cinterops.create("native_greeting") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/native_greeting.def"))
            includeDirs.headerFilterOnly(nativeSourceDir.dir("include"))
            extraOpts("-libraryPath", layout.buildDirectory.dir("cargo/$nativeBuildName/out").get().asFile.absolutePath)
        }
        tasks.named(nativeGreeting.interopProcessingTaskName) {
            dependsOn(buildNativeIos)
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
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

tasks.named<Test>("jvmTest") {
    // Load the library exactly like the packaged desktop app does.
    val nativeLibsDir = buildNativeDesktop.flatMap { it.outputDir.dir(currentDesktopPlatform) }
    inputs.dir(nativeLibsDir).withPropertyName("nativeLibsDir").withPathSensitivity(PathSensitivity.RELATIVE)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dcompose.application.resources.dir=${nativeLibsDir.get().asFile.absolutePath}")
    })
}

// This library has no Compose UI tests, so its Wasm tests don't need an executable bundle.
// Remove this (and add `binaries.executable()` to wasmJs) if Compose UI tests are added.
tasks.named { it == "checkComposeUiTestConfigurationForWasmJs" }.configureEach {
    enabled = false
}
