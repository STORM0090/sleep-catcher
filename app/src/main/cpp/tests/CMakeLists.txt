# GoogleTest, set up the way its README shows.
include(FetchContent)
FetchContent_Declare(
    googletest
    URL https://github.com/google/googletest/releases/download/v1.17.0/googletest-1.17.0.tar.gz
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE  # only silences a warning in newer CMake
)
# For Windows: Prevent overriding the parent project's compiler/linker settings
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

add_executable(cycle_planner_test cycle_planner_test.cpp plan_invariants_test.cpp)
target_link_libraries(cycle_planner_test sleepcore_lib GTest::gtest_main)

include(GoogleTest)
gtest_discover_tests(cycle_planner_test)
