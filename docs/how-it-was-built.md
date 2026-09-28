# How it was built

A short write-up of how this app came together, in the order things
happened.

## 1. The question

I wear a Galaxy Watch 4 Classic to bed and wanted to know why I remember my
dreams on some mornings and not on others. My first guess was sleep quality:
more REM, better score, more dreams. So I kept notes for twelve nights next
to the Samsung Health data.

The guess was wrong. The notes are in [sleep-observations.md](sleep-observations.md).
What did line up was the end of the night: when I woke up in several steps
and finally got up on my own, I remembered a dream every time. When one clean
alarm or one clean wake-up ended the night, I remembered nothing.

## 2. The rule

That turned into two alarm modes:

- **Refreshed**: one loud alarm on the last 90-minute cycle boundary before
  the target time. If that leaves fewer than three cycles, take the third
  boundary even if it is a bit late.
- **Catch a dream**: a soft vibration on the last boundary that is at least
  20 minutes before the target, then a loud alarm exactly at the target so
  I am never late.

Cycles are counted from when I actually fall asleep, not from when I set the
alarm. The last thing I do before sleep is scroll TikTok and lock the phone,
so "screen off while TikTok was open" is used as the start of the night, plus
15 minutes to actually fall asleep.

## 3. Why the rule is in C++

The rule is only arithmetic on timestamps. I know C++ better than Kotlin, and
a small C++ library with no Android in it can be tested on a laptop in under
a second. So the planner lives in `app/src/main/cpp/sleepcore` and is tested
with GoogleTest.

There are two kinds of tests there. The normal ones check one night each
("asleep at 1:00, target 9:00, expect 8:45"). The grid tests walk over
thousands of combinations of bedtime and target time and check what must
always hold: the plan is never empty, times go up, the last signal is the
loud one, nothing rings before I fall asleep.

Going through the edge cases for those tests turned up real problems: a
cycle length of zero divided by zero, and "Catch a dream" with a target that
had already passed put the alarm in the past.

## 4. How Kotlin talks to it

Android APIs are Java and Kotlin only, so the app around the core is Kotlin.
The two meet through JNI in `jni_bridge.cpp`. The plan comes back as one flat
array of numbers (`[warning, time, kind, time, kind, ...]`) because building
Kotlin objects from C++ takes a lot more code. The bridge checks its input
and turns C++ exceptions into Java ones, since an exception that crosses JNI
kills the whole app.

On the Kotlin side three things sit behind a small interface: the planner,
the settings and AlarmManager. The current time is simply passed in as a
parameter. That way the scheduling logic runs in plain JUnit tests with
fakes, without a phone.

## 5. What happens at night

1. Arming starts a foreground service. Android only delivers the screen-off
   event to receivers registered by running code, so something has to stay
   alive.
2. When the screen goes off, the receiver asks UsageStatsManager which app
   was in front. If it was TikTok, that moment is the start of the night.
3. The native core builds a plan and the scheduler hands it to AlarmManager.
   The loud alarm uses `setAlarmClock`, which is the one kind of alarm that
   still fires on time in deep sleep mode.
4. A soft pulse is a short, weak vibration. The loud alarm is a service that
   plays the alarm sound and shows a full-screen stop button over the lock
   screen.

Locking the phone again later moves the plan, unless less than one cycle is
left before the target. After a reboot the plan is rebuilt from the saved
start time, skipping anything that should already have rung.

## 6. Things that went wrong on the way

- Usage stats only record when an app comes to the front, not while you use
  it. A long evening in one feed has no new events, so the lookback window
  had to be much longer than I first thought.
- Lint caught that I had forgotten to declare the exact alarm permissions.
  The unit tests could not have found that: the fake alarm manager has no
  idea what a permission is.
- After a reboot at 8:00 the restored plan still contained the 7:15 nudge,
  and Android fires alarms set in the past immediately.
- Android 15 draws apps behind the status bar by default, which would have
  pushed the first line of text under the clock.
- The first working version had too many tiny classes: a separate class for
  every rule, a clock interface, formatter objects for the status text. I
  went back and folded most of them into the places that use them. The code
  got shorter and easier to follow, and the tests that mattered stayed.

The first run on the phone found two more things the tests on the PC could
not:

- Android 16 warned that the native library was not aligned for 16 KB
  memory pages. NDK 27 only does that with one extra linker flag.
- Trying "Catch a dream" with a target a few minutes ahead showed that the
  alarm moved hours later when the target was within the first 15 minutes.
  That broke the one promise of the mode: never ring later than asked.

After that the whole chain worked on the phone: TikTok, screen off, alarm
set, alarm over the lock screen, Stop, Disarm, and a reboot in the middle of
a "night" that brought the soft pulse back on time.

## 7. What is still open

- The hour after which I wake up by myself anyway is fixed at 11:00 in the
  code, and the warning about it only shows in the preview.
- After a reboot the alarms come back only once the phone is unlocked for
  the first time.
- Samsung may stop the background service. Opening the app starts it again,
  but the reliable fix is putting the app on the "never sleeping" list.
- No dream journal yet, and the watch data still comes from screenshots.
  The plan is a journal screen that opens at the wake-up and Health Connect
  to pull the night from the watch next to the journal entry.
