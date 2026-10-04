import dev.muazkadan.hellonative.buildlogic.CMakeBuild
import dev.muazkadan.hellonative.buildlogic.currentDesktopPlatform
import dev.muazkadan.hellonative.buildlogic.isMacOsHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("hellonative.cmake")
}

// Native C++ builds for iOS and desktop. Android builds the same sources via AGP in :native.
val nativeSourceDir = layout.settingsDirectory.dir("native")

fun registerNativeBuild(name: String, configure: CMakeBuild.() -> Unit) =
    tasks.register<CMakeBuild>(name) {
        group = "native"
        sourceDir.set(nativeSourceDir)
        sources.from(fileTree(nativeSourceDir) { include("CMakeLists.txt", "include/**", "src/**", "jni/**") })
        buildDir.set(layout.buildDirectory.dir("cmake/$name/build"))
        installDir.set(layout.buildDirectory.dir("cmake/$name/install"))
        configure()
    }

val buildNativeDesktop = registerNativeBuild("buildNativeDesktop") {
    description = "Builds the JNI library for the host OS/arch, laid out for Compose Desktop app resources."
    arguments.addAll("-DNATIVE_GREETING_JNI=ON", "-DCMAKE_INSTALL_LIBDIR=$currentDesktopPlatform")
    if (isMacOsHost) arguments.add("-DCMAKE_OSX_DEPLOYMENT_TARGET=12.0")
    // FindJNI locates jni.h through JAVA_HOME.
    environment.put("JAVA_HOME", javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
    }.map { it.metadata.installationPath.asFile.absolutePath })
}

// Exposes the desktop JNI library to :desktopApp, which bundles it as app resources.
configurations.consumable("desktopNativeLibsElements") {
    attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named("desktop-native-libs")) }
    outgoing.artifact(buildNativeDesktop.flatMap { it.installDir })
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
        iosArm64() to "iphoneos",
        iosSimulatorArm64() to "iphonesimulator",
    ).forEach { (iosTarget, sdk) ->
        val nativeBuildName = "buildNative${iosTarget.name.replaceFirstChar(Char::uppercase)}"
        val buildNativeIos = registerNativeBuild(nativeBuildName) {
            description = "Builds the static C++ library for ${iosTarget.name}."
            arguments.addAll(
                "-DCMAKE_SYSTEM_NAME=iOS",
                "-DCMAKE_OSX_SYSROOT=$sdk",
                "-DCMAKE_OSX_ARCHITECTURES=arm64",
                // Kotlin/Native's minimum iOS version, so linking never warns about newer objects.
                "-DCMAKE_OSX_DEPLOYMENT_TARGET=15.0",
            )
        }

        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false
        }

        val nativeGreeting = iosTarget.compilations["main"].cinterops.create("native_greeting") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/native_greeting.def"))
            includeDirs.headerFilterOnly(nativeSourceDir.dir("include"))
            extraOpts("-libraryPath", layout.buildDirectory.dir("cmake/$nativeBuildName/install/lib").get().asFile.absolutePath)
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
    val nativeLibsDir = buildNativeDesktop.flatMap { it.installDir.dir(currentDesktopPlatform) }
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
