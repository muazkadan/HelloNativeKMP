plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("cargo") {
            id = "hellonative.cargo"
            implementationClass = "dev.muazkadan.hellonative.buildlogic.CargoPlugin"
        }
    }
}
