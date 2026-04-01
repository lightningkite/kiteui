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
        "dialog box", "popup window", "modal window", "confirmation"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Dialogs and Modals") {
                text("Learn how to create dialogs, modals, and popups in KiteUI.")

                space()

                titledSection("Dialog Basics") {
                    text("Dialogs in KiteUI are views that appear on top of the main content. They use the 'dialog' semantic theme modifier.")

                    space()
                    h5("Key Concepts:")
                    card.col {
                        text("• Dialogs block interaction with content beneath them")
                        text("• Use Signal to control visibility")
                        text("• 'dialog' modifier applies appropriate styling")
                        text("• Dismiss on background click or explicit close action")
                    }
                }

                space()

                titledSection("Simple Dialog") {
                    text("Create a basic dialog with shownWhen modifier:")

                    space()
                    code {
                        content = """
                            val showDialog = Signal(false)

                            button {
                                text("Open Dialog")
                                onClick { showDialog.value = true }
                            }

                            shownWhen { showDialog() }.dialog.card.col {
                                h3("Dialog Title")
                                text("This is the dialog content")

                                space()
                                row {
                                    expanding.button {
                                        text("Cancel")
                                        onClick { showDialog.value = false }
                                    }
                                    important.button {
                                        text("OK")
                                        onClick {
                                            // Perform action
                                            showDialog.value = false
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Confirmation Dialog") {
                    text("A reusable confirmation dialog pattern:")

                    space()
                    code {
                        content = """
                            fun ViewWriter.confirmDialog(
                                title: String,
                                message: String,
                                confirmText: String = "Confirm",
                                cancelText: String = "Cancel",
                                onConfirm: () -> Unit,
                                onCancel: () -> Unit = {}
                            ): Unit = run {
                                dialog.card.col {
                                    h3(title)
                                    text(message)

                                    space()
                                    row {
                                        expanding.button {
                                            text(cancelText)
                                            onClick { onCancel() }
                                        }
                                        important.button {
                                            text(confirmText)
                                            onClick { onConfirm() }
                                        }
                                    }
                                }
                            }

                            // Usage
                            val showConfirm = Signal(false)

                            button {
                                text("Delete Item")
                                onClick { showConfirm.value = true }
                            }

                            shownWhen { showConfirm() }.confirmDialog(
                                title = "Confirm Delete",
                                message = "Are you sure you want to delete this item?",
                                onConfirm = {
                                    deleteItem()
                                    showConfirm.value = false
                                },
                                onCancel = {
                                    showConfirm.value = false
                                }
                            )
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Alert Dialog") {
                    text("Simple alert with single action:")

                    space()
                    code {
                        content = """
                            fun ViewWriter.alert(
                                title: String,
                                message: String,
                                onDismiss: () -> Unit
                            ): Unit = run {
                                dialog.card.col {
                                    h3(title)
                                    text(message)

                                    space()
                                    important.button {
                                        text("OK")
                                        onClick { onDismiss() }
                                    }
                                }
                            }

                            // Usage
                            val showAlert = Signal(false)

                            shownWhen { showAlert() }.alert(
                                title = "Success",
                                message = "Item saved successfully!"
                            ) {
                                showAlert.value = false
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
                            val showInputDialog = Signal(false)
                            val inputValue = Signal("")

                            button {
                                text("Rename Item")
                                onClick {
                                    inputValue.value = currentName
                                    showInputDialog.value = true
                                }
                            }

                            shownWhen { showInputDialog() }.dialog.card.col {
                                h3("Rename Item")

                                field("New Name") {
                                    textInput {
                                        content bind inputValue
                                        hint = "Enter new name"
                                    }
                                }

                                space()
                                row {
                                    expanding.button {
                                        text("Cancel")
                                        onClick { showInputDialog.value = false }
                                    }
                                    important.button {
                                        text("Rename")
                                        onClick {
                                            renameItem(inputValue())
                                            showInputDialog.value = false
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Form Dialog") {
                    text("Dialog with multiple form fields:")

                    space()
                    code {
                        content = """
                            val showFormDialog = Signal(false)
                            val formData = Signal(FormData())

                            shownWhen { showFormDialog() }.dialog.card.col {
                                h3("Add New User")

                                field("Name") {
                                    textInput {
                                        content bind formData.lens { it.name } { copy(name = it) }
                                    }
                                }

                                field("Email") {
                                    textInput {
                                        content bind formData.lens { it.email } { copy(email = it) }
                                        keyboardHints = KeyboardHints.email
                                    }
                                }

                                field("Role") {
                                    select {
                                        bind(
                                            formData.lens { it.role } { copy(role = it) },
                                            listOf("Admin", "User", "Guest")
                                        ) { it }
                                    }
                                }

                                space()
                                row {
                                    expanding.button {
                                        text("Cancel")
                                        onClick { showFormDialog.value = false }
                                    }
                                    important.button {
                                        text("Create")
                                        onClick {
                                            createUser(formData())
                                            showFormDialog.value = false
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Dismiss on Background Click") {
                    text("Close dialog when clicking outside:")

                    space()
                    code {
                        content = """
                            val showDialog = Signal(false)

                            // Overlay that closes on click
                            shownWhen { showDialog() }.frame {
                                // Background overlay
                                onClick { showDialog.value = false }

                                // Dialog content - stop propagation
                                centered.dialog.card.col {
                                    onClick { it.stopPropagation() }

                                    h3("Click outside to close")
                                    text("Dialog content here")

                                    button {
                                        text("Close")
                                        onClick { showDialog.value = false }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Loading Dialog") {
                    text("Show loading state during async operations:")

                    space()
                    code {
                        content = """
                            val isLoading = Signal(false)

                            button {
                                text("Save Data")
                                onClick {
                                    isLoading.value = true
                                    launch {
                                        try {
                                            saveData()
                                            toast("Saved successfully!")
                                        } catch (e: Exception) {
                                            toast("Error: ${'$'}{e.message}")
                                        } finally {
                                            isLoading.value = false
                                        }
                                    }
                                }
                            }

                            shownWhen { isLoading() }.dialog.card.col {
                                activityIndicator()
                                space()
                                text("Saving...")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Bottom Sheet (Mobile-Style)") {
                    text("Create a bottom sheet dialog:")

                    space()
                    code {
                        content = """
                            val showBottomSheet = Signal(false)

                            shownWhen { showBottomSheet() }.frame {
                                onClick { showBottomSheet.value = false }

                                // Position at bottom
                                atBottom.dialog.card.col {
                                    onClick { it.stopPropagation() }

                                    h3("Options")

                                    button {
                                        text("Edit")
                                        onClick {
                                            editItem()
                                            showBottomSheet.value = false
                                        }
                                    }

                                    button {
                                        text("Share")
                                        onClick {
                                            shareItem()
                                            showBottomSheet.value = false
                                        }
                                    }

                                    danger.button {
                                        text("Delete")
                                        onClick {
                                            deleteItem()
                                            showBottomSheet.value = false
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Popover (Positioned Dialog)") {
                    text("Show a popover next to an element:")

                    space()
                    code {
                        content = """
                            val showPopover = Signal(false)

                            row {
                                button {
                                    text("Show Info")
                                    onClick { showPopover.value = !showPopover() }
                                }

                                // Position relative to button
                                shownWhen { showPopover() }.popover.card.col {
                                    text("Additional information here")
                                    text("This appears next to the button")

                                    button {
                                        text("Got it")
                                        onClick { showPopover.value = false }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Toast Notifications") {
                    text("Simple non-blocking notifications:")

                    space()
                    code {
                        content = """
                            // Simple toast
                            toast("Operation completed")

                            // Toast with duration
                            toast("Saved successfully!", 3000)

                            // Different semantic types
                            button {
                                text("Show Success")
                                onClick { toast("Success message!") }
                            }

                            danger.button {
                                text("Show Error")
                                onClick { toast("Error occurred!") }
                            }

                            warning.button {
                                text("Show Warning")
                                onClick { toast("Warning: Check this") }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Dialog State Management") {
                    text("Manage multiple dialogs:")

                    space()
                    code {
                        content = """
                            sealed class DialogState {
                                object None : DialogState()
                                data class Confirm(val message: String) : DialogState()
                                data class Edit(val item: Item) : DialogState()
                                object Loading : DialogState()
                            }

                            val dialogState = Signal<DialogState>(DialogState.None)

                            // Show different dialogs based on state
                            when (val state = dialogState()) {
                                is DialogState.None -> { }
                                is DialogState.Confirm -> {
                                    shownWhen { true }.confirmDialog(
                                        title = "Confirm",
                                        message = state.message,
                                        onConfirm = {
                                            performAction()
                                            dialogState.value = DialogState.None
                                        },
                                        onCancel = {
                                            dialogState.value = DialogState.None
                                        }
                                    )
                                }
                                is DialogState.Edit -> {
                                    // Edit dialog
                                }
                                is DialogState.Loading -> {
                                    // Loading dialog
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Dialog Sizing") {
                    text("Control dialog size and scrolling:")

                    space()
                    code {
                        content = """
                            shownWhen { showDialog() }.dialog.card.col {
                                // Fixed width
                                sizeConstraints(width = 400.dp)

                                // Max height with scrolling
                                sizeConstraints(maxHeight = 600.dp).scrolling.col {
                                    h3("Long Content")
                                    // Lots of content...
                                }

                                // Actions always visible at bottom
                                row {
                                    expanding.button { text("Cancel") }
                                    important.button { text("OK") }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Backdrop and Theming") {
                    text("Customize dialog appearance:")

                    space()
                    code {
                        content = """
                            // Centered with backdrop
                            shownWhen { showDialog() }.frame {
                                // Semi-transparent backdrop
                                hasPopIn = true

                                centered.dialog.card.col {
                                    h3("Styled Dialog")
                                    text("Custom themed dialog")
                                }
                            }

                            // Different semantic themes
                            shownWhen { showWarning() }.dialog.warning.card.col {
                                h3("Warning")
                                text("This is a warning dialog")
                            }

                            shownWhen { showError() }.dialog.danger.card.col {
                                h3("Error")
                                text("An error occurred")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Multi-Step Dialogs (Wizard)") {
                    text("Create a wizard with multiple steps:")

                    space()
                    code {
                        content = """
                            val currentStep = Signal(0)
                            val wizardData = Signal(WizardData())

                            shownWhen { showWizard() }.dialog.card.col {
                                h3("Setup Wizard")

                                when (currentStep()) {
                                    0 -> {
                                        text("Step 1: Basic Info")
                                        field("Name") {
                                            textInput {
                                                content bind wizardData.lens { it.name } { copy(name = it) }
                                            }
                                        }
                                    }
                                    1 -> {
                                        text("Step 2: Preferences")
                                        // Preferences fields
                                    }
                                    2 -> {
                                        text("Step 3: Review")
                                        // Show summary
                                    }
                                }

                                space()
                                row {
                                    if (currentStep() > 0) {
                                        button {
                                            text("Back")
                                            onClick { currentStep.value-- }
                                        }
                                    }

                                    expanding.space()

                                    if (currentStep() < 2) {
                                        important.button {
                                            text("Next")
                                            onClick { currentStep.value++ }
                                        }
                                    } else {
                                        important.button {
                                            text("Finish")
                                            onClick {
                                                submitWizard(wizardData())
                                                showWizard.value = false
                                            }
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Best Practices") {
                    card.col {
                        text("✓ Use semantic modifiers (dialog, warning, danger) for appropriate context")
                        text("✓ Always provide a way to dismiss dialogs")
                        text("✓ Keep dialog content focused - avoid overwhelming users")
                        text("✓ Use loading dialogs for operations that take time")
                        text("✓ Confirm destructive actions with confirmation dialogs")
                        text("✓ Use toasts for non-critical notifications")
                        text("✓ Make primary actions prominent (important modifier)")
                        text("✓ Position buttons consistently (Cancel left, OK/Confirm right)")
                        text("✓ Test dialogs on both mobile and desktop")
                        text("✓ Consider accessibility - keyboard navigation and screen readers")
                    }
                }

                space()

                titledSection("Common Patterns Summary") {
                    card.col {
                        h5("Simple Alert:")
                        code {
                            content = """
                                shownWhen { show() }.dialog.card.col {
                                    h3("Title")
                                    text("Message")
                                    button { text("OK"); onClick { show.value = false } }
                                }
                            """.trimIndent()
                        }

                        space()
                        h5("Confirmation:")
                        code {
                            content = """
                                shownWhen { show() }.dialog.card.col {
                                    h3("Confirm?")
                                    text("Message")
                                    row {
                                        button { text("Cancel"); onClick { show.value = false } }
                                        important.button { text("OK"); onClick { action(); show.value = false } }
                                    }
                                }
                            """.trimIndent()
                        }

                        space()
                        h5("Loading:")
                        code {
                            content = """
                                shownWhen { loading() }.dialog.card.col {
                                    activityIndicator()
                                    text("Loading...")
                                }
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}
