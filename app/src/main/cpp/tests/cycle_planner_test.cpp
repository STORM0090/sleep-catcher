#include <gtest/gtest.h>

#include "cycle_planner.h"

using namespace sleepcore;

namespace {

// Times are minutes since an arbitrary midnight, so at(9) is 09:00.
constexpr int64_t kMs = 60 * 1000;
int64_t at(int hour, int minute = 0) { return (hour * 60 + minute) * kMs; }

}  // namespace

TEST(CycleBoundaries, StartAfterLatencyAndRepeatEveryCycle) {
    const auto b = cycleBoundaries(at(0), Tuning{}, 3);
    ASSERT_EQ(b.size(), 3u);
    EXPECT_EQ(b[0], at(1, 45));
    EXPECT_EQ(b[1], at(3, 15));
    EXPECT_EQ(b[2], at(4, 45));
}

TEST(CycleBoundaries, ZeroCountGivesEmptyList) {
    EXPECT_TRUE(cycleBoundaries(at(1), Tuning{}, 0).empty());
}

TEST(CycleBoundaries, NegativeCountGivesEmptyList) {
    EXPECT_TRUE(cycleBoundaries(at(1), Tuning{}, -1).empty());
}
