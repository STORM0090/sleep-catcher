#include <jni.h>

#include <exception>
#include <vector>

#include "cycle_planner.h"

namespace {

void throwJava(JNIEnv* env, const char* className, const char* message) {
    jclass clazz = env->FindClass(className);
    if (clazz != nullptr) env->ThrowNew(clazz, message);
}

}  // namespace

// The plan comes back as one flat array, which is far less JNI code than
// building Kotlin objects field by field:
//   [0]        natural-wake warning flag (0 or 1)
//   [1 + 2*i]  time of signal i in epoch milliseconds
//   [2 + 2*i]  kind of signal i
extern "C" JNIEXPORT jlongArray JNICALL
Java_com_yehor_sleepcatcher_core_NativePlanEngine_nativeBuildPlan(
        JNIEnv* env, jobject /*thiz*/,
        jlong onsetMs, jlong targetMs, jint mode, jlong naturalWakeMs,
        jint cycleMinutes, jint fallAsleepMinutes, jint minCycles, jint softGapMinutes) {
    if (!sleepcore::isValidMode(mode)) {
        throwJava(env, "java/lang/IllegalArgumentException", "Unknown wake-up mode");
        return nullptr;
    }

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

        const sleepcore::Plan plan = sleepcore::buildPlan(req);

        std::vector<jlong> out;
        out.reserve(1 + 2 * plan.signals.size());
        out.push_back(plan.naturalWakeWarning ? 1 : 0);
        for (const sleepcore::Signal& signal : plan.signals) {
            out.push_back(signal.atMs);
            out.push_back(static_cast<jlong>(signal.kind));
        }

        jlongArray result = env->NewLongArray(static_cast<jsize>(out.size()));
        if (result == nullptr) return nullptr;   // out of memory, already pending
        env->SetLongArrayRegion(result, 0, static_cast<jsize>(out.size()), out.data());
        return result;
    } catch (const std::exception& e) {
        // A C++ exception must never unwind through JNI, so turn it into a Java one.
        throwJava(env, "java/lang/RuntimeException", e.what());
        return nullptr;
    } catch (...) {
        throwJava(env, "java/lang/RuntimeException", "Unknown failure in the planning core");
        return nullptr;
    }
}
