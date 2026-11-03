# KiteUI Dialogs & Overlays

## Toast Notification

```kotlin
onClick {
    toast("Operation successful!")
}
```

## Alert Dialog

```kotlin
onClick {
    alert("Are you sure you want to delete this?")
}
```

## Confirm Dialog

```kotlin
onClick {
    val confirmed = confirm("Delete this item?")
    if (confirmed) {
        deleteItem()
    }
}
```

## Custom Dialog

Use `dialog { close -> }` pattern for modal dialogs:

```kotlin
onClick {
    dialog { close ->  // close callback provided automatically
        card.col {
            h2("Custom Dialog")
            text("Dialog content here")
            row {
                button {
                    text("Cancel")
                    onClick { close() }
                }
                important.button {
                    text("Confirm")
                    onClick {
                        // Do something
                        close()
                    }
                }
            }
        }
    }
}
```

**Benefits:**
- No manual Signal needed to track dialog state
- Automatic overlay/background handling
- `close()` callback provided automatically
- Cleaner than manual `dismissBackground` + `shownWhen` pattern

## Bottom Sheet

Bottom sheets are modal overlays that slide up from the bottom of the screen, commonly used for forms and option menus.

```kotlin
onClick {
    openBottomSheet {
        col {
            h3 { content = "Options" }
            subtext { content = "Select an option:" }
            button {
                text { content = "Option 1" }
                onClick { /* ... */ }
            }
            button {
                text { content = "Option 2" }
                onClick { /* ... */ }
            }
        }
    }
}
```

**Closing behavior:**
- User taps outside the sheet → closes automatically
- User navigates to another page → closes automatically
- Call `dismissBackground()` from inside the sheet to close programmatically

### Form in Bottom Sheet

```kotlin
fun ViewWriter.EditItemSheet(item: Item, onSave: () -> Unit) {
    val nameInput = Signal(item.name)
    val descriptionInput = Signal(item.description)

    col {
        h2 { content = "Edit Item" }

        field("Name") {
            textInput {
                content bind nameInput
            }
        }

        field("Description") {
            textArea {
                content bind descriptionInput
            }
        }

        row {
            button {
                text { content = "Cancel" }
                onClick { dismissBackground() }  // Close sheet
            }
            important.button {
                text { content = "Save" }
                action = Action("Save") {
                    val session = currentSession.await() ?: return@Action
                    session.items[item._id].modify(modification {
                        it.name assign nameInput.value
                        it.description assign descriptionInput.value
                    })
                    onSave()
                    dismissBackground()  // Close sheet after save
                }
            }
        }

        subtext { content = "Tap outside to close" }
    }
}

// Usage
button {
    text { content = "Edit" }
    onClick {
        openBottomSheet {
            EditItemSheet(item) {
                // Refresh list after save
                loadItems()
            }
        }
    }
}
```

**Note**: Use `openBottomSheet { }` (not `bottomSheet { }`). The function name indicates it opens a new sheet.

## Confirm Danger Dialog

For destructive actions that require extra confirmation:

```kotlin
onClick {
    confirmDanger("Delete Project", "This cannot be undone. Are you sure?") {
        // Only runs if user confirms
        api.deleteProject(projectId)
        toast("Project deleted")
    }
}
```

## Menu Buttons with opensMenu

Create dropdown menus with complex content:

```kotlin
menuButton {
    icon(Icon.moreVert, "More options")
    requireClick = true  // Don't open on hover, only on click
    opensMenu {
        col {
            button {
                text("Edit")
                onClick { edit() }
            }
            button {
                text("Delete")
                onClick { delete() }
            }
            separator()
            row {
                checkbox { checked bind includeArchived }
                text("Include Archived")
            }
        }
    }
}

// Menu with dynamic theme
menuButton {
    dynamicTheme {
        if (hasUnread()) SelectedSemantic
        else null
    }
    icon(Icon.notification, "Notifications")
    opensMenu {
        sizeConstraints(width = 30.rem, height = 40.rem).col {
            // Complex menu content with recyclerView, etc.
        }
    }
}
```
