#!/bin/bash

# Build script for iOS native library using COMBINED options
# This script uses OS64COMBINED for device+simulator fat library and SIMULATORARM64 for arm64 simulator
# Much simpler approach using the toolchain's built-in COMBINED functionality

set -e  # Exit on any error

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Script directory: $SCRIPT_DIR"

# Toolchain file path
TOOLCHAIN_FILE="$SCRIPT_DIR/ios.toolchain.cmake"

# Source directory
SRC_DIR="$SCRIPT_DIR/src"

echo "Building native_greeting for iOS platforms using COMBINED options..."

# Build 1: OS64COMBINED - creates FAT library with arm64 device + x86_64 simulator
echo "Building OS64COMBINED (arm64 device + x86_64 simulator fat library)..."
cmake -S "$SRC_DIR" -B "$SCRIPT_DIR/build/ios_combined" \
    -G Xcode \
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN_FILE" \
    -DPLATFORM=OS64COMBINED
cmake --build "$SCRIPT_DIR/build/ios_combined" --config Release
cmake --install "$SCRIPT_DIR/build/ios_combined" --config Release

echo "OS64COMBINED fat library built at: build/ios_combined/Release-iphoneos/libnative_greeting.a"

# Build 2: SIMULATORARM64 - arm64 simulator only
echo "Building SIMULATORARM64 (arm64 simulator)..."
cmake -S "$SRC_DIR" -B "$SCRIPT_DIR/build/ios_simulator_arm64" \
    -G Xcode \
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN_FILE" \
    -DPLATFORM=SIMULATORARM64
cmake --build "$SCRIPT_DIR/build/ios_simulator_arm64" --config Release

echo "SIMULATORARM64 library built at: build/ios_simulator_arm64/Release-iphonesimulator/libnative_greeting.a"

# Verify the library architectures
echo "Verifying OS64COMBINED fat library architectures:"
lipo -info "$SCRIPT_DIR/build/ios_combined/Release-iphoneos/libnative_greeting.a"

echo "Verifying SIMULATORARM64 library architectures:"
lipo -info "$SCRIPT_DIR/build/ios_simulator_arm64/Release-iphonesimulator/libnative_greeting.a"

echo "Build completed successfully!"
echo "Libraries built at:"
echo "  - Fat library (arm64 device + x86_64 simulator): build/ios_combined/Release-iphoneos/libnative_greeting.a"
echo "  - SIMULATORARM64 (arm64 simulator): build/ios_simulator_arm64/Release-iphonesimulator/libnative_greeting.a"
echo ""
echo "Target mapping:"
echo "  - iosArm64() → build/ios_combined/Release-iphoneos/libnative_greeting.a (arm64 slice)"
echo "  - iosX64() → build/ios_combined/Release-iphoneos/libnative_greeting.a (x86_64 slice)"  
echo "  - iosSimulatorArm64() → build/ios_simulator_arm64/Release-iphonesimulator/libnative_greeting.a"
