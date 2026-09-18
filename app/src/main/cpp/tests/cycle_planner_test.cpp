#include <gtest/gtest.h>

#include "cycle_planner.h"

using namespace sleepcore;

namespace {

// Times are minutes since an arbitrary midnight, so at(9) is 09:00.
constexpr int64_t kMs = 60 * 1000;
int64_t at(int hour, int minute = 0) { return (hour * 60 + minute) * kMs; }

PlanRequest request(Mode mode, int64_t onset, int64_t target) {
    PlanRequest req;
    req.mode = mode;
    req.onsetMs = onset;
    req.targetMs = target;
    return req;
}

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

TEST(Refreshed, PicksLatestBoundaryNotAfterTarget) {
    // Sleep starts 1:15, boundaries 2:45 4:15 5:45 7:15 8:45 10:15.
    const Plan plan = buildPlan(request(Mode::Refreshed, at(1), at(9)));
    ASSERT_EQ(plan.signals.size(), 1u);
    EXPECT_EQ(plan.signals[0].atMs, at(8, 45));
    EXPECT_EQ(plan.signals[0].kind, SignalKind::LoudAlarm);
}

TEST(Refreshed, BoundaryExactlyOnTargetIsUsed) {
    const Plan plan = buildPlan(request(Mode::Refreshed, at(1), at(8, 45)));
    ASSERT_EQ(plan.signals.size(), 1u);
    EXPECT_EQ(plan.signals[0].atMs, at(8, 45));
}

TEST(Refreshed, ShortNightExtendsPastTargetToMinimumCycles) {
    // Only one full cycle fits before 6:00, so the plan takes three cycles instead.
    const Plan plan = buildPlan(request(Mode::Refreshed, at(4), at(6)));
    ASSERT_EQ(plan.signals.size(), 1u);
    EXPECT_EQ(plan.signals[0].atMs, at(8, 45));
}

TEST(Refreshed, TargetBeforeSleepStartStillGivesMinimumCycles) {
    const Plan plan = buildPlan(request(Mode::Refreshed, at(4), at(4, 5)));
    ASSERT_EQ(plan.signals.size(), 1u);
    EXPECT_EQ(plan.signals[0].atMs, at(8, 45));
}

TEST(Tuning, CustomCycleLengthAndLatencyAreRespected) {
    PlanRequest req = request(Mode::Refreshed, at(0), at(6));
    req.tuning.cycleMinutes = 100;
    req.tuning.fallAsleepMinutes = 20;
    req.tuning.minCycles = 1;
    // Sleep starts 0:20, boundaries 2:00 3:40 5:20 7:00 -> 5:20 is the last one before 6:00.
    EXPECT_EQ(buildPlan(req).signals[0].atMs, at(5, 20));
}
