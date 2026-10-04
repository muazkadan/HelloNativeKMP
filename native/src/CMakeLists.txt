cmake_minimum_required(VERSION 3.10)
project(NativeGreeting)

set(CMAKE_CXX_STANDARD 11)

# Build static library for iOS, shared for other platforms
if(IOS)
    add_library(native_greeting STATIC 
        native_greeting.cpp 
        native_greeting.h
    )
else()
    add_library(native_greeting SHARED 
        native_greeting.cpp 
        native_greeting.h
    )
endif()