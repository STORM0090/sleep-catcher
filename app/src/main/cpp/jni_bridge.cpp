#include <jni.h>

#include <exception>
#include <vector>

#include "cycle_planner.h"

static void throwJava(JNIEnv* env, const char* className, const char* message) {
    jclass cls = env->FindClass(className);
    if (cls != nullptr) env->ThrowNew(cls, message);
}

// Returns the plan as one flat array: [warning, time0, kind0, time1, kind1, ...]
extern "C" JNIEXPORT jlongArray JNICALL
Java_com_yehor_sleepcatcher_core_NativePlanEngine_nativeBuildPlan(
        JNIEnv* env, jobject,
        jlong onsetMs, jlong targetMs, jint mode, jlong naturalWakeMs,
        jint cycleMinutes, jint fallAsleepMinutes, jint minCycles, jint softGapMinutes) {
    if (!sleepcore::isValidMode(mode)) {
        throwJava(env, "java/lang/IllegalArgumentException", "Unknown wake-up mode");
        return nullptr;
    }

    // A C++ exception must not reach the JVM, that would crash the app.
    try {
        sleepcore::PlanRequest req;
        req.onsetMs = onsetMs;
        req.targetMs = targetMs;
        req.mode = static_cast<sleepcore::Mode>(mode);
        req.naturalWakeMs = naturalWakeMs;
        req.tuning.cycleMinutes = cycleMinutes;
        req.tuning.fallAsleepMinutes = fallAsleepMinutes;
        req.tuning.minCycles = minCycles;
        req.tuning.softGapMinutes = softGapMinutes;

        sleepcore::Plan plan = sleepcore::buildPlan(req);

        std::vector<jlong> out;
        out.push_back(plan.naturalWakeWarning ? 1 : 0);
        for (const sleepcore::Signal& s : plan.signals) {
            out.push_back(s.atMs);
            out.push_back(static_cast<jlong>(s.kind));
        }

        jsize size = static_cast<jsize>(out.size());
        jlongArray result = env->NewLongArray(size);
        if (result == nullptr) return nullptr;  // out of memory
        env->SetLongArrayRegion(result, 0, size, out.data());
        return result;
    } catch (const std::exception& e) {
        throwJava(env, "java/lang/RuntimeException", e.what());
        return nullptr;
    }
}
