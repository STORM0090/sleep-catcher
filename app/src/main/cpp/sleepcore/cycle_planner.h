#pragma once

#include <cstdint>
#include <vector>

namespace sleepcore {

enum class Mode {
    Refreshed = 0,   // one clear alarm on a cycle boundary
    CatchDream = 1   // gentle nudge first, loud safety alarm at the target
};

enum class SignalKind {
    SoftPulse = 0,   // short vibration only, meant not to fully wake the sleeper
    LoudAlarm = 1    // sound + strong vibration
};

struct Signal {
    int64_t atMs;
    SignalKind kind;
};

struct Tuning {
    int cycleMinutes = 90;
    int fallAsleepMinutes = 15;   // assumed latency between screen-off and actual sleep
    int minCycles = 3;            // fewer cycles than this counts as "too short"
    int softGapMinutes = 20;      // a soft pulse closer than this to the loud alarm is pointless
};

struct PlanRequest {
    int64_t onsetMs = 0;          // screen went off after the trigger app
    int64_t targetMs = 0;         // latest acceptable wake-up time
    Mode mode = Mode::Refreshed;
    int64_t naturalWakeMs = 0;    // after this moment the sleeper tends to wake anyway (0 = unknown)
    Tuning tuning;
};

struct Plan {
    std::vector<Signal> signals;   // ascending by time, last one is the final wake-up
    bool naturalWakeWarning = false;
};

// Ends of sleep cycles 1..count, measured from onset + fall-asleep latency.
std::vector<int64_t> cycleBoundaries(int64_t onsetMs, const Tuning& tuning, int count);

Plan buildPlan(const PlanRequest& req);

}  // namespace sleepcore
