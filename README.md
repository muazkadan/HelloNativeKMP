# HelloNativeKMP

A Compose Multiplatform app (Android, iOS, Desktop, Web) that calls one shared C++ library from
Kotlin. Each platform uses its officially recommended interop mechanism, and Gradle builds the
native code, so a plain `./gradlew` build or an Xcode build is all you need.

| Platform | Interop | How the C++ is built and shipped |
|---|---|---|
| iOS | Kotlin/Native **cinterop** against a static library | `composeApp:buildNativeIos*` runs CMake before cinterop; the `.a` is embedded in the klib |
| Android | **JNI** | `:native` builds `libnative_greeting_jni.so` for every ABI with AGP's `externalNativeBuild` |
| Desktop (JVM) | **JNI** (same binding as Android) | `composeApp:buildNativeDesktop` runs CMake; `desktopApp` ships the library as Compose app resources |
| Web (Wasm) | — | No native code; see [Web](#web) |

## Project layout

```
native/        C++ sources + CMake, and the Android library module that packages the JNI .so
  include/       Public C API (native_greeting.h), used by cinterop and the JNI bridge
  src/           Platform-independent core
  jni/           JNI bridge, registers methods with RegisterNatives in JNI_OnLoad
composeApp/    Shared KMP library (UI + interop), uses com.android.kotlin.multiplatform.library
  commonMain/      expect fun nativeGreeting(): String
  jvmCommonMain/   Single JNI binding shared by androidMain and jvmMain
  androidMain/     System.loadLibrary
  jvmMain/         Loads the library from Compose Desktop app resources
  iosMain/         Calls the cinterop bindings
androidApp/    Android application entry point
desktopApp/    Desktop entry point and packaging (DMG/MSI/DEB)
webApp/        Kotlin/Wasm entry point
iosApp/        Xcode project
build-logic/   CMakeBuild Gradle task used for the iOS and desktop native builds
```

### Why this setup

- **cinterop on iOS** is the only first-class way to call C from Kotlin/Native.
- **JNI on Android and desktop** is the official NDK path. It needs no runtime dependency (JNA
  ships its own `libjnidispatch.so` for each ABI and adds marshalling overhead to every call), and
  one binding in `jvmCommonMain` serves both targets. The Java FFM API (JDK 22+) would also work on
  desktop, but Android doesn't support it, so you'd need a second binding.
- **`RegisterNatives` in `JNI_OnLoad`**, with hidden symbol visibility, means `JNI_OnLoad` is the
  only exported symbol and Kotlin names never leak into C++ symbol names. AGP's default R8 rules
  keep classes that declare `native` methods, so release builds are minified safely.
- **The `:native` module is separate** because the Android-KMP library plugin doesn't support
  `externalNativeBuild`.

## Prerequisites

- JDK 21. Gradle resolves it automatically through `gradle/gradle-daemon-jvm.properties` and toolchains.
- Android SDK with NDK `29.0.14206865` (see `android-ndk` in `gradle/libs.versions.toml`).
  NDK r28+ produces 16 KB-aligned libraries, which Google Play requires.
- CMake 3.22.1+. Gradle uses, in order: the `cmake.executable` Gradle property, `cmake` on `PATH`,
  or the newest CMake installed in the Android SDK.
- Xcode, for iOS (Apple Silicon simulators only; `iosX64` isn't supported by Compose Multiplatform anymore).

## Build and run

### Android

```bash
./gradlew :androidApp:assembleDebug
```

Or run the `androidApp` configuration from Android Studio.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run. The Xcode build phase calls
`:composeApp:embedAndSignAppleFrameworkForXcode`, which builds the C++ library first.

### Desktop

```bash
./gradlew :desktopApp:run
```

To build a distributable for the current OS (the native library is bundled automatically):

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

The native library is built for the host OS and architecture only, so build each installer on its
target platform (for example, with a CI matrix).

### Web

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

The C++ library isn't compiled for the browser, so `nativeGreeting()` returns a placeholder on Web.
To share the C++ code there too, compile it to WebAssembly with Emscripten and call it from
`wasmJsMain` through JS interop.

## Tests

```bash
./gradlew :composeApp:allTests
```

`NativeGreetingTest` calls into the real C++ library on the JVM (through JNI, loading the library the
same way the packaged desktop app does) and on the iOS simulator (through cinterop).

## Adding native functions

1. Declare the function in `native/include/native_greeting.h` and implement it in `native/src/`.
2. **iOS:** it's available right away through `dev.muazkadan.hellonative.cinterop`.
3. **Android/desktop:** add an `external fun` to `NativeGreetingJni` in `jvmCommonMain`, then add
   the matching wrapper and `JNINativeMethod` entry to `native/jni/native_greeting_jni.cpp`.
4. Expose it to shared code with an `expect`/`actual` declaration.
