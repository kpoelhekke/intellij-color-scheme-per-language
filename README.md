# Editor Scheme by Language

Editor Scheme by Language is an IntelliJ IDEA plugin that applies installed editor color schemes to active editors based on configured IntelliJ `Language` or `FileType` rules.

## Behavior

- Configure rules in `Settings | Tools | Editor Scheme by Language`.
- Rules map a `Language` or `FileType` to an installed editor color scheme.
- `Language` rules take precedence over `FileType` rules.
- Unmapped editors use the current IDE default from `Settings | Editor | Color Scheme`.
- The plugin updates only the active editor through the supported text-editor path.

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
