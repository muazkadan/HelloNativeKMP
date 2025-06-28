package dev.muazkadan.hellonative

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform