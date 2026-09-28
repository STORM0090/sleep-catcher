#include "cycle_planner.h"

namespace sleepcore {

static const int64_t kMinute = 60 * 1000;

static int64_t minutes(int count) {
    return count * kMinute;
}

// How many whole cycles fit between start and limit.
static int64_t cyclesBetween(int64_t start, int64_t limit, int64_t cycle) {
    if (limit <= start) return 0;
    return (limit - start) / cycle;
}

// Last cycle end before the target, but at least minCycles into the night.
static int64_t refreshedAlarm(int64_t sleepStart, int64_t target, int64_t cycle, int minCycles) {
    int64_t k = cyclesBetween(sleepStart, target, cycle);
    if (k < minCycles) k = minCycles;
    return sleepStart + k * cycle;
}

bool isValidMode(int mode) {
    return mode == 0 || mode == 1;
}

Tuning sanitize(Tuning t) {
    if (t.cycleMinutes < 1) t.cycleMinutes = 90;
    if (t.fallAsleepMinutes < 0) t.fallAsleepMinutes = 0;
    if (t.minCycles < 1) t.minCycles = 1;
    if (t.softGapMinutes < 0) t.softGapMinutes = 0;
    return t;
}

std::vector<int64_t> cycleBoundaries(int64_t onsetMs, const Tuning& tuning, int count) {
    Tuning t = sanitize(tuning);
    int64_t sleepStart = onsetMs + minutes(t.fallAsleepMinutes);
    int64_t cycle = minutes(t.cycleMinutes);

    std::vector<int64_t> result;
    for (int k = 1; k <= count; k++) {
        result.push_back(sleepStart + k * cycle);
    }
    return result;
}

Plan buildPlan(const PlanRequest& req) {
    Tuning t = sanitize(req.tuning);
    int64_t sleepStart = req.onsetMs + minutes(t.fallAsleepMinutes);
    int64_t cycle = minutes(t.cycleMinutes);
    Plan plan;

    if (req.mode == Mode::CatchDream && req.targetMs > req.onsetMs) {
        // Soft pulse on the last cycle end that is at least softGap before the target.
        int64_t latestSoft = req.targetMs - minutes(t.softGapMinutes);
        int64_t k = cyclesBetween(sleepStart, latestSoft, cycle);
        int64_t softAt = sleepStart + k * cycle;
        if (k >= t.minCycles && softAt < req.targetMs) {
            plan.signals.push_back({softAt, SignalKind::SoftPulse});
        }
        // Loud alarm exactly at the target, so it never wakes me later than I asked.
        plan.signals.push_back({req.targetMs, SignalKind::LoudAlarm});
    } else {
        // Refreshed mode, an unknown mode, or a target that is not after the onset.
        int64_t alarmAt = refreshedAlarm(sleepStart, req.targetMs, cycle, t.minCycles);
        plan.signals.push_back({alarmAt, SignalKind::LoudAlarm});
    }

    if (req.naturalWakeMs > 0) {
        plan.naturalWakeWarning = plan.signals.back().atMs > req.naturalWakeMs;
    }
    return plan;
}

}  // namespace sleepcore
