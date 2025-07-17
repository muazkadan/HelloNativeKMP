#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"

rm -rf "$SCRIPT_DIR/build"

# Build function
build_ios() {
    local name=$1
    local arch=$2
    local sysroot=$3
    
    cmake -S "$SRC_DIR" -B "$SCRIPT_DIR/build/$name" -G Xcode \
        -DCMAKE_SYSTEM_NAME=iOS \
        -DCMAKE_OSX_ARCHITECTURES="$arch" \
        -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
        ${sysroot:+-DCMAKE_OSX_SYSROOT="$sysroot"}
    
    cmake --build "$SCRIPT_DIR/build/$name" --config Release
}

# Build all targets
build_ios ios_device arm64
build_ios ios_simulator_x64 x86_64 iphonesimulator
build_ios ios_simulator_arm64 arm64 iphonesimulator

echo "Build completed successfully!"
