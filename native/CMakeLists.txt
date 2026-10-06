cmake_minimum_required(VERSION 3.22.1)
project(NativeGreeting VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

option(NATIVE_GREETING_JNI "Build the JNI bridge used by Android" OFF)
option(NATIVE_GREETING_SHARED "Build the core as a shared library for the Java FFM API (desktop)" OFF)

# Platform-independent core with a plain C API. Kotlin/Native links it statically
# through cinterop, the JNI bridge links it into a shared library, and desktop
# loads it directly as a shared library through the Java FFM API.
if(NATIVE_GREETING_SHARED)
    add_library(native_greeting SHARED src/native_greeting.cpp)
    target_compile_definitions(native_greeting PRIVATE NATIVE_GREETING_EXPORTS)
else()
    add_library(native_greeting STATIC src/native_greeting.cpp)
endif()
target_include_directories(native_greeting PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

if(NATIVE_GREETING_JNI)
    add_library(native_greeting_jni SHARED jni/native_greeting_jni.cpp)
    target_link_libraries(native_greeting_jni PRIVATE native_greeting)

endif()

# Used by the Gradle CMakeBuild tasks for iOS and desktop (Android packages via AGP).
include(GNUInstallDirs)
if(NATIVE_GREETING_JNI)
    install(TARGETS native_greeting_jni
        LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
        RUNTIME DESTINATION ${CMAKE_INSTALL_LIBDIR})
else()
    install(TARGETS native_greeting
        ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
        LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
        RUNTIME DESTINATION ${CMAKE_INSTALL_LIBDIR})
endif()
