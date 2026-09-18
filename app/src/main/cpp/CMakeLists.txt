cmake_minimum_required(VERSION 3.22)
project(sleepcore LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Pure planning logic with no Android dependencies, so it can be built
# and unit-tested on a desktop machine.
add_library(sleepcore_lib STATIC sleepcore/cycle_planner.cpp)
target_include_directories(sleepcore_lib PUBLIC sleepcore)

if(NOT ANDROID)
    enable_testing()
    add_subdirectory(tests)
endif()
