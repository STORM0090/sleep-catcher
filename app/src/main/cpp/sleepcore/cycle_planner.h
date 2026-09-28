#pragma once

#include <cstdint>
#include <vector>

namespace sleepcore {

enum class Mode {
    Refreshed = 0,   // one alarm on a cycle boundary
    CatchDream = 1   // soft nudge first, loud alarm at the target
};

enum class SignalKind {
    SoftPulse = 0,   // short weak vibration
    LoudAlarm = 1    // sound and strong vibration
};

struct Signal {
    int64_t atMs;
    SignalKind kind;
};

struct Tuning {
    int cycleMinutes = 90;
    int fallAsleepMinutes = 15;   // time between screen off and real sleep
    int minCycles = 3;            // shorter nights count as too short
    int softGapMinutes = 20;      // minimum time between the soft pulse and the loud alarm
};

struct PlanRequest {
    int64_t onsetMs = 0;          // when the screen went off
    int64_t targetMs = 0;         // latest time to wake up
    Mode mode = Mode::Refreshed;
    int64_t naturalWakeMs = 0;    // I usually wake up by myself after this (0 = unknown)
    Tuning tuning;
};

struct Plan {
    std::vector<Signal> signals;  // sorted by time, the last one is the loud alarm
    bool naturalWakeWarning = false;
};

// The mode comes from Kotlin as a plain int.
bool isValidMode(int mode);

// Replaces zero or negative values with safe ones.
Tuning sanitize(Tuning tuning);

// End of each sleep cycle, counted from onset + fall asleep time.
std::vector<int64_t> cycleBoundaries(int64_t onsetMs, const Tuning& tuning, int count);

Plan buildPlan(const PlanRequest& req);

}  // namespace sleepcore
