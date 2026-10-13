# Sleep Catcher

An Android alarm that counts sleep cycles from the moment you actually fall
asleep, not from the moment you set the alarm. It notices when the screen goes
off right after TikTok, treats that as the start of the night, and puts the
alarm on a 90-minute cycle boundary. The planning logic is a small C++ core
with unit tests; the Android shell is written in Kotlin.

This is a personal project and an early version: the screen is a plain
settings page, not a finished app. There is no screenshot for that reason.

## Why

I logged twelve nights with a Galaxy Watch 4 and noted whether I remembered a
dream. Sleep score and REM share did not predict it. How the night ended did:
a gradual last wake-up came with a dream 5 of 5 times, one clean wake-up came
with nothing 2 of 2 times. Small sample, details in
[docs/sleep-observations.md](docs/sleep-observations.md).

So the app has two modes:

- **Refreshed**: one loud alarm on the last cycle boundary before your target
  time. If that would leave fewer than three cycles, it takes the third
  boundary even if that is a bit later than the target.
- **Catch a dream**: a soft vibration on the last cycle boundary that is at
  least 20 minutes before the target, then a loud alarm exactly at the target
  as a safety net. On a short night the soft vibration is skipped.

Cycles start 15 minutes after the screen goes off. If you lock the phone again
later, the plan moves with you, unless less than one cycle is left.

How it all fits together: [docs/how-it-was-built.md](docs/how-it-was-built.md).

## What you need

- An Android phone with Android 10 or newer. Built and used on a Galaxy S24
  Ultra with Android 16.
- Android Studio. Gradle downloads the NDK (27.0.12077973) and CMake (3.22.1)
  by itself the first time, if the SDK licenses are accepted.
- JDK 17 for Gradle. Newer JDKs do not work with this Gradle version. In
  Android Studio: Settings > Build, Execution, Deployment > Build Tools >
  Gradle > Gradle JDK.
- For the C++ tests only: CMake and a C++17 compiler (g++, clang or MinGW).

## Build and install

With Android Studio: open the folder, wait for the Gradle sync, connect the
phone with USB debugging on, press Run.

From a terminal:

```
./gradlew assembleDebug      # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # installs it on the connected phone
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## First start on the phone

The app needs a few things that only you can switch on. The status block at
the top of the screen shows which ones are still missing.

1. **Usage access**: "Open usage access settings", find Sleep Catcher, turn
   it on. Without it the app cannot see that TikTok was open.
2. **Notifications**: asked the first time you press Arm.
3. **Exact alarms**: granted automatically on Android 13 and newer. On
   Android 12 a button appears if it is missing.
4. **Full-screen alarm**: on Android 14 and newer a button appears if the
   alarm is not allowed to cover the lock screen.
5. **Battery**: optional, but Samsung stops background apps. On One UI add
   the app under Settings > Battery > Background usage limits > Never
   sleeping apps (the exact name differs a little between One UI versions).

Then pick a mode and a time and press **Arm**. A quiet notification says the
app is waiting. Scroll TikTok, lock the phone, and the alarms are set.

To try it without waiting for a night: set the time a few minutes ahead,
choose "Catch a dream" and press "Pretend I just fell asleep". "Preview a
night starting now" only shows the plan and sets nothing.

Everything the app decides goes to the log, for example
`Fell asleep at Thu 01:03: SCHEDULED` or `Screen off after com.whatsapp, not TikTok`:

```
adb logcat -s SleepCatcher
```

## Tests

C++ core, no Android SDK needed:

```
cmake -S app/src/main/cpp -B build/sleepcore
cmake --build build/sleepcore
ctest --test-dir build/sleepcore --output-on-failure
```

On Windows with MinGW add `-G "MinGW Makefiles"` to the first command and
make sure the MinGW `bin` folder is on `PATH`.

Kotlin logic (runs on the JVM, no phone or emulator):

```
./gradlew testDebugUnitTest
```

Both run on every push through GitHub Actions
(`.github/workflows/core-tests.yml`). The C++ job must pass; the Kotlin job is
allowed to fail, because the Android tooling on the runner changes on its own.

## Layout

```
app/src/main/cpp/sleepcore/        the planner: cycle boundaries and the two modes
app/src/main/cpp/tests/            GoogleTest tests for the planner
app/src/main/cpp/jni_bridge.cpp    the only place where C++ and Kotlin meet
app/src/main/java/.../core/        plan types and the class that calls the C++ code
app/src/main/java/.../time/        "next 09:00" in the phone's time zone
app/src/main/java/.../data/        settings stored in SharedPreferences
app/src/main/java/.../scheduling/  turns a plan into AlarmManager alarms
app/src/main/java/.../detection/   screen-off, which app was open, reboot
app/src/main/java/.../alarm/       vibration, ringing service, lock screen alarm
app/src/main/java/.../ui/          the main screen
app/src/test/                      JUnit tests, with a fake planner, settings and alarm manager
```

## Known limitations

- The trigger app is hard-coded: TikTok (both of its package names).
- The hour after which I wake up by myself anyway is fixed at 11:00. The
  warning about it only appears in the preview and when pretending to fall
  asleep, not at night.
- After a reboot the alarms come back only once the phone has been unlocked.
  On the S24 it then took about a minute and a half until Android told the
  app that the phone had started.
- If Samsung stops the background service and you do not open the app, a
  screen-off can be missed. The loud alarm itself is not affected.
- Android records which app is open with a small delay, so locking the phone
  within a second of opening TikTok may not count.

## Next

- A dream journal screen that opens right at the wake-up.
- Health Connect: read the night from the watch and store it next to the
  journal entry.
- A real UI.

## License

Yehor Vorobiov
