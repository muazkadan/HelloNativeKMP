plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("cmake") {
            id = "hellonative.cmake"
            implementationClass = "dev.muazkadan.hellonative.buildlogic.CMakePlugin"
        }
    }
}
