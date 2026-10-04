package dev.muazkadan.hellonative

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeGreetingTest {

    @Test
    fun greetingComesFromCppLibrary() {
        assertEquals("Hello from C++", nativeGreeting())
    }
}
