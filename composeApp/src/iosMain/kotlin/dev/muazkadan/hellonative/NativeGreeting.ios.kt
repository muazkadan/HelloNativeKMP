package dev.muazkadan.hellonative

import dev.muazkadan.hellonative.cinterop.getNativeGreeting
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
actual fun nativeGreeting(): String = requireNotNull(getNativeGreeting()).toKString()
