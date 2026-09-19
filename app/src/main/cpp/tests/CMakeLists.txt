include(FetchContent)
FetchContent_Declare(
    googletest
    URL https://github.com/google/googletest/releases/download/v1.17.0/googletest-1.17.0.tar.gz
    URL_HASH SHA256=65fab701d9829d38cb77c14acdc431d2108bfdbf8979e40eb8ae567edf10b27c
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE
)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

add_executable(cycle_planner_test
    cycle_planner_test.cpp
    plan_invariants_test.cpp
)
target_link_libraries(cycle_planner_test PRIVATE sleepcore_lib GTest::gtest_main)

include(GoogleTest)
# Listing the tests at test time instead of build time keeps the build from
# depending on the runtime libraries being on PATH.
gtest_discover_tests(cycle_planner_test DISCOVERY_MODE PRE_TEST)
