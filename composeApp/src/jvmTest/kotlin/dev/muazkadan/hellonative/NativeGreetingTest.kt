package dev.muazkadan.hellonative

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeGreetingTest {

    @Test
    fun greetingComesFromRustLibrary() {
        assertEquals("Hello from Rust", nativeGreeting())
    }
}
