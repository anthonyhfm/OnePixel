# Agent instructions

## Project context

Read [readme.md](readme.md) before making changes. It defines OnePixel's purpose, goals, and current status.

OnePixel aims to bring the look, feel, and behavior of a Google Pixel phone to Samsung hardware running One UI through focused tools, launchers, and system UI replacements.

## Project organization

- Give every subproject its own top-level directory. Use descriptive, lowercase, hyphen-separated names, such as `notification-shade` or `volume-slider`.
- Keep each subproject's source code, resources, build configuration, and documentation inside its directory.
- Keep the repository root focused on project-wide documentation and configuration.
- Preserve clear boundaries between components. Avoid unnecessary dependencies and a single, cluttered Gradle project containing all features.
- Introduce shared code only when multiple components actually need it, and give it a clearly defined home.

## Implementation guidance

- Use the Pixel experience as the reference for both visuals and interaction behavior.
- Write new runtime helpers and development tools in Kotlin where practical. Keep smali limited to patches that connect to or adapt the original APK; use shell only for thin entry points.
- Preserve the emulator pull → patch → apply workflow. Validate supported APK/framework checksums and fail clearly for other versions instead of applying patches optimistically.
- Keep proprietary emulator inputs, decoded APKs, generated binaries, signing keys, and diagnostics outside the repository. `ONEPIXEL_HOME` selects local state; never delete or regenerate an existing signing key during cleanup.
- Declare tool and runtime dependencies in the component's build configuration. Keep versions pinned and update dependency checksums when changing artifacts.
- Keep the current development setup small. Do not add CI or release/build-pipeline infrastructure unless requested.
- Keep changes focused on the component being worked on.
- Document component-specific setup, build steps, required permissions, and platform limitations in that component's README.
- Distinguish planned features from implemented behavior. Do not describe a component as working until it exists and has been verified.
- Run the checks relevant to the affected component when available, and report any verification limitations.

## Code quality conventions

- Use braces for every `if`, `else`, loop, and exception handler. Never put a control-flow body on the same line as its condition or catch declaration.
- Use named arguments when calling Kotlin functions and constructors with named parameters. Java APIs do not support named arguments; keep those positional. Keep vararg collections and Gradle DSL calls idiomatic.
- Put each parameter of a multiline declaration or call on its own line. Expand long argument lists instead of compressing several unrelated values onto one line.
- Break long fluent call chains across lines, with one chained call per line. Apply the same layout to Compose modifier chains in future UI modules.
- Separate logical steps and declarations with blank lines. Use descriptive names and one statement per line; do not join statements with semicolons.
- Keep entry points small and split substantial files by responsibility. Avoid introducing abstractions that only add indirection.
- Avoid comments and KDoc unless requested or essential to explain a non-obvious constraint. Keep essential comments short; explain architecture and workflows in the module README.
- Preserve the static JVM signatures used by smali patches when refactoring runtime helpers.
- Injected APK helpers must continue to avoid Kotlin standard-library runtime calls. Use Android/JDK APIs there and retain the bytecode check; ordinary Kotlin is appropriate for JVM development tools.

