# HelloNativeKMP

A Compose Multiplatform app (Android, iOS, Desktop, Web) that calls one shared Rust library from
Kotlin. Each platform uses its officially recommended interop mechanism, and Gradle drives Cargo,
so a plain `./gradlew` build or an Xcode build is all you need.

> This is the `experimental/rust-native` branch. `main` has the same app backed by a C++ library;
> the Kotlin interop layer is identical on both branches.

| Platform | Interop | How the Rust is built and shipped |
|---|---|---|
| iOS | Kotlin/Native **cinterop** against a static library | `composeApp:buildNativeIos*` runs Cargo before cinterop; the `.a` is embedded in the klib |
| Android | **JNI** | `native:buildNativeAndroid` runs Cargo with the NDK linkers for every ABI; AGP packages the output as `jniLibs` |
| Desktop (JVM) | **JNI** (same binding as Android) | `composeApp:buildNativeDesktop` runs Cargo; `desktopApp` ships the library as Compose app resources |
| Web (Wasm) | — | No native code; see [Web](#web) |

## Project layout

```
native/        Cargo workspace, and the Android library module that packages the JNI .so
  core/          Platform-independent core (native_greeting_core, rlib)
  c/             Plain C API for cinterop (native_greeting, staticlib)
  include/       Header for the C API (native_greeting.h)
  jni/           JNI bridge (native_greeting_jni, cdylib), registers methods with RegisterNatives in JNI_OnLoad
  .cargo/        Cargo config (16 KB page alignment for Android)
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
build-logic/   CargoBuild Gradle task used for the iOS, Android and desktop native builds
```

### Why this setup

- **cinterop on iOS** is the only first-class way to call C from Kotlin/Native. Rust exposes the
  same C ABI through `extern "C"` functions, so the `.def` file and the Kotlin code are unchanged.
- **JNI on Android and desktop** is the official NDK path. It needs no runtime dependency (JNA
  ships its own `libjnidispatch.so` for each ABI and adds marshalling overhead to every call), and
  one binding in `jvmCommonMain` serves both targets. The Java FFM API (JDK 22+) would also work on
  desktop, but Android doesn't support it, so you'd need a second binding.
- **`RegisterNatives` in `JNI_OnLoad`** means `JNI_OnLoad` is the only exported symbol and Kotlin
  names never leak into Rust symbol names. The [`jni`](https://crates.io/crates/jni) crate's
  `native_method!` macro derives each Java name and signature from the Rust declaration and turns
  errors and panics into Java exceptions. AGP's default R8 rules keep classes that declare `native`
  methods, so release builds are minified safely.
- **The C API is its own crate (`c/`).** A `cdylib` re-exports every `#[no_mangle]` symbol of its
  dependencies, so if the C API lived in `core/` the JNI library would export it too.
- **`panic = "abort"`** in the release profile, because unwinding across the C or JNI boundary is
  undefined behavior.
- **Cargo, not cargo-ndk.** `build-logic` points Cargo at the NDK's clang for each Android target
  (`CARGO_TARGET_<TRIPLE>_LINKER`), so the only extra tools are `rustup` targets.
- **The `:native` module is separate** so the Android-only packaging stays out of the KMP library.

## Prerequisites

- JDK 21. Gradle resolves it automatically through `gradle/gradle-daemon-jvm.properties` and toolchains.
- Rust (stable, 1.85+ for edition 2024) through [rustup](https://rustup.rs), plus the targets:

  ```bash
  rustup target add aarch64-apple-ios aarch64-apple-ios-sim aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
  ```

  Gradle uses, in order: the `cargo.executable` Gradle property, `cargo` on `PATH`,
  `$CARGO_HOME/bin/cargo`, or `~/.cargo/bin/cargo`. The fallbacks matter for Xcode builds, which
  don't inherit your shell `PATH`.
- Android SDK with NDK `29.0.14206865` (see `android-ndk` in `gradle/libs.versions.toml`). Cargo
  links Android libraries with the NDK's clang.
- Xcode, for iOS (Apple Silicon simulators only; `iosX64` isn't supported by Compose Multiplatform anymore).

## Build and run

### Android

```bash
./gradlew :androidApp:assembleDebug
```

Or run the `androidApp` configuration from Android Studio.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run. The Xcode build phase calls
`:composeApp:embedAndSignAppleFrameworkForXcode`, which builds the Rust library first.

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

The Rust library isn't compiled for the browser, so `nativeGreeting()` returns a placeholder on Web.
To share the Rust code there too, compile `core/` for `wasm32-unknown-unknown` (for example with
`wasm-bindgen`) and call it from `wasmJsMain` through JS interop.

## Tests

```bash
./gradlew :composeApp:allTests
```

`NativeGreetingTest` calls into the real Rust library on the JVM (through JNI, loading the library the
same way the packaged desktop app does) and on the iOS simulator (through cinterop).

## Adding native functions

1. Implement the logic in `native/core/src/lib.rs`.
2. **iOS:** add an `extern "C"` wrapper to `native/c/src/lib.rs` and declare it in
   `native/include/native_greeting.h`. It's then available through `dev.muazkadan.hellonative.cinterop`.
3. **Android/desktop:** add an `external fun` to `NativeGreetingJni` in `jvmCommonMain`, then add
   a `native_method!` entry and its implementation to `native/jni/src/lib.rs`.
4. Expose it to shared code with an `expect`/`actual` declaration.
