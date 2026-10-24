# OnePixel

OnePixel is my personal collection of tools, launchers, and system UI replacements that aim to make Samsung's One UI look, feel, and behave as closely as possible to a Google Pixel phone.

I love Samsung's hardware and prefer Google's approach to software. OnePixel brings those preferences together, one component at a time.

> [!NOTE]
> Currently, OnePixel is targeted at OneUI 8 with Android 16 due to my own hardware limitations. Its future goal is to support OneUI 9 and whatever comes after :D

## Goals

- Recreate the Pixel experience in both appearance and interaction.
- Build focused components for individual parts of the experience, such as the notification shade or volume slider.
- Keep each component easy to develop and maintain on its own.

## Accomplishments

1. 🏠 Bringing the [Pixel Launcher](launcher/readme.md) to One UI
   - Patched system functions for One UI compatibility
   - Circular Pixel icons and folder shapes
   - App search, live web suggestions, and cross-app search
   - Rootless Shizuku+ integration for task and animation APIs

## How the ports work

Our starting point is the Pixel software running in an Android emulator. For the launcher, we **pull the original APK and framework resources from a matching emulator, apply focused compatibility patches, then install the locally signed result on a Samsung device**.

We keep the original Pixel interface and behavior where they work. OnePixel supplies Kotlin helpers for the parts that need adaptation to One UI, using public Android APIs and selected Shizuku+ operations. This is a port of an existing APK, not a source build of Google's launcher.

The repository contains our Kotlin code, patch files, resources, dependency declarations, and documentation. Original APKs, decoded Google code, generated outputs, signing keys, and device diagnostics stay outside the repository. Each developer extracts their own supported emulator input.

New implementation and development tools should use **Kotlin wherever practical**. Smali patches remain necessary to connect our code to the original APK. Small shell entry points and standard Gradle wrapper files are the exceptions.

## Status

OnePixel is in early development. The first component is an experimental [Pixel Launcher port](launcher/readme.md), tested on a Samsung SM-F936N with Android 16 / One UI 8, using Shizuku+ for selected privileged operations. Its README documents verified behavior, limitations, and the local pull → patch → apply workflow.

The launcher has a small, independent Kotlin/Gradle development setup with pinned dependencies, including Apktool. CI, release automation, and a more extensive build pipeline are deferred until later in the project.
