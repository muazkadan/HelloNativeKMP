package dev.muazkadan.hellonative.nativeinterop

// The :native module packages the library into the APK for every ABI.
internal actual fun loadNativeGreetingLibrary() {
    System.loadLibrary(NATIVE_GREETING_LIBRARY)
}
