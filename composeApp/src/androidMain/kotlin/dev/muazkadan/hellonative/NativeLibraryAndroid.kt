package dev.muazkadan.hellonative

import com.sun.jna.Library
import com.sun.jna.Native
import kotlin.getValue

interface NativeLibraryAndroid: Library {
    fun getNativeGreeting(): String

    companion object {
        val INSTANCE by lazy { Native.load("native_greeting", NativeLibraryAndroid::class.java) }
    }
}