package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.widgets.code

@Routable("docs/dialogs")
object DialogsAndModalsPage : DocPage {
    override val covers: List<String> = listOf(
        "dialog", "modal", "popup", "overlay", "alert", "confirm",
        "dialog box", "popup window", "modal window", "confirmation",
        "toast", "bottom sheet", "popover", "menu"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Dialogs and Modals") {
                text("Toasts, dialogs, bottom sheets, popovers and menus all open in the app's overlay, which appBase sets up.")

                space()

                titledSection("Dialog Basics") {
                    card.col {
                        text("• Each is a function on ElementContext: call context.dialog { }, context.toast(...), and so on")
                        text("• context is in scope anywhere inside an element's builder, including onClick { } and Action { }")
                        text("• Dialogs are centered over a dimmed background and themed with DialogSemantic")
                        text("• Tapping the background or pressing back closes a dialog; pass dismissable = false to prevent both")
                    }
                }

                space()

                titledSection("Simple Dialog") {
                    text("Open a dialog from a click; the block receives a function that closes it:")

                    space()
                    code {
                        content = """
                            button {
                                text("Open Dialog")
                                onClick {
                                    context.dialog { close ->
                                        col {
                                            h3("Dialog Title")
                                            text("This is the dialog content")
                                            row {
                                                expanding.button {
                                                    centered.text("Cancel")
                                                    onClick { close() }
                                                }
                                                expanding.important.button {
                                                    centered.text("OK")
                                                    onClick { close() }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Confirmation Dialog") {
                    text("The block runs only if the user confirms; the dialog shows progress until it finishes:")

                    space()
                    code {
                        content = """
                            danger.button {
                                text("Delete Item")
                                onClick {
                                    context.confirmDanger(
                                        "Confirm Delete",
                                        "Are you sure you want to delete this item?",
                                        actionName = "Delete",
                                    ) {
                                        deleteItem()
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Alert Dialog") {
                    text("A message with an OK button:")

                    space()
                    code {
                        content = """
                            button {
                                text("Save")
                                onClick {
                                    saveItem()
                                    context.alert("Success", "Item saved successfully!")
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Input Dialog") {
                    text("Dialog with user input:")

                    space()
                    code {
                        content = """
                            button {
                                text("Rename Item")
                                onClick {
                                    context.dialog { close ->
                                        col {
                                            val newName = Signal(currentName)
                                            h3("Rename Item")
                                            field("New Name") {
                                                textInput {
                                                    hint = "Enter new name"
                                                    keyboardHints = KeyboardHints.title
                                                    content bind newName
                                                }
                                            }
                                            row {
                                                expanding.button {
                                                    centered.text("Cancel")
                                                    onClick { close() }
                                                }
                                                expanding.important.button {
                                                    centered.text("Rename")
                                                    onClick {
                                                        renameItem(newName())
                                                        close()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Loading and Errors") {
                    text("onClick and Action already show a working state while they run, and errors they throw are shown in a dialog. You don't need a loading dialog or a try/catch:")

                    space()
                    code {
                        content = """
                            button {
                                text("Save Data")
                                onClick {
                                    saveData()          // button shows progress meanwhile
                                    context.toast("Saved successfully!")
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Bottom Sheet") {
                    text("A sheet that slides up from the bottom:")

                    space()
                    code {
                        content = """
                            button {
                                text("Options")
                                onClick {
                                    context.coordinatorFrame.bottomSheet(startState = BottomSheetState.PARTIALLY_EXPANDED) { control ->
                                        themed(DialogSemantic).col {
                                            centered.coordinatorDragHandle()
                                            h3("Options")
                                            button {
                                                text("Edit")
                                                onClick {
                                                    editItem()
                                                    control.close()
                                                }
                                            }
                                            danger.button {
                                                text("Delete")
                                                onClick {
                                                    deleteItem()
                                                    control.close()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Menu") {
                    text("A button that opens a menu next to itself:")

                    space()
                    code {
                        content = """
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
                                                deleteItem()
                                            }
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Popover") {
                    text("Show a popover next to any element:")

                    space()
                    code {
                        content = """
                            button {
                                text("Show Info")
                                onClick {
                                    openPopover(PopoverPreferredDirection.aboveCenter) {
                                        text("Additional information here")
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Toast Notifications") {
                    text("Non-blocking notifications, shown at the bottom center for 3 seconds by default:")

                    space()
                    code {
                        content = """
                            button {
                                text("Notify")
                                onClick { context.toast("Operation completed") }
                            }

                            // Custom content and duration
                            button {
                                text("Upload")
                                onClick {
                                    uploadFiles()
                                    context.toast(5.seconds) {
                                        row {
                                            icon(Icon.done, "Done")
                                            text("Uploaded 3 files")
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Dialog Sizing") {
                    text("Size the dialog's content; scroll the long part and keep the actions visible:")

                    space()
                    code {
                        content = """
                            context.dialog { close ->
                                sizeConstraints(width = 30.rem).col {
                                    h3("Long Content")
                                    sizeConstraints(maxHeight = 30.rem).scrolling.col {
                                        // Lots of content...
                                    }
                                    row {
                                        expanding.button {
                                            centered.text("Cancel")
                                            onClick { close() }
                                        }
                                        expanding.important.button {
                                            centered.text("OK")
                                            onClick { close() }
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Raw Overlay") {
                    text("The building block the others use. modal = false leaves the UI underneath interactive:")

                    space()
                    code {
                        content = """
                            context.overlay(modal = false) { close ->
                                atTopEnd.card.row {
                                    text("Update available")
                                    button {
                                        icon(Icon.close, "Dismiss")
                                        onClick { close() }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Best Practices") {
                    card.col {
                        text("✓ Prefer context.dialog to a shownWhen overlay you manage with a Signal: it handles the background, back button, focus and stacking")
                        text("✓ Don't wrap dialog content in card: the dialog already applies DialogSemantic")
                        text("✓ Confirm destructive actions with context.confirmDanger")
                        text("✓ Use toasts for non-critical notifications")
                        text("✓ Make primary actions prominent (important modifier)")
                        text("✓ Back closes the top dialog before it pops a page")
                    }
                }
            }
        }
    }
}
