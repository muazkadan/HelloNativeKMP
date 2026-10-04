package dev.muazkadan.hellonative.nativeinterop

/**
 * JNI binding to `libnative_greeting_jni`, shared by Android and desktop.
 *
 * The native side binds these methods with `RegisterNatives` in `JNI_OnLoad`,
 * so the class name and method signatures must match `native/jni/native_greeting_jni.cpp`.
 */
internal object NativeGreetingJni {
    init {
        loadNativeGreetingLibrary()
    }

    @JvmStatic
    external fun getNativeGreeting(): String
}

internal const val NATIVE_GREETING_LIBRARY = "native_greeting_jni"

/** Loads [NATIVE_GREETING_LIBRARY] from wherever the platform packages it. */
internal expect fun loadNativeGreetingLibrary()
