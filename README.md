# Sleep Catcher

An Android alarm that counts sleep cycles from the moment you actually fall
asleep, not from the moment you set the alarm. It notices when the screen goes
off right after TikTok, treats that as the start of the night, and puts the
alarm on a 90-minute cycle boundary. The planning logic is a small C++ core
with unit tests; the Android shell is written in Kotlin.
