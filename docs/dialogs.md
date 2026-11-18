# Dialogs

Toasts, dialogs, bottom sheets, popovers and menus all open in the app's overlay, which `appBase` sets up.  Each is a function on `ElementContext`, so call it as `context.xxx(...)` from any element, usually inside `onClick`.

## Minimal example

```kotlin
button {
    text("Rename")
    onClick {
        context.dialog { close ->
            col {
                val name = Signal("")
                h2("Rename")
                textInput { hint = "Name"; keyboardHints = KeyboardHints.title; content bind name }
                row {
                    expanding.button {
                        centered.text("Cancel")
                        onClick { close() }
                    }
                    expanding.important.button {
                        centered.text("Save")
                        onClick {
                            saveName(name.value)
                            close()
                        }
                    }
                }
            }
        }
    }
}
```

The dialog is centered over a dimmed background and themed with `DialogSemantic`.  Tapping the background or pressing back closes it; pass `dismissable = false` to prevent both.

## Common tasks

### Toast

```kotlin
context.toast("Saved")

context.toast(5.seconds) {
    row {
        icon(Icon.done, "Done")
        text("Uploaded 3 files")
    }
}
```

Shown at the bottom center for 3 seconds by default.  It doesn't block interaction.

### Confirm a destructive action

```kotlin
danger.button {
    text("Delete project")
    onClick {
        context.confirmDanger("Delete project", "This can't be undone.", actionName = "Delete") {
            deleteProject()
        }
    }
}
```

The block runs only if the user confirms; the dialog stays open, showing progress, until it finishes.  For a plain message with an OK button, use `context.alert(title, body)`.  There is no `confirm()` that returns a `Boolean`: put the follow-up in the block.

### Bottom sheet

```kotlin
onClick {
    context.coordinatorFrame.bottomSheet(startState = BottomSheetState.PARTIALLY_EXPANDED) { control ->
        themed(DialogSemantic).col {
            centered.coordinatorDragHandle()
            h2("Filters")
            button {
                text("Done")
                onClick { control.close() }
            }
        }
    }
}
```

`control.state` is a `MutableReactive<BottomSheetState>` for expanding or collapsing it from code.  `leftSlidingPanel` and `rightSlidingPanel` work the same way.  `openBottomSheet` is deprecated in favor of this.

### Menu

```kotlin
menuButton {
    icon(Icon.moreVert, "More options")
    requireClick = true
    preferredDirection = PopoverPreferredDirection.belowLeft
    opensMenu {
        col {
            button {
                text("Duplicate")
                onClick { context.currentPopover?.close() }
            }
            danger.button {
                text("Delete")
                onClick {
                    context.currentPopover?.close()
                    deleteProject()
                }
            }
        }
    }
}
```

Without `requireClick = true`, the menu also opens on hover on the web.  A `menuButton` inside a menu opens a submenu.  `context.currentPopover?.close()` closes the whole menu; `context.enclosingPopover` is the one an element sits in.

### Popover from any element

```kotlin
button {
    text("What's this?")
    onClick {
        openPopover(PopoverPreferredDirection.aboveCenter) {
            text("Projects group related tasks.")
        }
    }
}
```

Only one popover is open at a time, plus any opened from inside it.

### Raw overlay

```kotlin
context.overlay(modal = false) { close ->
    atTopEnd.card.row {
        text("Update available")
        button {
            icon(Icon.close, "Dismiss")
            onClick { close() }
        }
    }
}
```

The building block the others use.  `modal = false` leaves the UI underneath interactive; `navClosable` makes back close it.

## Gotchas

- **Call these on `context`.**  The `ElementWriter.toast`, `ElementWriter.dialog`, `alert` and `confirmDanger` receivers without `context.` are deprecated.  `context` is in scope anywhere inside an element's builder, including `onClick {}` and `Action {}` lambdas.
- **Don't wrap dialog content in `card`.**  The dialog already applies `DialogSemantic`; `card` adds a second surface inside it.
- **Prefer `context.dialog` to a `shownWhen` overlay you manage with a `Signal`.**  `dialog` handles the background, back button, focus and stacking.
- **Back closes the top dialog before it pops a page**, on Android and in the browser.
- On iOS, modal overlays open in a new view controller, so a non-modal overlay appears beneath an open bottom sheet.

## See also

- [Error handling](error-handling.md): errors thrown in `onClick` or an `Action` are shown in a dialog automatically
- [Forms](forms.md): fields inside dialogs
