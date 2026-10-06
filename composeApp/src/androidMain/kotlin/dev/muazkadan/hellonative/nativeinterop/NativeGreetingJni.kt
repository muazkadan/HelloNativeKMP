package dev.muazkadan.hellonative.nativeinterop

/**
 * JNI binding to `libnative_greeting_jni`, which the :native module packages for every ABI.
 *
 * The native side binds these methods with `RegisterNatives` in `JNI_OnLoad`,
 * so the class name and method signatures must match `native/jni/native_greeting_jni.cpp`.
 */
internal object NativeGreetingJni {
    init {
        System.loadLibrary("native_greeting_jni")
    }

    @JvmStatic
    external fun getNativeGreeting(): String
}
