cmake_minimum_required(VERSION 3.22)
project(sleepcatcher_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# The planner without any Android code, so it also builds and runs on a PC.
add_library(sleepcore_lib STATIC sleepcore/cycle_planner.cpp)
target_include_directories(sleepcore_lib PUBLIC sleepcore)

if(ANDROID)
    # Loaded from Kotlin with System.loadLibrary("sleepcore").
    add_library(sleepcore SHARED jni_bridge.cpp)
    target_link_libraries(sleepcore sleepcore_lib)
else()
    enable_testing()
    add_subdirectory(tests)
endif()
