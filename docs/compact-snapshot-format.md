# Compact AI Driver Snapshot Format — Implementation Summary
<!-- by Claude -->

This document describes the compact snapshot format changes implemented for the AI driver.
Intended as a handoff reference for other agents working on this codebase.

## What Changed

### Phase 1: Relative IDs + Compact Rendering

**1a. Named elements reset the path prefix** — Both snapshot walkers (`SnapshotWalkerBase.kt` for Android/iOS/JS and `SnapshotWalker.jvmSsr.kt` for JVM SSR) now check if a view has a `debugName` or `ariaDescription`. If it does, the component's `id` is just the segment name (not the full accumulated path), and children build their paths from that segment.

Before: `swapView for appNavFactory/outer nav/navigatorView/0/0/logout-btn`
After: `logout-btn`

**1b. Anchored path resolution** — `resolveAiPath()` in `AriaDescription.kt` now deep-searches for the first segment if it's a name that doesn't match a direct child, then follows remaining segments from there. This supports paths like `navigatorView/0/0/2/1` where `navigatorView` may be deeply nested.

**1c. Hierarchical `findById()`** — `UiComponent.findById()` in `UiSnapshot.kt` now supports anchored prefix matching: if the search ID starts with a component's own ID + "/", it strips the prefix and recurses into children.

**1d. Compact text format** — `UiComponent.render()` now shows just the segment (last part of the id), uses `= "value"` syntax, and shows `[actions]` inline. The CLI server's `formatSnapshotText()` delegates to `UiComponent.render()` for a unified format.

Format: `segment: Type = "value" [actions] (flags)`

**1e. Value field cleanup** — Snapshot walkers no longer fall back to `ariaDescription` or `debugName` for the `value` field. Value is now exclusively from `accessibilityValue`. The name is already captured in the component ID.

### Phase 2: Filter Hidden Elements

Both snapshot walkers now skip elements where `visible == false` when `includeLessVisible == false` (the default). Uses `mapIndexedNotNull` but preserves the original `mapIndexed` index so sibling indices stay stable.

The `buildSnapshot()` expect/actual signature gained a `settings: UiSnapshotSettings` parameter (with default value).

### Phase 3: `interactiveOnly` Mode

`UiSnapshotSettings` has a new `interactiveOnly: Boolean = false` field. `UiSnapshot.compact()` and `UiComponent.compact()` implement a post-processing pass that:
- Discards leaf nodes with no name/value/actions
- Collapses single-child structural containers (merging indices into the child's path)
- Keeps multi-child structural containers as grouping nodes

The CLI `snapshot` command has a new `--interactiveOnly` flag that applies `compact()` server-side.

### Phase 4: Drag/Drop Action

`UiAction.DragAndDrop(targetId, toTargetId)` drags an element and drops it onto another. The common dispatcher gets `dragData` from the source view and invokes the destination's `dropTargetDelegate.drop()` directly — no platform-specific event synthesis needed. Recording export generates proper `UiAction.DragAndDrop(...)` code.

## Files Modified

| File | Changes |
|------|---------|
| `library/src/commonMain/.../aidriver/UiSnapshot.kt` | `interactiveOnly` setting, `compact()` methods, hierarchical `findById()`, compact `render()` |
| `library/src/commonMain/.../aidriver/UiAction.kt` | Added `DragAndDrop` variant |
| `library/src/commonMain/.../aidriver/ActionDispatcherCommon.kt` | Drag dispatch logic |
| `library/src/commonMain/.../aidriver/CliCommand.kt` | `interactiveOnly` flag on Snapshot |
| `library/src/commonMain/.../aidriver/PlatformExpects.kt` | `settings` parameter on `buildSnapshot` |
| `library/src/commonMain/.../views/AriaDescription.kt` | Anchored path resolution |
| `library/src/commonInteractiveMain/.../aidriver/SnapshotWalkerBase.kt` | Relative IDs, hidden filtering, settings param |
| `library/src/jvmSsrMain/.../aidriver/SnapshotWalker.jvmSsr.kt` | Same as above |
| `library-swing/src/jvmMain/.../aidriver/PlatformActuals.kt` | Updated signature |
| `ai-driver-server/.../server/CliServer.kt` | `--interactiveOnly` handling, compact format |
| `ai-driver-server/.../server/AppSession.kt` | `findComponent` delegates to `findById` |
| `ai-driver-server/.../server/Recording.kt` | Drag export |
| `library/src/androidMain/.../aidriver/ActionDispatcher.android.kt` | Screenshot support |
| `library/src/iosMain/.../aidriver/ActionDispatcher.ios.kt` | Screenshot support |
| `library/src/jsMain/.../aidriver/ActionDispatcher.js.kt` | Screenshot support |
| `library/src/jvmSsrTest/.../aidriver/CompactSnapshotTest.kt` | **NEW** — 20 tests for compact format |
| `.claude/skills/ai-driver.md` | Updated snapshot format docs, drag command |

## Completed Follow-up Items

1. **DragAndDrop implementation** — Handled entirely in common code (`ActionDispatcherCommon.kt`). Gets `dragData` from source view, finds `dropTargetDelegate` on destination view (walking up ancestors), and invokes `enter()` → `drop()` → `end()` directly. No platform-specific event synthesis needed.

2. **Bug fix** — Fixed `ActionDispatcherCommon.kt` drag dispatch: removed erroneous `?: "Drag not yet supported..."` fallback that would set an error message on successful results.

3. **Bug fix** — Fixed `UiComponent.isInteresting` to check only the last segment of the ID (`id.substringAfterLast("/")`), not the full path. IDs like `"0/0"` contain `/` which is not a digit, making them falsely "interesting".

4. **New tests** — `CompactSnapshotTest.kt` (20 tests) covering:
   - Relative IDs: named elements get short IDs, unnamed children build from ancestor
   - Nested named elements each reset independently
   - Anchored path resolution: deep search for named first segment, then follow remaining
   - Hierarchical `findById`: exact match, suffix match, anchored prefix match
   - Compact text format: segment-only display, inline actions, no name-as-value
   - Value field cleanup: debugName not echoed as value, accessibilityValue is source of truth
   - Hidden element filtering with settings
   - `compact()` post-processing: leaf pruning, single-child collapse, multi-child preservation, named element preservation, value preservation
   - Drag action dispatch: returns not-supported on SSR, reports view-not-found
   - Visual verification: renders a realistic UI tree and asserts compact format properties

5. **Visual verification** — The `compactFormatVisualVerification` test renders a realistic app structure and verifies the output format. Confirmed output:
   ```
   appContainer: Column
     toolbar: Row
       0: Text = "My App"
       settings: Button [click,longClick]
         0: Text = "Settings"
     content: Column
       0: Text = "Welcome!"
       1: Row
         decrement: Button [click,longClick]
           0: Text = "-"
         counter: TextInput = "0" [click,setValue]
         increment: Button [click,longClick]
           0: Text = "+"
       submit: Button [click,longClick]
         0: Text = "Submit"
   ```

### Phase 5: `find` Subcommand

<!-- by Claude -->
`CliCommand.Find` provides structured component search so scripts don't need to grep/sed snapshot text.

CLI: `kiteui-drive find <appId> [--value text] [--type Type] [--action click] [--id partial] [--limit 10] [--format Text|Json]`

- Filters are combined with AND, all case-insensitive
- Text output: `id Type = "value" [actions] (disabled)` — one per line
- JSON output: array of `FindResult` objects with `id`, `type`, `value`, `actions`, `enabled`
- On no match: returns exit code 1 and `NOT_FOUND:` + full snapshot for debugging

**Files:**
| File | Changes |
|------|---------|
| `library/src/commonMain/.../aidriver/CliCommand.kt` | `Find` command + `FindResult` data class |
| `ai-driver-server/.../server/CliServer.kt` | Find handler: tree search with filter matching |
| `ai-driver-server/.../server/Main.kt` | Exit code 1 on NOT_FOUND, usage text |
| `library/src/jvmSsrTest/.../aidriver/CompactSnapshotTest.kt` | 7 find tests |

## What Still Needs Doing

1. **Real platform drag implementations** — Android needs `MotionEvent` synthesis, iOS needs programmatic gesture synthesis, JS needs `PointerEvent` dispatch. Currently all return "not yet implemented" stubs.

## Backwards Compatibility

- All existing tests pass (`./gradlew :library:jvmSsrTest :example-app:jvmSsrTest`)
- `findById()` still supports full paths and suffix matching, so old-style absolute IDs work
- `resolveAiPath()` still resolves direct paths before falling back to anchored search
- Default `UiSnapshotSettings()` preserves previous behavior (no hidden filtering, no interactiveOnly)
- The `buildSnapshot()` `settings` parameter has a default value so existing callers don't break
