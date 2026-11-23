# KiteUI Advanced Patterns

## Keyboard shortcuts

```kotlin
col {
    // onKeyCode needs a ViewWriter receiver (a container like col/row), not the page's CanAddTheme.
    onKeyCode(keyCode { Ctrl + it.letter('s') }) { saveAction.startAction(this) }
    onKeyCode(keyCode { Ctrl + Shift + it.letter('z') }) { redo() }
}
```

- Modifiers: `Ctrl`, `Alt`, `Shift`, `Meta` (capitalized; there is no `shortcut`/platform-command modifier).  Keys: `it.letter('a')`, `it.num(1)`, `it.enter`, `it.escape`, `it.left`, … (`KeyCodes`).
- The handler is a plain `() -> Unit`, not suspend: use `launch { }` or `action.startAction(this)` for async work.  It is removed with the view.

## Drag and drop

```kotlin
// Source
card.text { content = "Drag me"; dragData = DragData("Greeting", "text/plain", "Hello there!") }

// Target
card.col {
    text("Drop here")
    dropTargetDelegate = object : DropTargetDelegate {
        override fun drop(event: DragEvent): Boolean { dropped.value += event.data.data; return true }
    }
}
```

With star imports of both `kiteui.views.*` and `kiteui.models.*`, `DropTargetDelegate` is ambiguous (a deprecated alias remains in `views`): add `import com.lightningkite.kiteui.models.DropTargetDelegate`.  `DropTargetDelegate` also has `over`, `enter`, `exit`, `end` (default `true`).  `DragData` can carry several MIME types (`typeToData`) and a `dragShadow`.

## Reorderable lists

```kotlin
renderReorderableListIn(ElementWriter::col, numbers, reorder = { move -> numbers.value = move.reorder(numbers.value) }) { n ->
    card.text { ::content { n().toString() } }
}
expanding.recyclerView {
    childrenReorderable(numbers, id = { it }, reorder = { move -> numbers.value = move.reorder(numbers.value) }) { n -> /* ... */ }
}
```

`forEachReorderable` is deprecated; use `renderReorderableListIn`.

## Permission-based UI

```kotlin
shownWhen { session().role >= UserRole.Admin }.button { text("Admin"); onClick { /* ... */ } }

link {
    ::to { if (canEdit()) ({ EditPage(item()._id) }) else null }   // Link.to is (() -> Page)?
    text { ::content { item().name } }
}
```

Hiding UI is not security; the server must enforce permissions too.

## Component design

1. Write the concrete view inline; extract a helper only when it's reused or prevents drift.
2. Helpers take few parameters and may load their own data.
3. Helper receiver: `ElementWriter.CanAddTheme` (or `ViewWriter` if callers need alignment/weight).

## Performance

- Long lists: `recyclerView` or `windowedList`, not `colOf`.
- Filter on the server, not with `.filter()` over a big client list.
- `remember` shares one calculation among readers; reuse it rather than recomputing in many bindings.
- `debounce(…milliseconds)` input before expensive work.
