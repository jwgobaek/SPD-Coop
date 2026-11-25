# Android validation backlog

Common game logic, persistence and LAN protocol run under the desktop JVM and libGDX Headless. The Android build has its own package and signature checks in `scripts/verify-android-build.sh`. The following checks require an Android runtime. A standard unrooted installation is sufficient for the installation and UI checks.

| Check | Result this session | Procedure when a device becomes available |
|---|---|---|
| Install, launch and update | NOT RUN | Install the signed development APK alongside the official game. Update an existing development installation signed by the same certificate and reopen its saves. |
| Graphics, layout and input | NOT RUN | Open both seats' HUDs, stock and inventory windows. Tap, hold, drag, pinch, rotate in portrait and landscape and check cell coordinates and the safe area. |
| Screen lock, background, return, process death | NOT RUN | Interrupt each seat during a transaction. Resume, authenticate the same seat and check the original receipt and balance. |
| AndroidKeystore and app storage | NOT RUN | Reopen stored credentials after process death and device restart. Verify key loss leaves the seat locked. Interrupt generation writing and inspect HEAD/BACKUP on device. |
| WLAN, hotspot, offline LAN | NOT RUN | Discover, connect by address, transact and reconnect across two installations on Wi-Fi and on an internet-disconnected hotspot. |
| Diagnostics export | NOT RUN | Invoke the Android share target and inspect the exported text for redaction. |
| Device performance and endurance | NOT RUN | Measure frame pacing, latency, heap usage, thermal behavior and battery use over an extended session. |

The manifest declares INTERNET and ACCESS_NETWORK_STATE. AndroidLauncher supplies the lifecycle and ACTION_SEND integration. AndroidSeatKeys uses the platform AndroidKeyStore provider. Desktop screenshots, JVM loopback tests and package inspection have separate results in TEST_REPORT.md.
