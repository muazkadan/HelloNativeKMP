import dev.muazkadan.hellonative.buildlogic.CargoBuild
import dev.muazkadan.hellonative.buildlogic.androidCargoEnvironment
import dev.muazkadan.hellonative.buildlogic.androidRustTargets

plugins {
    alias(libs.plugins.androidLibrary)
    id("hellonative.cargo")
}

// Packages the JNI bridge (libnative_greeting_jni.so) for every Android ABI.
// Lives in its own module, as it did with the CMake build, so the Android-only
// packaging stays out of the KMP library.
android {
    namespace = "dev.muazkadan.hellonative.nativelib"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    // Provides the linkers for Cargo, and lets AGP strip debug symbols when packaging.
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

val buildNativeAndroid = tasks.register<CargoBuild>("buildNativeAndroid") {
    group = "native"
    description = "Builds the JNI library for every Android ABI with Cargo."
    workspaceDir.set(layout.projectDirectory)
    sources.from(fileTree(layout.projectDirectory) { include("Cargo.*", ".cargo/**", "*/Cargo.toml", "*/src/**") })
    packageName.set("native_greeting_jni")
    libraryFileName.set("libnative_greeting_jni.so")
    targets.set(androidRustTargets)
    environment.putAll(androidComponents.sdkComponents.ndkDirectory.map {
        androidCargoEnvironment(it.asFile, libs.versions.android.minSdk.get().toInt())
    })
    targetDir.set(layout.buildDirectory.dir("cargo/target"))
    outputDir.set(layout.buildDirectory.dir("cargo/jniLibs"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(buildNativeAndroid, CargoBuild::outputDir)
    }
}
