plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "dev.muazkadan.hellonative.nativelib"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    externalNativeBuild {
        cmake {
            path = file("src/CMakeLists.txt")
        }
    }
}
