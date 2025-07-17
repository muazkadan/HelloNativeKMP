package dev.muazkadan.hellonative

import com.sun.jna.Library
import com.sun.jna.Native
import java.io.File
import kotlin.getValue

interface NativeLibraryDesktop: Library {
    fun getNativeGreeting(): String

    companion object {
        val INSTANCE by lazy { 
            // Get the path to the native library
            val projectDir = System.getProperty("user.dir")
            val osName = System.getProperty("os.name").lowercase()
            val libraryName = when {
                osName.contains("mac") -> "libnative_greeting.dylib"
                osName.contains("win") -> "native_greeting.dll"
                else -> "libnative_greeting.so"
            }
            
            // Check if we're already in composeApp directory or need to go up
            val libraryPath = if (projectDir.endsWith("composeApp")) {
                File(projectDir, "native/build/desktop/$libraryName").absolutePath
            } else {
                File(projectDir, "composeApp/native/build/desktop/$libraryName").absolutePath
            }
            
            println("Loading native library from: $libraryPath")
            Native.load(libraryPath, NativeLibraryDesktop::class.java) 
        }
    }
}