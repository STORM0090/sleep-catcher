cmake_minimum_required(VERSION 3.22)
project(sleepcore LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Makefile and Ninja builds leave the build type empty, which compiles
# with no flags at all. Pick one so a plain configure behaves sensibly.
get_property(isMultiConfig GLOBAL PROPERTY GENERATOR_IS_MULTI_CONFIG)
if(NOT isMultiConfig AND NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Debug CACHE STRING "Build type" FORCE)
endif()

# Pure planning logic with no Android dependencies, so it can be built
# and unit-tested on a desktop machine.
add_library(sleepcore_lib STATIC sleepcore/cycle_planner.cpp)
target_include_directories(sleepcore_lib PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/sleepcore)
# Required to fold this static library into a shared one on Android.
set_target_properties(sleepcore_lib PROPERTIES POSITION_INDEPENDENT_CODE ON)

if(NOT CMAKE_SYSTEM_NAME STREQUAL "Android")
    enable_testing()
    add_subdirectory(tests)
endif()
