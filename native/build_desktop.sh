#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"

rm -rf "$SCRIPT_DIR/build/desktop"

# Build shared library for desktop
cmake -S "$SRC_DIR" -B "$SCRIPT_DIR/build/desktop" \
    -DCMAKE_BUILD_TYPE=Release

cmake --build "$SCRIPT_DIR/build/desktop" --config Release

echo "Desktop library built at: build/desktop/libnative_greeting.dylib"