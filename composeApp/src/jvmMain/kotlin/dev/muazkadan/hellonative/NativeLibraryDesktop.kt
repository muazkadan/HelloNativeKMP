package dev.muazkadan.hellonative

import com.sun.jna.Library
import com.sun.jna.Native
import org.scijava.nativelib.NativeLoader
import kotlin.getValue

interface NativeLibraryDesktop: Library {
    fun getNativeGreeting(): String

    companion object {
        val INSTANCE by lazy { 
            try {
                // Try to load from system library path first
                NativeLoader.loadLibrary("native_greeting")
                Native.load("native_greeting", NativeLibraryDesktop::class.java)
            } catch (e: Exception) {
                println("Failed to load library via NativeLoader: ${e.message}")
                // Fallback to JNA's default loading mechanism
                Native.load("native_greeting", NativeLibraryDesktop::class.java)
            }
        }
    }
}