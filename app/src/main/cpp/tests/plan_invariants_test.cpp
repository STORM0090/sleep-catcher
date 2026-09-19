#include <gtest/gtest.h>

#include <algorithm>
#include <string>

#include "cycle_planner.h"

using namespace sleepcore;

namespace {

constexpr int64_t kMinute = 60 * 1000;

int64_t minutes(int64_t count) { return count * kMinute; }

// Going to bed between 22:00 and 04:00, waking up between 05:00 and 12:00.
constexpr int64_t kFirstOnsetMinute = 22 * 60;
constexpr int64_t kLastOnsetMinute = 28 * 60;
constexpr int64_t kFirstTargetMinute = 29 * 60;
constexpr int64_t kLastTargetMinute = 36 * 60;

PlanRequest gridRequest(int64_t onsetMs, int64_t targetMs, Mode mode, int softGapMinutes) {
    PlanRequest req;
    req.onsetMs = onsetMs;
    req.targetMs = targetMs;
    req.mode = mode;
    req.tuning.softGapMinutes = softGapMinutes;
    return req;
}

std::string describe(int64_t onsetMs, int64_t targetMs, Mode mode, int softGapMinutes) {
    return "onset=" + std::to_string(onsetMs / kMinute) + "min target=" + std::to_string(targetMs / kMinute) +
           "min mode=" + std::to_string(static_cast<int>(mode)) + " softGap=" + std::to_string(softGapMinutes);
}

}  // namespace

// Instead of checking one night at a time, walk over many nights and assert
// the properties every plan has to hold, whatever the numbers are.
TEST(PlanInvariants, EveryPlanIsUsable) {
    for (int64_t onsetMinute = kFirstOnsetMinute; onsetMinute <= kLastOnsetMinute; onsetMinute += 7) {
        for (int64_t targetMinute = kFirstTargetMinute; targetMinute <= kLastTargetMinute; targetMinute += 11) {
            for (Mode mode : {Mode::Refreshed, Mode::CatchDream}) {
                for (int softGap : {0, 20, 120}) {
                    const int64_t onset = minutes(onsetMinute);
                    const int64_t target = minutes(targetMinute);
                    SCOPED_TRACE(describe(onset, target, mode, softGap));

                    const Plan plan = buildPlan(gridRequest(onset, target, mode, softGap));
                    ASSERT_FALSE(plan.signals.empty());
                    EXPECT_EQ(plan.signals.back().kind, SignalKind::LoudAlarm);
                    for (size_t i = 0; i < plan.signals.size(); ++i) {
                        EXPECT_GT(plan.signals[i].atMs, onset);
                        if (i > 0) EXPECT_LT(plan.signals[i - 1].atMs, plan.signals[i].atMs);
                    }
                }
            }
        }
    }
}

TEST(PlanInvariants, RefreshedAlwaysWakesOnACycleBoundary) {
    for (int64_t onsetMinute = kFirstOnsetMinute; onsetMinute <= kLastOnsetMinute; onsetMinute += 7) {
        for (int64_t targetMinute = kFirstTargetMinute; targetMinute <= kLastTargetMinute; targetMinute += 11) {
            const int64_t onset = minutes(onsetMinute);
            const int64_t target = minutes(targetMinute);
            SCOPED_TRACE(describe(onset, target, Mode::Refreshed, 20));

            const Plan plan = buildPlan(gridRequest(onset, target, Mode::Refreshed, 20));
            const std::vector<int64_t> boundaries = cycleBoundaries(onset, Tuning{}, 20);
            ASSERT_EQ(plan.signals.size(), 1u);
            EXPECT_NE(std::find(boundaries.begin(), boundaries.end(), plan.signals[0].atMs), boundaries.end());
        }
    }
}

TEST(PlanInvariants, LaterTargetNeverWakesYouEarlier) {
    for (Mode mode : {Mode::Refreshed, Mode::CatchDream}) {
        int64_t previousWake = 0;
        for (int64_t targetMinute = kFirstTargetMinute; targetMinute <= kLastTargetMinute; targetMinute += 11) {
            const int64_t target = minutes(targetMinute);
            SCOPED_TRACE(describe(minutes(23 * 60), target, mode, 20));

            const Plan plan = buildPlan(gridRequest(minutes(23 * 60), target, mode, 20));
            EXPECT_GE(plan.signals.back().atMs, previousWake);
            previousWake = plan.signals.back().atMs;
        }
    }
}
