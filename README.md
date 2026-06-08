# Editor Scheme by Language

Editor Scheme by Language is an IntelliJ IDEA plugin that applies installed editor color schemes to active editors based on configured IntelliJ `Language` or `FileType` rules.

## Behavior

- Configure rules in `Settings | Tools | Editor Scheme by Language`.
- Rules map a `Language` or `FileType` to an installed editor color scheme.
- `Language` rules take precedence over `FileType` rules.
- Unmapped editors use the current IDE default from `Settings | Editor | Color Scheme`.
- The plugin updates active editors through supported text-editor paths.
- Supported diff viewers that expose editor panes through public IntelliJ Platform APIs use the same `Language`/`FileType` scheme rules as normal editors.
- Merge conflict views and editor-backed previews are handled when IntelliJ creates them as ordinary editor instances with public file or file type metadata.
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
