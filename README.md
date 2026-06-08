# Color Scheme per Language

Color Scheme per Language is an IntelliJ IDEA plugin that applies installed editor color schemes to active editors based on configured IntelliJ `Language` rules.

## Behavior

- Configure rules in `Settings | Tools | Color Scheme per Language`.
- Rules map a `Language` to an installed editor color scheme.
- Unmapped editors use the current IDE default from `Settings | Editor | Color Scheme`.
- The plugin updates active editors through supported text-editor paths.
- Supported diff viewers that expose editor panes through public IntelliJ Platform APIs use the same `Language` scheme rules as normal editors.
- Merge conflict views and editor-backed previews are handled when IntelliJ creates them as ordinary editor instances with public language metadata.
- Existing diff, merge, and preview editors are not rescanned when settings change; reopen or recreate the view to apply updated mappings.

## Development

Run tests:

```bash
./gradlew test
```

Build plugin:

```bash
./gradlew buildPlugin
```

Run development IDE:

```bash
./gradlew runIde
```
