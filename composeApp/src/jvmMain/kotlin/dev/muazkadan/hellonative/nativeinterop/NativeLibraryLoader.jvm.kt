package dev.muazkadan.hellonative.nativeinterop

import java.io.File

internal actual fun loadNativeGreetingLibrary() {
    // Compose Desktop sets this for `run` and for packaged apps; it points at the
    // app resources for the current OS/arch, where desktopApp bundles the library.
    val bundledLibrary = System.getProperty("compose.application.resources.dir")
        ?.let { File(it, System.mapLibraryName(NATIVE_GREETING_LIBRARY)) }
        ?.takeIf(File::isFile)

    if (bundledLibrary != null) {
        System.load(bundledLibrary.absolutePath)
    } else {
        System.loadLibrary(NATIVE_GREETING_LIBRARY)
    }
}
