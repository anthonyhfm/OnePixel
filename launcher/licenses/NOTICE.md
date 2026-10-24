# Third-party components

OnePixel's launcher module contains original Kotlin compatibility code and patches
for a separately obtained Google Pixel Launcher APK. The Google APK, its decoded
source/resources, and Android emulator framework APK are local inputs and are not
included in this repository. Google and Android component names remain the
property of their respective owners. These patches do not grant rights to
redistribute the original APK or a modified copy.

The patched APK incorporates these downloaded libraries:

- **Shizuku API 13.1.5**, RikkaApps, MIT. See [Shizuku-API-LICENSE](Shizuku-API-LICENSE).
- **HiddenApiBypass 6.2**, LSPosed, Apache License 2.0. See [HiddenApiBypass-LICENSE](HiddenApiBypass-LICENSE).

Development-only tools include Apktool, Kotlin, java-diff-utils, Gradle, their
transitive dependencies, and Robolectric's Android platform compile definitions.
They are resolved externally and are not injected into the patched APK. The
standard Gradle wrapper JAR and scripts are included under Apache License 2.0;
see [Apache-2.0.txt](Apache-2.0.txt) and the script headers.

Android SDK components and the separately installed Shizuku+ manager/server are
external prerequisites governed by their respective upstream terms. Versions,
upstream links, and integration details are listed in the [module README](../readme.md).
