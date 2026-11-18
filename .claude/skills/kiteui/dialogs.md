# KiteUI Dialogs & Overlays

Full chapter: [docs/dialogs.md](../../../docs/dialogs.md).  All are `ElementContext` functions: call `context.xxx(...)`.  `context` is in scope anywhere inside an element builder, including `onClick {}` and `Action {}`.  The receiver-less `toast(...)`, `dialog {}`, `alert`, `confirmDanger` on `ElementWriter` are deprecated.

```kotlin
context.toast("Saved")                                   // 3 s, bottom center
context.toast(5.seconds) { row { icon(Icon.done, "Done"); text("Uploaded") } }

context.alert("Offline", "Check your connection.")       // title + body, OK button

context.confirmDanger("Delete project", "This can't be undone.", actionName = "Delete") {
    deleteProject()                                       // runs only if confirmed (suspend)
}
// There is NO confirm(): Boolean.  Put the follow-up in the block.

context.dialog { close ->                                 // dismissable = false to block bg/back
    col {                                                 // no `card` - DialogSemantic is applied
        h2("Rename")
        textInput { content bind name }
        row {
            expanding.button { centered.text("Cancel"); onClick { close() } }
            expanding.important.button { centered.text("Save"); onClick { save(); close() } }
        }
    }
}

context.coordinatorFrame.bottomSheet(startState = BottomSheetState.PARTIALLY_EXPANDED) { control ->
    themed(DialogSemantic).col {
        centered.coordinatorDragHandle()
        button { text("Done"); onClick { control.close() } }
    }
}
// also leftSlidingPanel / rightSlidingPanel.  openBottomSheet {} is deprecated.

menuButton {
    icon(Icon.moreVert, "More options")
    requireClick = true                                   // otherwise opens on hover (web)
    preferredDirection = PopoverPreferredDirection.belowLeft
    opensMenu {
        col { button { text("Edit"); onClick { context.currentPopover?.close(); edit() } } }
    }
}

button { onClick { openPopover(PopoverPreferredDirection.aboveCenter) { text("Hint") } } }

context.overlay(modal = false) { close -> atTopEnd.card.row { /* ... */ } }   // raw building block
```

- Prefer `context.dialog` over a hand-rolled `shownWhen` + `Signal` overlay: it handles background, back, focus trap, stacking.
- Back closes the top dialog before popping a page.
- Errors thrown in `onClick`/`Action` already show an error dialog; see error-handling.md.
