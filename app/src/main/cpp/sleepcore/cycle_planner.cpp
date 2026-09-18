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

}  // namespace sleepcore
