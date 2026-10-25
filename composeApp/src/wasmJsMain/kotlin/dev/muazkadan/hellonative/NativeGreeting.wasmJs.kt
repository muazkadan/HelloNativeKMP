package dev.muazkadan.hellonative

// The Rust library isn't compiled for the browser; see README for a wasm32 route.
actual fun nativeGreeting(): String = "Native Rust library isn't available on Web"
