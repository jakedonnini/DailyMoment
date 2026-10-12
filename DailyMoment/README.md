# Daily Moment

BeReal-style random daily photo + a short journal ("What happened?" / "What did you learn?").

## Run it
1. Android Studio -> Open -> select this folder (let it sync; it will offer to create the Gradle wrapper).
2. On your Pixel: enable Developer options + USB debugging, plug in, press Run.
3. Allow notifications + camera when asked.
4. To test the notification without waiting: in `DailyScheduler.computeNextTrigger`,
   temporarily return `System.currentTimeMillis() + 15_000`.

## Where things live
- notify/DailyScheduler.kt  - picks a random time in your window (default 9am-9pm) and sets the alarm
- notify/AlarmReceiver.kt   - fires the notification, schedules the next day
- notify/BootReceiver.kt    - re-arms the alarm after a reboot
- ui/CaptureScreen.kt       - CameraX capture
- ui/JournalScreen.kt       - photo + two prompts
- ui/TimelineScreen.kt      - past entries + streak
- data/                     - Room database (entries) and prefs

## Next steps
Settings screen for the time window, rotating prompts, late-post handling, export/backup.
