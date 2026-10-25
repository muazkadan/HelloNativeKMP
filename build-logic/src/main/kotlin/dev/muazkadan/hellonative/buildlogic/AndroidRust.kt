package dev.muazkadan.hellonative.buildlogic

import java.io.File

/** Maps each Android ABI to its Rust target triple and the NDK clang target that links it. */
private val androidTargets = listOf(
    AndroidTarget(abi = "arm64-v8a", rustTarget = "aarch64-linux-android", clangTarget = "aarch64-linux-android"),
    AndroidTarget(abi = "armeabi-v7a", rustTarget = "armv7-linux-androideabi", clangTarget = "armv7a-linux-androideabi"),
    AndroidTarget(abi = "x86", rustTarget = "i686-linux-android", clangTarget = "i686-linux-android"),
    AndroidTarget(abi = "x86_64", rustTarget = "x86_64-linux-android", clangTarget = "x86_64-linux-android"),
)

private class AndroidTarget(val abi: String, val rustTarget: String, val clangTarget: String)

/** Rust target triple to ABI directory name, as [CargoBuild.targets] expects for a `jniLibs` layout. */
val androidRustTargets: Map<String, String> = androidTargets.associate { it.rustTarget to it.abi }

/**
 * Cargo environment that links every Android target with the NDK's clang for [minSdk].
 * Page alignment is set in `native/.cargo/config.toml`.
 */
fun androidCargoEnvironment(ndkDir: File, minSdk: Int): Map<String, String> {
    val hostTag = "$currentOs-x86_64".replace("macos", "darwin")
    val suffix = if (currentOs == "windows") ".cmd" else ""
    val bin = File(ndkDir, "toolchains/llvm/prebuilt/$hostTag/bin")
    return androidTargets.associate { target ->
        val variable = "CARGO_TARGET_${target.rustTarget.uppercase().replace('-', '_')}_LINKER"
        variable to File(bin, "${target.clangTarget}$minSdk-clang$suffix").absolutePath
    }
}
