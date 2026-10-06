package dev.muazkadan.hellonative

import dev.muazkadan.hellonative.ffm.NativeGreetingLib
import dev.muazkadan.hellonative.nativeinterop.loadNativeGreetingLibrary

actual fun nativeGreeting(): String = NativeGreetingFfm.getNativeGreeting()

/** Calls the jextract-generated FFM bindings once the library is loaded. */
private object NativeGreetingFfm {
    init {
        loadNativeGreetingLibrary()
    }

    fun getNativeGreeting(): String =
        // The C function returns a pointer to a NUL-terminated string of unknown size,
        // so widen the zero-length segment before reading it.
        NativeGreetingLib.getNativeGreeting()
            .reinterpret(Long.MAX_VALUE)
            .getString(0)
}
