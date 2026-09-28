#include <gtest/gtest.h>

#include <algorithm>
#include <string>

#include "cycle_planner.h"

using namespace sleepcore;

// Instead of single nights, these tests try many bedtimes and targets
// and check what has to be true for every plan.

static int64_t minutes(int m) {
    return m * 60 * 1000LL;
}

static PlanRequest request(int onset, int target, Mode mode, int softGap) {
    PlanRequest req;
    req.onsetMs = minutes(onset);
    req.targetMs = minutes(target);
    req.mode = mode;
    req.tuning.softGapMinutes = softGap;
    return req;
}

// Minutes after some midnight: bedtime 22:00 to 04:00, target 05:00 to 12:00.
static const int kFirstOnset = 22 * 60;
static const int kLastOnset = 28 * 60;
static const int kFirstTarget = 29 * 60;
static const int kLastTarget = 36 * 60;

TEST(PlanInvariants, EveryPlanIsUsable) {
    for (int onset = kFirstOnset; onset <= kLastOnset; onset += 7) {
        for (int target = kFirstTarget; target <= kLastTarget; target += 11) {
            for (Mode mode : {Mode::Refreshed, Mode::CatchDream}) {
                for (int softGap : {0, 20, 120}) {
                    SCOPED_TRACE("onset " + std::to_string(onset) + ", target " + std::to_string(target) +
                                 ", soft gap " + std::to_string(softGap));
                    Plan plan = buildPlan(request(onset, target, mode, softGap));

                    ASSERT_FALSE(plan.signals.empty());
                    EXPECT_EQ(plan.signals.back().kind, SignalKind::LoudAlarm);
                    for (size_t i = 0; i < plan.signals.size(); i++) {
                        EXPECT_GT(plan.signals[i].atMs, minutes(onset));
                        if (i > 0) EXPECT_LT(plan.signals[i - 1].atMs, plan.signals[i].atMs);
                    }
                }
            }
        }
    }
}

TEST(PlanInvariants, RefreshedAlwaysWakesOnACycleBoundary) {
    for (int onset = kFirstOnset; onset <= kLastOnset; onset += 7) {
        for (int target = kFirstTarget; target <= kLastTarget; target += 11) {
            Plan plan = buildPlan(request(onset, target, Mode::Refreshed, 20));
            std::vector<int64_t> boundaries = cycleBoundaries(minutes(onset), Tuning{}, 20);

            ASSERT_EQ(plan.signals.size(), 1u);
            bool onBoundary = std::find(boundaries.begin(), boundaries.end(), plan.signals[0].atMs) != boundaries.end();
            EXPECT_TRUE(onBoundary) << "onset " << onset << ", target " << target;
        }
    }
}

TEST(PlanInvariants, LaterTargetNeverWakesYouEarlier) {
    for (Mode mode : {Mode::Refreshed, Mode::CatchDream}) {
        int64_t previousWake = 0;
        for (int target = kFirstTarget; target <= kLastTarget; target += 11) {
            Plan plan = buildPlan(request(23 * 60, target, mode, 20));
            EXPECT_GE(plan.signals.back().atMs, previousWake) << "target " << target;
            previousWake = plan.signals.back().atMs;
        }
    }
}
