# Pixel Launcher

An experimental port of the Android 16 emulator's Pixel Launcher to Samsung One UI, with [Shizuku+](https://github.com/thejaustin/ShizukuPlus) handling selected privileged calls.

## Verified behavior

Tested on a Samsung SM-F936N running Android 16 / One UI 8:

- The launcher starts and can hold the default HOME role.
- The homescreen and app drawer display installed apps.
- Opening an app from the drawer works.
- Adaptive icons use a circular mask inside the launcher, including icon normalization and shadows. The launcher's built-in circle model supplies the default icon and folder geometry.
- Logs confirm that task queries, remote-animation registration, and registration of the next app-launch animation run through Shizuku+.
- Launcher search combines local app matches, live web suggestions, and search actions for Google, YouTube, Maps, Play Store, settings, and contacts when the relevant app can handle the query. Suggestion arrows use the original query builder.
- Device checks confirm query transfer from the launcher into Play Store (`goodnotes`), Samsung settings search (`WLAN`), and Samsung contacts search (`OnePixel`). A web-suggestion click opens Google search with the selected suggestion.

Animation registration succeeding does not establish that every transition matches a Pixel. Samsung's system gestures, recents screen, and taskbar integration have not been replaced. The inner folding display has not been tested.

Pixel's system search service and Pixel-specific Android System Intelligence search provider are absent on the tested Samsung. Google's OneSearch and Play providers also reject this locally signed APK with a first-party signature error. Search uses the compatibility backend described below. Prediction initialization tolerates missing permissions. The existing overlay fallback is used instead of Pixel's system TaskFragment overlay.

## Search compatibility

The port keeps the original launcher renderer, local app matching, result merging, and query-builder controls. `SearchCompat` is the static entry point used by the smali patches. It delegates web scheduling and requests to `WebSearch` and `WebSuggestions`, target construction to `SearchTargets`, and fallback actions to `LocalSearchActions` and `ContactsSearch`. `CompatibleTargetLauncher` opens compatible targets through ordinary search intents and the launcher's normal activity-launch handling instead of Google's private selection RPC. These helpers live in `src/compat/kotlin/org/onepixel/compat/`.

App-search actions pass the current query into the destination app:

| Destination | Integration |
| --- | --- |
| Google | Web-search intent handled by the Google app |
| YouTube | The installed YouTube app's search intent |
| Maps | A query encoded in a `geo:` URI |
| Play Store | A `market://search` URI restricted to apps |
| Settings | The device's exported settings search activity |
| Contacts | An exported search activity discovered in Google, Samsung, or AOSP Contacts, with a preference for the dedicated contact-search entry point |

On the tested Samsung, settings search is provided by `com.android.settings.intelligence`. Contacts uses Samsung's `ContactSearchActivity` with `CLEAR_TOP` so an existing contacts task opens the requested search. These actions open the matching app's search screen; they do not query the contacts database or settings index inside the launcher.

Web suggestions use the device's language, a 150 ms debounce, a short in-memory cache, timeouts, and generation checks to discard obsolete responses. No account cookies are sent. Typed queries are sent to `suggestqueries.google.com`; local app matching and app-search actions work without that request succeeding. The original web-search preference continues to control whether the launcher requests web suggestions.

This is a compatible implementation, not the original Pixel search backend. Account-personalized suggestions, rich answer cards, Play Store suggestions, and Pixel's system settings/shortcut corpus are not reproduced. The HTTPS suggestion endpoint is not a documented public Google API and can change; failure leaves the local results and search actions available. Language, region, account state, and installed apps can also change the results compared with an emulator.

## Local workflow

The development entry point is `./onepixel`, run from this directory. It uses this module's Gradle wrapper to compile our Kotlin tools and runtime helpers. No global Gradle, Kotlin, Python, or Apktool installation is required. This is a small local development setup; CI and release/build-pipeline automation come later.

Prerequisites:

- JDK 21 installed locally. `gradle/gradle-daemon-jvm.properties` selects Java 21 for Gradle, and the Kotlin toolchain also uses Java 21. The IDE itself can run on a newer JBR. If the IDE asks for a Gradle JVM, select the installed JDK 21 rather than its bundled JBR 25.
- Android SDK Platform 36, Build Tools 36.0.0, and Platform Tools (`adb`).
- An authorized Android 16 Pixel emulator containing the supported launcher below.
- An authorized Samsung device running Android 16. Only the SM-F936N / One UI 8 has been tested.
- Shizuku+ installed on the Samsung. The tested manager is the drop-in build `13.7.0.r2766`, package `moe.shizuku.privileged.api`.

Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` if the SDK is outside `~/Library/Android/sdk`. `ONEPIXEL_BUILD_TOOLS` can override the build-tools directory. The first command downloads the pinned Gradle/tool/library dependencies.

```sh
cd launcher
./onepixel pull <emulator-serial>
./onepixel patch
./onepixel shizuku <samsung-serial>
./onepixel apply <samsung-serial>
# After checking the launcher, make it the default home app:
./onepixel home <samsung-serial>
```

Use `adb devices -l` to find the serials. Every device operation requires an explicit serial so another connected device is not selected accidentally.

1. **Pull:** obtain the launcher APK and `framework-res.apk` from the emulator. Validate both against `source.properties` before replacing local inputs. Other emulator builds are rejected.
2. **Patch:** decode a fresh copy, apply strict class-specific smali diffs, compile/import Kotlin helpers and Android libraries, merge resources and the protected Shizuku provider, then rebuild, align, sign, and verify. A publicized copy of the emulator framework is used only by the local resource linker.
3. **Apply:** install with `adb install -r`, grant the Shizuku client permission, and check startup. Existing launcher data is retained when updating with the same signing key. Setting the default HOME role is a separate explicit command.

The tools do not uninstall either launcher, change global hidden-API policy, or replace device framework resources. An ADB-started Shizuku+ server runs as shell UID 2000 and needs restarting after a reboot. Root is not required. The launcher's optional privileged integrations tolerate a missing or unauthorized Shizuku service.

For an additional startup check or to restore the tested Samsung's original home app:

```sh
./onepixel check <samsung-serial>
./onepixel restore-home <samsung-serial>
adb -s <samsung-serial> shell input keyevent KEYCODE_HOME
```

## Local files stay outside the repository

All inputs and generated state live under `${ONEPIXEL_HOME:-~/.local/share/onepixel}/launcher/`:

```text
input/          Launcher and framework APKs pulled from the emulator
keys/           Persistent local development signing key
output/         Verified onepixel.apk
work/           Disposable decoded files and rebuild intermediates
build/          Kotlin/Gradle compilation output and extracted libraries
gradle-cache/   Project-specific Gradle state
kotlin-cache/   Kotlin compiler state
diagnostics/    Startup crash diagnostics
```

`ONEPIXEL_HOME` must point outside the repository. Gradle's shared dependency cache remains in the user's Gradle home. Android Studio may create ignored editor files when opening the project.

**Keep and back up `keys/onepixel.keystore`.** It is generated only when absent and must survive cleanup. Reusing it allows updates to the installed launcher without clearing its layout and data. The development password and alias are `android` and `onepixel`; this is a local development key, not a release signing setup.

The repository contains only source, patches, resources, configuration, documentation, licenses, and the standard Gradle wrapper. APKs, extracted Google code, keys, screenshots, and device logs are not distributed here. The original launcher is an external local input.

## Supported emulator input

`com.google.android.apps.nexuslauncher`, version name `16`, version code `907`, minimum and target API `36`. The exact launcher and framework SHA-256 values are recorded in [`source.properties`](source.properties).

Patches target that input and Apktool 3.0.3's output. To support a different emulator build, review the changed Android APIs and rebase the patches against its pristine decode, then update both input checksums. Do not bypass validation to apply these diffs to a different APK.

## Source layout

| Path | Purpose |
| --- | --- |
| `src/main/kotlin/` | Kotlin CLI for pull, patch, apply, startup checks, and Shizuku+ startup |
| `src/compat/kotlin/` | Kotlin search, icon, display, settings, color, and privileged Binder helpers injected into the launcher |
| `patches/` | Unified smali diffs against the supported original APK, selected by class descriptor |
| `resources/` | Additional localized search labels |
| `source.properties` | Supported input checksums and Android SDK versions |
| `build.gradle.kts` | Standalone Kotlin build and declared dependencies |
| `gradle/` | Standard wrapper and dependency verification metadata |
| `licenses/` | Notices and dependency license texts |

The JVM tools keep command dispatch in `LauncherCli`, environment and source validation in `LauncherEnvironment` and `SourceSpec`, and process/device access in `ProcessRunner`, `AndroidDevice`, and `AndroidTools`. `EmulatorPull`, `LauncherPatcher`, `LauncherDeviceActions`, and `ShizukuStarter` own their respective commands. APK patching delegates smali diffs, runtime import, manifest changes, framework resources, and signing to focused helpers.

The compatibility code deliberately uses Android/JDK APIs without runtime Kotlin standard-library calls. Google's APK already includes an obfuscated Kotlin runtime; injecting another copy can cause class collisions. The patch command checks the generated helper smali for Kotlin runtime calls and checks all injected classes for collisions. JVM-side development tools can use ordinary Kotlin.

Smali diffs are necessary at the proprietary APK boundary. Keep new behavior in Kotlin rather than adding full hand-written smali helpers. Patch headers identify Android class descriptors because Apktool's filename suffixes for case-colliding names can vary between decodes.

## Dependencies

Versions are declared in [`build.gradle.kts`](build.gradle.kts). Gradle verifies downloaded artifacts using the checked-in SHA-256 metadata; the wrapper distribution also has a pinned checksum.

- [Apktool 3.0.3](https://apktool.org/), Apache 2.0: decode and rebuild APK resources and smali. Its pinned smali fork comes from the upstream JitPack repository.
- [Kotlin 2.2.21](https://kotlinlang.org/), Apache 2.0: runtime helpers and JVM development tools.
- [java-diff-utils 4.15](https://github.com/java-diff-utils/java-diff-utils), Apache 2.0: apply strict unified patches.
- [Shizuku API 13.1.5](https://github.com/RikkaApps/Shizuku-API), MIT: compatible with the tested Shizuku+ drop-in server. The manager/server is installed separately.
- [HiddenApiBypass 6.2](https://github.com/LSPosed/AndroidHiddenApiBypass), Apache 2.0: process-local access to non-SDK APIs.
- [Robolectric android-all 16-robolectric-13921718](https://github.com/robolectric/robolectric), compile-only: Android 16 hidden interface definitions. It is not included in the patched APK.
- Android SDK Build Tools 36.0.0: D8, zipalign, and apksigner, installed separately through the SDK.

When changing dependencies, review upstream changes and regenerate verification metadata deliberately. Do not accept a changed checksum solely to make a build pass. See [`licenses/NOTICE.md`](licenses/NOTICE.md) for distribution boundaries.
