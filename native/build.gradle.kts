plugins {
    alias(libs.plugins.androidLibrary)
}

// Packages the JNI bridge (libnative_greeting_jni.so) for every Android ABI.
// Lives in its own module because com.android.kotlin.multiplatform.library
// does not support externalNativeBuild.
android {
    namespace = "dev.muazkadan.hellonative.nativelib"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        externalNativeBuild {
            cmake {
                arguments += "-DNATIVE_GREETING_JNI=ON"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }
}
