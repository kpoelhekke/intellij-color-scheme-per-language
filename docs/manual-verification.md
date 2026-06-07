# Manual Verification

## Automated Verification

- `./gradlew test buildPlugin`: pass
- `./gradlew test buildPlugin verifyPlugin`: pass
- Plugin ZIP produced: `build/distributions/editor-scheme-by-language-0.1.0.zip`
- Plugin Verifier compatibility:
  - IU-252.28539.54: compatible
  - IU-253.33813.25: compatible
  - IU-261.25134.95: compatible
  - IU-262.7132.23: compatible

## Settings

- Settings page appears under Tools: not manually verified in GUI
- Enable checkbox appears: covered by implementation and tests, not manually verified in GUI
- Language target can be selected: covered by implementation, not manually verified in GUI
- FileType target can be selected: covered by implementation, not manually verified in GUI
- Duplicate targets are rejected: covered by `EditorSchemeSettingsPanelValidationTest`
- Scheme selection uses installed schemes: covered by implementation, not manually verified in GUI

## Runtime

- Kotlin editor receives configured scheme when activated: not manually verified in GUI
- TypeScript/TSX editor receives configured scheme when activated: not manually verified in GUI
- Unmapped editor receives current IntelliJ default scheme: covered by `EditorSchemeMatcherTest`
- Inactive editors are not proactively scanned or updated: covered by implementation shape and runtime tests

## Notes

GUI verification was not completed in this non-interactive run. The plugin builds successfully, automated tests pass, and Plugin Verifier reports compatibility for all scheduled IntelliJ IDEA builds.
