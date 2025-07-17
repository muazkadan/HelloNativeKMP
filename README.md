This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that's common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple's CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you're sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

## Building Targets with Native Components

This project includes native C++ libraries that are integrated differently for each platform.

### Android Build

Android targets build the native C++ library automatically using CMake integration:

- **Automatic Build**: The native library is built automatically when you build the Android target
- **CMake Integration**: Uses `externalNativeBuild` in `build.gradle.kts` pointing to `native/src/CMakeLists.txt`
- **JNA Interface**: Uses Java Native Access (JNA) to interface with the native library instead of cinterop
- **No Manual Steps**: Simply run `./gradlew assembleDebug` or build through Android Studio

The Android implementation loads the native library at runtime using:
```kotlin
Native.load("native_greeting", NativeLibraryAndroid::class.java)
```

To build the Android target:
```bash
# Debug build
./gradlew :composeApp:assembleDebug

# Or open in Android Studio
open -a "Android Studio" .
```

### Prerequisites for iOS Build

- Xcode (with command line tools)
- CMake 3.14+ 
- iOS Toolchain for CMake

### iOS Build - Manual Native Library Build

Unlike Android, iOS targets require manual building of the native C++ libraries before building the iOS app:

```bash
cd composeApp/native
chmod +x build_ios.sh
./build_ios.sh
```

This script will:
- Build a fat library for iOS device (arm64) and iOS simulator (x86_64) using `OS64COMBINED`
- Build a separate library for iOS simulator arm64 using `SIMULATORARM64`
- Create the necessary library files that Kotlin/Native cinterop requires

The built libraries will be placed at:
- `build/ios_combined/Release-iphoneos/libnative_greeting.a` - Fat library for device and x86_64 simulator
- `build/ios_simulator_arm64/Release-iphonesimulator/libnative_greeting.a` - ARM64 simulator library

### Desktop Build - Manual Native Library Build

Similar to iOS, desktop targets require manual building of the native C++ libraries:

```bash
cd composeApp/native
chmod +x build_desktop.sh
./build_desktop.sh
```

This script will:
- Build a shared library for the desktop platform (.dylib on macOS, .so on Linux, .dll on Windows)
- Use standard CMake build process without custom toolchains
- Create the library file that JNA requires for runtime loading

The built library will be placed at:
- `build/desktop/libnative_greeting.dylib` (macOS)
- `build/desktop/libnative_greeting.so` (Linux) 
- `build/desktop/native_greeting.dll` (Windows)

### Running Desktop Target

After building the native library, you can run the desktop target:

```bash
# Run desktop application
./gradlew :composeApp:run

# Or build desktop distribution
./gradlew :composeApp:createDistributable
```

**Note**: The desktop implementation uses JNA (Java Native Access) with `native-lib-loader` for simplified cross-platform library loading. The library is automatically detected from the system library path without manual path construction.

### Running iOS Targets

After building the native libraries, you can run the iOS targets:

```bash
# For iOS device/simulator
./gradlew :composeApp:iosSimulatorArm64Test
./gradlew :composeApp:iosX64Test 
./gradlew :composeApp:iosArm64Test

# Or open the iOS project in Xcode
open iosApp/iosApp.xcodeproj
```

**Note**: Always build the native libraries first before running any iOS-related Gradle tasks, as the Kotlin/Native cinterop depends on these compiled libraries.

## Additional Resources

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.