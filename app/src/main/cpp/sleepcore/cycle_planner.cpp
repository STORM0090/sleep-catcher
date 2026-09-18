#include "cycle_planner.h"

namespace sleepcore {
namespace {

constexpr int64_t kMsPerMinute = 60 * 1000;

int64_t minutesToMs(int minutes) {
    return static_cast<int64_t>(minutes) * kMsPerMinute;
}

int64_t sleepStart(int64_t onsetMs, const Tuning& tuning) {
    return onsetMs + minutesToMs(tuning.fallAsleepMinutes);
}

int64_t boundaryAt(int64_t sleepStartMs, int64_t cycleMs, int64_t k) {
    return sleepStartMs + k * cycleMs;
}

// Number of whole cycles that fit between sleepStart and limit (0 if limit is earlier).
int64_t fullCyclesBefore(int64_t sleepStartMs, int64_t limitMs, int64_t cycleMs) {
    if (limitMs <= sleepStartMs) return 0;
    return (limitMs - sleepStartMs) / cycleMs;
}

}  // namespace

std::vector<int64_t> cycleBoundaries(int64_t onsetMs, const Tuning& tuning, int count) {
    std::vector<int64_t> out;
    if (count <= 0) return out;

    const int64_t start = sleepStart(onsetMs, tuning);
    const int64_t cycle = minutesToMs(tuning.cycleMinutes);
    out.reserve(static_cast<size_t>(count));
    for (int64_t k = 1; k <= count; ++k) {
        out.push_back(boundaryAt(start, cycle, k));
    }
    return out;
}

Plan buildPlan(const PlanRequest& req) {
    Plan plan;
    const int64_t start = sleepStart(req.onsetMs, req.tuning);
    const int64_t cycle = minutesToMs(req.tuning.cycleMinutes);

    switch (req.mode) {
        case Mode::Refreshed:
        default: {
            // Latest boundary that is not after the target. If that would give a
            // very short night, prefer the minimum number of cycles even if it
            // means waking a bit later than asked.
            int64_t k = fullCyclesBefore(start, req.targetMs, cycle);
            if (k < req.tuning.minCycles) k = req.tuning.minCycles;
            plan.signals.push_back({boundaryAt(start, cycle, k), SignalKind::LoudAlarm});
            break;
        }
    }
    return plan;
}

}  // namespace sleepcore
