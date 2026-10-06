package dev.muazkadan.hellonative

// The C++ library isn't compiled for the browser; see README for an Emscripten route.
actual fun nativeGreeting(): String = "Native C++ library isn't available on Web"
