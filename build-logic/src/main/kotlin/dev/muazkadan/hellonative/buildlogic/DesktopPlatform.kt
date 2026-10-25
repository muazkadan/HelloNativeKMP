package dev.muazkadan.hellonative.buildlogic

internal val currentOs: String = System.getProperty("os.name").lowercase().let {
    when {
        it.contains("mac") -> "macos"
        it.contains("win") -> "windows"
        else -> "linux"
    }
}

private val currentArch: String = when (System.getProperty("os.arch")) {
    "aarch64", "arm64" -> "arm64"
    else -> "x64"
}

/**
 * The host's `<os>-<arch>` folder name, matching Compose Desktop's
 * `appResourcesRootDir` layout (for example `macos-arm64`).
 */
val currentDesktopPlatform: String = "$currentOs-$currentArch"

val isMacOsHost: Boolean get() = currentOs == "macos"

/** The Rust target triple for the host, so desktop builds use the same output layout as cross builds. */
val currentRustTarget: String = when (currentOs) {
    "macos" -> "apple-darwin"
    "windows" -> "pc-windows-msvc"
    else -> "unknown-linux-gnu"
}.let { if (currentArch == "arm64") "aarch64-$it" else "x86_64-$it" }
