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

TEST(CatchDream, SoftPulseOnLastBoundaryThenLoudAlarmAtTarget) {
    // 8:45 is only 15 min before the target, closer than the 20 min gap, so 7:15 is used.
    const Plan plan = buildPlan(request(Mode::CatchDream, at(1), at(9)));
    ASSERT_EQ(plan.signals.size(), 2u);
    EXPECT_EQ(plan.signals[0].atMs, at(7, 15));
    EXPECT_EQ(plan.signals[0].kind, SignalKind::SoftPulse);
    EXPECT_EQ(plan.signals[1].atMs, at(9));
    EXPECT_EQ(plan.signals[1].kind, SignalKind::LoudAlarm);
}

TEST(CatchDream, BoundaryFarEnoughBeforeTargetIsKept) {
    // Target 9:10: boundary 8:45 is 25 min earlier, which respects the gap.
    const Plan plan = buildPlan(request(Mode::CatchDream, at(1), at(9, 10)));
    ASSERT_EQ(plan.signals.size(), 2u);
    EXPECT_EQ(plan.signals[0].atMs, at(8, 45));
    EXPECT_EQ(plan.signals[1].atMs, at(9, 10));
}

TEST(CatchDream, BoundaryExactlyAtLatestSoftIsUsed) {
    // Target 9:05 minus the 20 min gap lands exactly on the 8:45 boundary.
    const Plan plan = buildPlan(request(Mode::CatchDream, at(1), at(9, 5)));
    ASSERT_EQ(plan.signals.size(), 2u);
    EXPECT_EQ(plan.signals[0].atMs, at(8, 45));
}

TEST(CatchDream, LoudAlarmNeverLaterThanTarget) {
    for (int minute : {0, 7, 30, 59}) {
        const Plan plan = buildPlan(request(Mode::CatchDream, at(0, 30), at(8, minute)));
        EXPECT_EQ(plan.signals.back().atMs, at(8, minute));
        EXPECT_EQ(plan.signals.back().kind, SignalKind::LoudAlarm);
    }
}

TEST(CatchDream, NoSoftPulseWhenNightIsTooShort) {
    const Plan plan = buildPlan(request(Mode::CatchDream, at(5), at(7)));
    ASSERT_EQ(plan.signals.size(), 1u);
    EXPECT_EQ(plan.signals[0].kind, SignalKind::LoudAlarm);
    EXPECT_EQ(plan.signals[0].atMs, at(7));
}

TEST(CatchDream, SoftGapLongerThanCycleStillWorks) {
    PlanRequest req = request(Mode::CatchDream, at(1), at(9));
    req.tuning.softGapMinutes = 120;
    // Latest soft moment is 7:00, so the pulse drops back to the 5:45 boundary.
    const Plan plan = buildPlan(req);
    ASSERT_EQ(plan.signals.size(), 2u);
    EXPECT_EQ(plan.signals[0].atMs, at(5, 45));
    EXPECT_EQ(plan.signals[1].atMs, at(9));
}

TEST(CatchDream, TargetBeforeOnsetFallsBackToMinimumCycles) {
    const Plan plan = buildPlan(request(Mode::CatchDream, at(4), at(3)));
    ASSERT_EQ(plan.signals.size(), 1u);
    EXPECT_EQ(plan.signals[0].atMs, at(8, 45));
    EXPECT_EQ(plan.signals[0].kind, SignalKind::LoudAlarm);
}

TEST(CatchDream, TargetAtSleepStartFallsBack) {
    // Onset 4:00 plus the 15 min latency is exactly the target.
    const Plan plan = buildPlan(request(Mode::CatchDream, at(4), at(4, 15)));
    ASSERT_EQ(plan.signals.size(), 1u);
    EXPECT_EQ(plan.signals[0].atMs, at(8, 45));
}

TEST(CatchDream, TargetInsideFirstCycleKeepsSafetyAlarm) {
    // A short nap: no room for a soft pulse, but the alarm stays where asked.
    const Plan plan = buildPlan(request(Mode::CatchDream, at(4), at(4, 40)));
    ASSERT_EQ(plan.signals.size(), 1u);
    EXPECT_EQ(plan.signals[0].atMs, at(4, 40));
    EXPECT_EQ(plan.signals[0].kind, SignalKind::LoudAlarm);
}

TEST(NaturalWake, WarnsWhenFinalSignalIsAfterNaturalWakeTime) {
    PlanRequest req = request(Mode::Refreshed, at(4), at(6));   // alarm lands at 8:45
    req.naturalWakeMs = at(8);
    EXPECT_TRUE(buildPlan(req).naturalWakeWarning);

    req.naturalWakeMs = at(11);
    EXPECT_FALSE(buildPlan(req).naturalWakeWarning);

    req.naturalWakeMs = 0;   // unknown, never warn
    EXPECT_FALSE(buildPlan(req).naturalWakeWarning);
}

TEST(NaturalWake, CatchDreamUsesLoudAlarmForTheCheck) {
    PlanRequest req = request(Mode::CatchDream, at(1), at(12));   // soft pulse 10:15, loud 12:00
    req.naturalWakeMs = at(11);
    EXPECT_TRUE(buildPlan(req).naturalWakeWarning);

    req.naturalWakeMs = at(10, 30);   // between the pulse and the alarm: still only the alarm counts
    EXPECT_TRUE(buildPlan(req).naturalWakeWarning);
}

TEST(NaturalWake, EqualToNaturalWakeDoesNotWarn) {
    PlanRequest req = request(Mode::CatchDream, at(1), at(11));
    req.naturalWakeMs = at(11);
    EXPECT_FALSE(buildPlan(req).naturalWakeWarning);
}

TEST(Sanitize, ZeroCycleFallsBackToDefault) {
    Tuning t;
    t.cycleMinutes = 0;
    EXPECT_EQ(sanitize(t).cycleMinutes, 90);
}

TEST(Sanitize, NegativeCycleFallsBackToDefault) {
    Tuning t;
    t.cycleMinutes = -90;
    EXPECT_EQ(sanitize(t).cycleMinutes, 90);
}

TEST(Sanitize, NegativeLatencyBecomesZero) {
    Tuning t;
    t.fallAsleepMinutes = -15;
    EXPECT_EQ(sanitize(t).fallAsleepMinutes, 0);
}

TEST(Sanitize, ZeroMinCyclesBecomesOne) {
    Tuning t;
    t.minCycles = 0;
    EXPECT_EQ(sanitize(t).minCycles, 1);
}

TEST(Sanitize, NegativeSoftGapBecomesZero) {
    Tuning t;
    t.softGapMinutes = -30;
    EXPECT_EQ(sanitize(t).softGapMinutes, 0);
}

TEST(Sanitize, BuildPlanWithZeroCycleDoesNotCrash) {
    PlanRequest broken = request(Mode::CatchDream, at(1), at(9));
    broken.tuning.cycleMinutes = 0;
    const Plan expected = buildPlan(request(Mode::CatchDream, at(1), at(9)));
    const Plan actual = buildPlan(broken);
    ASSERT_EQ(actual.signals.size(), expected.signals.size());
    for (size_t i = 0; i < expected.signals.size(); ++i) {
        EXPECT_EQ(actual.signals[i].atMs, expected.signals[i].atMs);
        EXPECT_EQ(actual.signals[i].kind, expected.signals[i].kind);
    }
}

TEST(Tuning, CustomCycleLengthAndLatencyAreRespected) {
    PlanRequest req = request(Mode::Refreshed, at(0), at(6));
    req.tuning.cycleMinutes = 100;
    req.tuning.fallAsleepMinutes = 20;
    req.tuning.minCycles = 1;
    // Sleep starts 0:20, boundaries 2:00 3:40 5:20 7:00 -> 5:20 is the last one before 6:00.
    EXPECT_EQ(buildPlan(req).signals[0].atMs, at(5, 20));
}
