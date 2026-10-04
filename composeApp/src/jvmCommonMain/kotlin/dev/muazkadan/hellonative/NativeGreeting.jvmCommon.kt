package dev.muazkadan.hellonative

import dev.muazkadan.hellonative.nativeinterop.NativeGreetingJni

actual fun nativeGreeting(): String = NativeGreetingJni.getNativeGreeting()
