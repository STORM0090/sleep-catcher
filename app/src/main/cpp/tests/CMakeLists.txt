include(FetchContent)
FetchContent_Declare(
    googletest
    URL https://github.com/google/googletest/archive/refs/tags/v1.17.0.zip
)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

add_executable(cycle_planner_test cycle_planner_test.cpp)
target_link_libraries(cycle_planner_test PRIVATE sleepcore_lib GTest::gtest_main)

include(GoogleTest)
gtest_discover_tests(cycle_planner_test)
