# Editor Scheme by Language Design

## Goal

Build an IntelliJ IDEA plugin that applies an editor color scheme to the active editor based on the current file's IntelliJ `Language` or `FileType`, with user-configurable global mappings in Settings.

## Product Scope

The plugin is a conventional IntelliJ Platform plugin built from the official `JetBrains/intellij-platform-plugin-template` and implemented in Kotlin.

The first version targets:

- IntelliJ IDEA only
- Official template defaults for IDE compatibility
- Global IDE-level settings
- Installed editor color schemes already available in IntelliJ
- Local editor-only scheme switching

The first version explicitly does not include:

- Global IDE scheme switching as a fallback
- Bundled custom schemes
- Scheme import support
- Project-level overrides
- Diagnostics UI
- Broad special-case support for editor-like surfaces that require dedicated integrations

## User Experience

The plugin adds an application-level settings page under `Tools` named `Editor Scheme by Language`.

The settings screen contains:

- A master checkbox: `Enable automatic editor scheme switching`
- A simple table of rules

Each rule maps one target to one installed editor color scheme:

- Target kinds supported:
  - `Language`
  - `FileType`
- Rule values are selected from IntelliJ-backed choosers, not entered as free text
- Scheme values are selected from installed editor color schemes, not entered as free text

The settings UI behavior is:

- One rule per unique target
- `Language` and `FileType` targets may both exist for the same effective file context
- Duplicate targets are blocked
- Invalid configuration blocks `Apply` and `OK`
- Rules referencing schemes that later disappear are shown as invalid in settings, but do not break runtime behavior

The plugin starts with an empty rule table.

## Runtime Behavior

The runtime behavior is intentionally narrow and performance-oriented.

On editor activation:

1. Detect the active editor through standard supported text-editor activation hooks.
2. Resolve the current editor's IntelliJ `Language`, if available.
3. Resolve the current editor or file's IntelliJ `FileType`.
4. Match rules using this precedence:
   - `Language`
   - `FileType`
   - current IntelliJ default editor color scheme from `Settings -> Editor -> Color Scheme`
5. Apply the resolved scheme to that editor instance only.

The plugin only updates an editor when that editor becomes active.

The plugin does not:

- Scan all open editors
- Repaint inactive editors on every focus change
- Persist per-editor transient state
- Track and restore historical editor-specific original schemes
- Introduce a plugin-managed fallback default scheme

If no rule matches, the plugin applies the currently configured IntelliJ default editor color scheme.

If the plugin is disabled, future switching stops. When disablement is applied through settings, the active editor should be returned to the current default scheme when practical.

## Matching Rules

The canonical matching model is:

- Use IntelliJ-provided canonical identifiers exactly as provided by the platform
- Store stable internal identifiers plus display names for UI rendering
- Prefer `Language` over `FileType` when both match
- Avoid custom identifier normalization

The chooser in settings should list all available `Language` and `FileType` values available in the IDE installation, not only values observed in the current session or project.

## Supported Editor Surfaces

The implementation should support any editor instance that comes through the standard supported text-editor path "for free".

It should not introduce special-case logic for editor surfaces that require dedicated integration work.

This means:

- Standard text editors are in scope
- Preview or diff contexts are only in scope if they naturally traverse the same supported text-editor integration path
- Any surface that needs custom branching or bespoke listeners is out of scope for v1

## Architecture

The plugin should be split into focused units:

### 1. Settings State Service

Responsible for persisting:

- `enabled`
- the list of rules

Each rule should contain:

- target kind (`Language` or `FileType`)
- target canonical ID
- target display name
- selected scheme reference

### 2. Settings Configurable

Responsible for:

- Rendering the application-level settings page
- Managing the master enable/disable toggle
- Rendering the simple rules table
- Handling chooser-driven row editing
- Validating duplicates and invalid rows
- Blocking `Apply` and `OK` when the configuration being edited is invalid

### 3. Rule Matcher

Responsible for:

- Accepting an editor context
- Resolving an effective scheme
- Applying precedence: `Language > FileType > current IntelliJ default scheme`
- Quietly ignoring missing scheme references at runtime and falling back to default

### 4. Runtime Listener

Responsible for:

- Listening for active editor changes
- Inspecting only the active supported editor
- Short-circuiting when the plugin is disabled
- Delegating matching and apply behavior

### 5. IntelliJ API Adapter Layer

Responsible for isolating IntelliJ-specific concerns such as:

- Extracting `Language` and `FileType`
- Reading the current default editor color scheme
- Looking up installed schemes
- Applying a scheme to a specific editor instance

This boundary keeps rule resolution and persistence easier to test without full editor integration.

## Data Flow

The end-to-end flow is:

1. IntelliJ activates an editor.
2. The runtime listener verifies that the editor is a supported text-editor context.
3. The adapter extracts the current `Language` and `FileType` metadata.
4. The matcher resolves the effective scheme from current settings.
5. The adapter applies that scheme to the active editor only.

When the settings dialog is applied:

- Updated settings become the active configuration
- If the currently active editor matches a rule, or would otherwise fall back, the plugin may update that active editor immediately
- The plugin does not rescan all open editors as part of settings apply

## Failure Handling

The implementation should fail soft at runtime:

- Missing configured scheme:
  - Ignore the broken mapping for runtime purposes
  - Fall back to the current IntelliJ default editor color scheme
- Unsupported editor surface:
  - Do nothing
- Disabled plugin:
  - Do nothing for future activations

The settings UI should surface broken scheme references so the user can repair them, but runtime should stay quiet and avoid notifications or interruptions in normal use.

## Performance Constraints

Performance is a first-order requirement.

The plugin should:

- Register lightweight listeners only
- Do no eager startup work beyond required listener setup
- Avoid global editor scans
- Avoid global IDE scheme mutation
- Avoid background indexing or polling
- Limit work to a small number of lookups on active-editor changes

The implementation should optimize for the requirement that only the actual active editor needs a different color scheme.

## Feasibility Gate

The highest-risk technical assumption is whether the IntelliJ Platform currently supports reliable local editor-only color scheme override without global side effects.

Implementation must validate this capability early using current IntelliJ APIs from the official plugin template baseline.

If true local editor-only scheme assignment cannot be implemented cleanly and reliably, the project stops and documents the platform constraint. It must not silently degrade into global IDE scheme switching.

## Testing Strategy

The first version should include:

### Unit Tests

Cover:

- `Language` rule precedence over `FileType`
- fallback to the current IntelliJ default editor color scheme
- disabled-plugin behavior
- missing-scheme handling

### Persistence Tests

Cover:

- saving and reloading the enabled flag
- saving and reloading canonical rule entries

### Narrow Integration Test

Cover:

- a supported editor activation path
- editor context extraction
- matcher invocation
- scheme application delegation

The initial test suite does not need broad UI automation for the settings screen, but settings validation logic should still be directly testable.

## Working Name

Plugin name: `Editor Scheme by Language`
