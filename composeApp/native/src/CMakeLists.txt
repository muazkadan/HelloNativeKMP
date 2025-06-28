cmake_minimum_required(VERSION 3.10)
project(NativeGreeting)

set(CMAKE_CXX_STANDARD 11)

add_library(native_greeting SHARED native_greeting.cpp)