cmake_minimum_required(VERSION 3.22.1)
project(NativeGreeting VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

option(NATIVE_GREETING_JNI "Build the JNI bridge used by Android and desktop JVM" OFF)

# Platform-independent core with a plain C API. Kotlin/Native links it
# statically through cinterop; the JNI bridge links it into a shared library.
add_library(native_greeting STATIC src/native_greeting.cpp)
target_include_directories(native_greeting PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

if(NATIVE_GREETING_JNI)
    add_library(native_greeting_jni SHARED jni/native_greeting_jni.cpp)
    target_link_libraries(native_greeting_jni PRIVATE native_greeting)

    # The NDK sysroot already provides jni.h; desktop needs the JDK headers.
    if(NOT ANDROID)
        find_package(JNI REQUIRED)
        target_include_directories(native_greeting_jni PRIVATE ${JNI_INCLUDE_DIRS})
    endif()
endif()
