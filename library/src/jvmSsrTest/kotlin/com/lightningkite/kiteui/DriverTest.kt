package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.testing.parseFindLine
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.DriverActionException
import com.lightningkite.kiteui.views.DropTargetDelegate
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DriverTest {
    @Test
    fun snapshotShowsViewTree() = uiTest(
        content = {
            col {
                debugName = "main"
                text { content = "Hello World"; debugName = "greeting" }
                button {
                    debugName = "myButton"
                    text("Click Me")
                    onClick { }
                }
            }
        }
    ) {
        val snap = snapshot()
        println("Snapshot:\n$snap")
        assertTrue(snap.contains("greeting"), "Should contain greeting view: $snap")
        assertTrue(snap.contains("Hello World"), "Should contain text content: $snap")
        assertTrue(snap.contains("myButton"), "Should contain button name: $snap")
        assertTrue(snap.contains("click"), "Should show click action: $snap")
    }

    @Test
    fun clickButtonWorks() = uiTest(
        content = {
            col {
                val counter = Signal(0)
                text {
                    debugName = "count"
                    ::content { "Count: ${counter()}" }
                }
                button {
                    debugName = "increment"
                    text("Add")
                    onClick { counter.value++ }
                }
            }
        }
    ) {
        // Check initial state
        val snap1 = snapshot("count")
        println("Initial: $snap1")
        assertTrue(snap1.contains("Count: 0"), "Initial count should be 0: $snap1")

        // Click the button
        val result = click("increment")
        println("Click result: $result")
        assertTrue(result == "OK", "Click should succeed: $result")

        // Check updated state
        val snap2 = snapshot("count")
        println("After click: $snap2")
        assertTrue(snap2.contains("Count: 1"), "Count should be 1 after click: $snap2")
    }

    @Test
    fun textInputSetValue() = uiTest(
        content = {
            col {
                textInput {
                    debugName = "email"
                    hint = "Enter email"
                }
            }
        }
    ) {
        val result = setValue("email", "test@example.com")
        assertTrue(result == "OK", "setValue should succeed: $result")

        val snap = snapshot("email")
        println("After setValue: $snap")
        assertTrue(snap.contains("test@example.com"), "Should show new value: $snap")
    }

    @Test
    fun findViewsByName() = uiTest(
        content = {
            col {
                text { content = "First"; debugName = "alpha" }
                text { content = "Second"; debugName = "beta" }
                text { content = "Third"; debugName = "alpha-sub" }
            }
        }
    ) {
        val result = find("alpha")
        println("Find result:\n$result")
        assertTrue(result.contains("alpha"), "Should find alpha: $result")
        assertTrue(result.contains("alpha-sub"), "Should find alpha-sub: $result")
    }

    @Test
    fun scrollIntoViewReturnsOk() = uiTest(
        content = {
            col {
                text { content = "Target"; debugName = "target" }
            }
        }
    ) {
        val result = scrollIntoView("target")
        assertEquals("OK", result, "scrollIntoView should return OK")
    }

    @Test
    fun getDragDataAndDrop() = uiTest(
        content = {
            col {
                col {
                    debugName = "source"
                    dragData = DragData("test-label", "text/plain", "hello-world")
                }
                col {
                    debugName = "target"
                    dropTargetDelegate = object : DropTargetDelegate {
                        override fun drop(event: DragEvent): Boolean {
                            return event.data["text/plain"] == "hello-world"
                        }
                    }
                }
            }
        }
    ) {
        // Verify getDragData is available
        val snap = snapshot("source")
        println("Source snapshot: $snap")
        assertTrue(snap.contains("getDragData"), "Source should have getDragData action: $snap")

        // Get drag data (throws on error, so just call it)
        val dragData = getDragData("source")
        println("Drag data: $dragData")
        assertTrue(dragData.isNotBlank(), "getDragData should return base64 data")

        // Drop it on target
        val result = drop("target", dragData)
        println("Drop result: $result")
        assertEquals("OK", result, "Drop should succeed")
    }

    @Test
    fun dropRejected() = uiTest(
        content = {
            col {
                col {
                    debugName = "source"
                    dragData = DragData("test", "text/plain", "data")
                }
                col {
                    debugName = "rejecter"
                    dropTargetDelegate = object : DropTargetDelegate {
                        override fun drop(event: DragEvent): Boolean = false
                    }
                }
            }
        }
    ) {
        val dragData = getDragData("source")
        assertFailsWith<DriverActionException>("Drop should be rejected") {
            drop("rejecter", dragData)
        }
    }

    @Test
    fun resolveDriverPath() = uiTest(
        content = {
            col {
                debugName = "root-col"
                col {
                    debugName = "inner"
                    text { content = "Nested"; debugName = "deepText" }
                }
            }
        }
    ) {
        // Can resolve nested path
        val snap = snapshot("inner/deepText")
        println("Nested snapshot: $snap")
        assertTrue(snap.contains("Nested"), "Should resolve nested path: $snap")
    }

    @Test
    fun parentNavigation() = uiTest(
        content = {
            col {
                debugName = "outer"
                col {
                    debugName = "inner"
                    text { content = "Child"; debugName = "child" }
                }
            }
        }
    ) {
        // Navigate to child then back up to parent
        val snap = snapshot("child/..")
        println("Parent snapshot: $snap")
        assertTrue(snap.contains("inner"), "child/.. should resolve to inner: $snap")
        assertTrue(snap.contains("child"), "inner should still contain child: $snap")
    }

    @Test
    fun unknownActionThrows() = uiTest(
        content = {
            col {
                debugName = "target"
                text { content = "Hello" }
            }
        }
    ) {
        assertFailsWith<DriverActionException>("Unknown action should throw") {
            raw("target\tnonExistentAction")
        }
    }

    @Test
    fun viewNotFoundThrows() = uiTest(
        content = {
            col {
                text { content = "Hello" }
            }
        }
    ) {
        assertFailsWith<DriverActionException>("Missing view should throw") {
            raw("nonExistent\tsnapshot")
        }
    }

    @Test
    fun findByWidgetType() = uiTest(
        content = {
            col {
                textInput { debugName = "name"; hint = "Name" }
                text { content = "Label" }
                textInput { debugName = "email"; hint = "Email" }
                checkbox { debugName = "agree" }
            }
        }
    ) {
        val result = find("TextInput")
        println("Find by type: $result")
        assertTrue(result.contains("name"), "Should find name input: $result")
        assertTrue(result.contains("email"), "Should find email input: $result")
        assertTrue(!result.contains("Label"), "Should not match TextView: $result")
    }

    @Test
    fun findByAction() = uiTest(
        content = {
            col {
                button { debugName = "btn"; text("Go"); onClick { } }
                text { content = "Static" }
                checkbox { debugName = "chk" }
            }
        }
    ) {
        val result = find("toggle")
        println("Find by action: $result")
        assertTrue(result.contains("chk"), "Should find checkbox with toggle action: $result")
        assertTrue(!result.contains("btn"), "Button has click, not toggle: $result")
    }

    @Test
    fun setValuePreservesSpaces() = uiTest(
        content = {
            col {
                textInput { debugName = "field" }
            }
        }
    ) {
        val result = raw("field\tsetValue\thello world")
        assertEquals("OK", result)
        val snap = snapshot("field")
        println("After tab setValue: $snap")
        assertTrue(snap.contains("hello world"), "Value should contain space: $snap")
    }

    @Test
    fun screenshotNotSupportedOnJvmSsr() = uiTest(
        content = {
            col {
                debugName = "target"
                text { content = "Hello" }
            }
        }
    ) {
        assertFailsWith<DriverActionException>("JVM SSR screenshot should throw") {
            screenshot("target")
        }
    }

    @Test
    fun navigateThrowsForInvalidRoute() = uiTest(
        content = {
            col { text { content = "Home" } }
        }
    ) {
        // navigate command requires a navigator; our test scope has no navigator
        assertFailsWith<DriverActionException>("Navigate without navigator should throw") {
            navigate("/some/route")
        }
    }

    @Test
    fun urlThrowsWithoutNavigator() = uiTest(
        content = {
            col { text { content = "Home" } }
        }
    ) {
        assertFailsWith<DriverActionException>("URL without navigator should throw") {
            url()
        }
    }

    @Test
    fun backThrowsWithoutNavigator() = uiTest(
        content = {
            col { text { content = "Home" } }
        }
    ) {
        assertFailsWith<DriverActionException>("Back without navigator should throw") {
            back()
        }
    }

    // --- Mock external services ---

    @Test
    fun mockFileQueuedAndReturnedByRequestFile() = uiTest(
        content = {
            col {
                val fileName = Signal("")
                button {
                    debugName = "upload"
                    text("Upload")
                    onClick {
                        val file = context.requestFile(listOf("image/*"))
                        fileName.value = file?.fileName() ?: "cancelled"
                    }
                }
                text {
                    debugName = "result"
                    ::content { "File: ${fileName()}" }
                }
            }
        }
    ) {
        // Queue a mock file before clicking
        mockFile("hello".encodeToByteArray(), "text/plain", "test.txt")

        // Click the upload button — requestFile() returns the queued mock
        click("upload")

        // Verify the file was received
        val snap = snapshot("result")
        assertTrue(snap.contains("File: test.txt"), "Should show mock filename: $snap")

        // Verify the call was recorded
        val calls = mockCalls()
        assertTrue(calls.contains("RequestFile"), "Should record RequestFile call: $calls")
    }

    @Test
    fun mockFileCancelReturnsNull() = uiTest(
        content = {
            col {
                val status = Signal("")
                button {
                    debugName = "upload"
                    text("Upload")
                    onClick {
                        val file = context.requestFile()
                        status.value = if (file == null) "cancelled" else "got file"
                    }
                }
                text {
                    debugName = "status"
                    ::content { status() }
                }
            }
        }
    ) {
        mockFileCancel()
        click("upload")
        val snap = snapshot("status")
        assertTrue(snap.contains("cancelled"), "Should show cancelled: $snap")
    }

    @Test
    fun mockGeolocationReturnsMockedPosition() = uiTest(
        content = {
            col {
                val location = Signal("")
                button {
                    debugName = "locate"
                    text("Locate")
                    onClick {
                        val pos = context.getCurrentPosition()
                        location.value = "${pos.latitude},${pos.longitude}"
                    }
                }
                text {
                    debugName = "location"
                    ::content { location() }
                }
            }
        }
    ) {
        mockGeolocation(37.7749, -122.4194)
        click("locate")
        val snap = snapshot("location")
        assertTrue(snap.contains("37.7749"), "Should contain latitude: $snap")
        assertTrue(snap.contains("-122.4194"), "Should contain longitude: $snap")
    }

    @Test
    fun mockClearCallsResetsLog() = uiTest(
        content = {
            col {
                button {
                    debugName = "btn"
                    text("Go")
                    onClick { context.requestFile() }
                }
            }
        }
    ) {
        mockFileCancel()
        click("btn")
        val callsBefore = mockCalls()
        assertTrue(callsBefore.contains("RequestFile"), "Should have recorded call: $callsBefore")

        mockClearCalls()
        val callsAfter = mockCalls()
        assertEquals("No calls recorded", callsAfter, "Should be empty after clear")
    }

    @Test
    fun mockExternalServicesPassedToUiTest() {
        val mock = MockExternalServices()
        mock.pendingFileResponses.addLast(
            createFileReferenceFromBytes("direct".encodeToByteArray(), "text/plain", "direct.txt")
        )
        uiTest(
            mockExternalServices = mock,
            content = {
                col {
                    val name = Signal("")
                    button {
                        debugName = "pick"
                        text("Pick")
                        onClick {
                            val file = context.requestFile()
                            name.value = file?.fileName() ?: "none"
                        }
                    }
                    text {
                        debugName = "name"
                        ::content { name() }
                    }
                }
            }
        ) {
            click("pick")
            val snap = snapshot("name")
            assertTrue(snap.contains("direct.txt"), "Should use directly-provided mock: $snap")
        }
    }

    @Test
    fun logsReturnsEntries() = uiTest(
        content = {
            col { text { content = "Hello" } }
        }
    ) {
        // LogBuffer may or may not have entries, but the command should not throw
        val result = logs()
        // Result is either "No log entries" or formatted log lines
        assertTrue(result.isNotBlank(), "Logs should return something: $result")
    }

    @Test
    fun snapshotInteractiveFiltersNonInteractive() = uiTest(
        content = {
            col {
                debugName = "wrapper"
                text { content = "Static text"; debugName = "label" }
                button {
                    debugName = "action"
                    text("Do it")
                    onClick { }
                }
            }
        }
    ) {
        val full = snapshot()
        val interactive = interactiveSnapshot()
        println("Full:\n$full")
        println("Interactive:\n$interactive")
        // Full snapshot should contain both
        assertTrue(full.contains("label"), "Full should show label: $full")
        assertTrue(full.contains("action"), "Full should show button: $full")
        // Interactive snapshot should show the button (has click action) and named views
        assertTrue(interactive.contains("action"), "Interactive should show button: $interactive")
    }

    @Test
    fun snapshotHiddenFlag() = uiTest(
        content = {
            col {
                debugName = "container"
                text {
                    content = "Visible"
                    debugName = "vis"
                }
                text {
                    content = "Hidden"
                    debugName = "hid"
                    shown = false
                }
            }
        }
    ) {
        val normal = snapshot()
        println("Normal:\n$normal")
        assertTrue(normal.contains("vis"), "Normal should show visible: $normal")
        assertTrue(!normal.contains("hid"), "Normal should hide hidden view: $normal")

        val withHidden = raw("root\tsnapshot\t--hidden")
        println("With hidden:\n$withHidden")
        assertTrue(withHidden.contains("vis"), "Hidden flag should still show visible: $withHidden")
        assertTrue(withHidden.contains("hid"), "Hidden flag should show hidden view: $withHidden")
    }

    // --- Toggle / Select / Submit / LongClick ---

    @Test
    fun toggleCheckbox() = uiTest(
        content = {
            col {
                checkbox {
                    debugName = "agree"
                }
            }
        }
    ) {
        assertValue("agree", "false")
        toggle("agree")
        assertValue("agree", "true")
        toggle("agree")
        assertValue("agree", "false")
    }

    @Test
    fun toggleSwitch() = uiTest(
        content = {
            col {
                switch {
                    debugName = "notifications"
                }
            }
        }
    ) {
        assertValue("notifications", "false")
        toggle("notifications")
        assertValue("notifications", "true")
    }

    @Test
    fun toggleButton() = uiTest(
        content = {
            col {
                toggleButton {
                    debugName = "favorite"
                    text("Fav")
                }
            }
        }
    ) {
        assertValue("favorite", "false")
        toggle("favorite")
        assertValue("favorite", "true")
    }

    @Test
    fun selectRadioButton() = uiTest(
        content = {
            col {
                radioButton { debugName = "optA" }
                radioButton { debugName = "optB" }
            }
        }
    ) {
        assertValue("optA", "false")
        assertValue("optB", "false")
        select("optA")
        assertValue("optA", "true")
    }

    @Test
    fun submitTextInput() = uiTest(
        content = {
            col {
                val submitted = Signal("")
                textInput {
                    debugName = "search"
                    action = Action("Submit", Icon.send) { submitted.value = content.value }
                }
                text {
                    debugName = "result"
                    ::content { "Submitted: ${submitted()}" }
                }
            }
        }
    ) {
        setValue("search", "hello")
        submit("search")
        assertValue("result", "Submitted: hello")
    }

    @Test
    fun longClickButton() = uiTest(
        content = {
            col {
                val log = Signal("")
                button {
                    debugName = "btn"
                    text("Press")
                    onClick { log.value = "click" }
                    onLongClick { log.value = "longClick" }
                }
                text {
                    debugName = "log"
                    ::content { log() }
                }
            }
        }
    ) {
        longClick("btn")
        assertValue("log", "longClick")
    }

    @Test
    fun setValueOnCheckbox() = uiTest(
        content = {
            col {
                checkbox { debugName = "chk" }
            }
        }
    ) {
        assertValue("chk", "false")
        setValue("chk", "true")
        assertValue("chk", "true")
        setValue("chk", "false")
        assertValue("chk", "false")
    }

    @Test
    fun setValueOnSlider() = uiTest(
        content = {
            col {
                slider {
                    debugName = "vol"
                    min = 0f
                    max = 100f
                }
            }
        }
    ) {
        setValue("vol", "75.0")
        assertValue("vol", "75.0")
    }

    @Test
    fun selectDropdown() = uiTest(
        content = {
            col {
                val choice = Signal("Red")
                select {
                    debugName = "color"
                    bind(
                        edits = choice,
                        data = Signal(listOf("Red", "Green", "Blue")),
                        render = { it }
                    )
                }
                text {
                    debugName = "chosen"
                    ::content { "Color: ${choice()}" }
                }
            }
        }
    ) {
        assertValue("color", "Red")
        setValue("color", "Blue")
        assertValue("color", "Blue")
        val snap = snapshot("chosen")
        assertTrue(snap.contains("Color: Blue"), "Backing property should update: $snap")
    }

    @Test
    fun selectDropdownInvalidOption() = uiTest(
        content = {
            col {
                val choice = Signal("Red")
                select {
                    debugName = "color"
                    bind(
                        edits = choice,
                        data = Signal(listOf("Red", "Green", "Blue")),
                        render = { it }
                    )
                }
            }
        }
    ) {
        assertFailsWith<DriverActionException>("Should throw for invalid option") {
            setValue("color", "Purple")
        }
    }

    // --- Date/Time parsing errors ---

    @Test
    fun localDateInvalidFormat() = uiTest(
        content = {
            col {
                localDateField { debugName = "date" }
            }
        }
    ) {
        val e = assertFailsWith<DriverActionException>("Invalid date should throw") {
            setValue("date", "not-a-date")
        }
        assertTrue(e.message!!.contains("not-a-date"), "Error should mention the bad input: ${e.message}")
    }

    @Test
    fun localTimeInvalidFormat() = uiTest(
        content = {
            col {
                localTimeField { debugName = "time" }
            }
        }
    ) {
        val e = assertFailsWith<DriverActionException>("Invalid time should throw") {
            setValue("time", "bad-time")
        }
        assertTrue(e.message!!.contains("bad-time"), "Error should mention the bad input: ${e.message}")
    }

    @Test
    fun localDateTimeInvalidFormat() = uiTest(
        content = {
            col {
                localDateTimeField { debugName = "dt" }
            }
        }
    ) {
        val e = assertFailsWith<DriverActionException>("Invalid datetime should throw") {
            setValue("dt", "nope")
        }
        assertTrue(e.message!!.contains("nope"), "Error should mention the bad input: ${e.message}")
    }

    // --- Assertion helpers ---

    @Test
    fun assertVisiblePasses() = uiTest(
        content = {
            col {
                text { content = "Hello"; debugName = "vis" }
            }
        }
    ) {
        assertVisible("vis")
    }

    @Test
    fun assertVisibleFailsOnHidden() = uiTest(
        content = {
            col {
                text { content = "Hidden"; debugName = "hid"; shown = false }
            }
        }
    ) {
        assertFailsWith<AssertionError>("assertVisible on hidden view should fail") {
            // Use --hidden flag so the view is resolvable but marked hidden
            raw("root\tsnapshot\t--hidden")  // warm up
            assertVisible("hid")
        }
    }

    @Test
    fun assertVisibleFailsOnMissing() = uiTest(
        content = {
            col { text { content = "Hello" } }
        }
    ) {
        assertFailsWith<AssertionError>("assertVisible on missing view should fail") {
            assertVisible("nonexistent")
        }
    }

    @Test
    fun assertNotVisibleForHidden() = uiTest(
        content = {
            col {
                text { content = "Hidden"; debugName = "hid"; shown = false }
            }
        }
    ) {
        // Hidden view can't be resolved unless --hidden flag is used, so it throws DriverActionException → treated as not visible
        assertNotVisible("hid")
    }

    @Test
    fun assertNotVisibleFailsOnVisible() = uiTest(
        content = {
            col {
                text { content = "Hello"; debugName = "vis" }
            }
        }
    ) {
        assertFailsWith<AssertionError>("assertNotVisible on visible view should fail") {
            assertNotVisible("vis")
        }
    }

    // --- FindResult / findAll / findWithAction ---

    @Test
    fun findAllParsesResults() = uiTest(
        content = {
            col {
                textInput { debugName = "email"; hint = "Email" }
                button { debugName = "submit"; text("Go"); onClick { } }
            }
        }
    ) {
        val results = findAll("email")
        assertTrue(results.isNotEmpty(), "Should find at least one result")
        val first = results.first()
        assertEquals("email", first.path)
        assertEquals("email", first.name)
        assertEquals("TextInput", first.type)
        assertTrue("setValue" in first.actions, "TextInput should have setValue action: ${first.actions}")
    }

    @Test
    fun findAllEmptyForNoMatch() = uiTest(
        content = {
            col { text { content = "Hello" } }
        }
    ) {
        val results = findAll("nonexistent_xyz")
        assertTrue(results.isEmpty(), "Should return empty for no match: $results")
    }

    @Test
    fun findWithActionFilters() = uiTest(
        content = {
            col {
                text { content = "Login"; debugName = "label" }
                button { debugName = "loginBtn"; text("Login"); onClick { } }
                textInput { debugName = "loginField" }
            }
        }
    ) {
        // "Login" matches the label text, button text, and possibly button name
        val clickable = findWithAction("login", "click")
        assertNotNull(clickable, "Should find a clickable result")
        assertTrue("click" in clickable.actions)

        val settable = findWithAction("login", "setValue")
        assertNotNull(settable, "Should find a settable result for loginField")
        assertTrue("setValue" in settable.actions)
    }

    @Test
    fun parseFindLineHandlesValueAndActions() {
        val line = """email: email: TextInput = "test@example.com" [setValue, submit]"""
        val result = parseFindLine(line)
        assertNotNull(result)
        assertEquals("email", result.path)
        assertEquals("email", result.name)
        assertEquals("TextInput", result.type)
        assertEquals("test@example.com", result.value)
        assertEquals(setOf("setValue", "submit"), result.actions)
    }

    @Test
    fun parseFindLineHandlesNoValueNoActions() {
        val line = "myLabel: myLabel: TextView"
        val result = parseFindLine(line)
        assertNotNull(result)
        assertEquals("myLabel", result.path)
        assertEquals("TextView", result.type)
        assertNull(result.value)
        assertTrue(result.actions.isEmpty())
    }

    // --- findClickable ---

    @Test
    fun findClickableWalksUpToButton() = uiTest(
        content = {
            col {
                button {
                    debugName = "loginBtn"
                    text("Login")
                    onClick { }
                }
            }
        }
    ) {
        // "Login" matches the Text inside the button, but findClickable should return the button
        val results = findClickable("Login")
        assertTrue(results.isNotEmpty(), "Should find clickable results: $results")
        val first = results.first()
        assertEquals("loginBtn", first.path, "Should resolve to button path")
        assertTrue("click" in first.actions, "Result should have click action: ${first.actions}")
    }

    @Test
    fun findClickableNoDuplicates() = uiTest(
        content = {
            col {
                button {
                    debugName = "btn"
                    text("Save")
                    text("Changes")
                    onClick { }
                }
            }
        }
    ) {
        // Both "Save" text children match, but the button should appear only once
        // We search for the button's debugName which matches the button itself
        val results = findClickable("btn")
        assertEquals(1, results.size, "Should deduplicate to one result: $results")
    }

    @Test
    fun findClickableNoMatch() = uiTest(
        content = {
            col {
                text { content = "Orphan text"; debugName = "orphan" }
            }
        }
    ) {
        // Text has no clickable ancestor
        val results = findClickable("Orphan")
        assertTrue(results.isEmpty(), "Should return empty when no clickable ancestor: $results")
    }

    // --- waitForId ---

    @Test
    fun waitForIdSucceeds() = uiTest(
        content = {
            col {
                text { content = "Present"; debugName = "target" }
            }
        }
    ) {
        waitForId("target")  // Should not throw
    }

    @Test
    fun waitForIdTimeoutThrows() = uiTest(
        content = {
            col { text { content = "Hello" } }
        }
    ) {
        assertFailsWith<AssertionError>("Should throw on timeout") {
            waitForId("nonexistent", timeoutMs = 200)
        }
    }

    // --- assertTextVisible / assertIdExists ---

    @Test
    fun assertTextVisiblePasses() = uiTest(
        content = {
            col { text { content = "Welcome back" } }
        }
    ) {
        assertTextVisible("Welcome back")
    }

    @Test
    fun assertTextVisibleFails() = uiTest(
        content = {
            col { text { content = "Hello" } }
        }
    ) {
        assertFailsWith<AssertionError>("Should fail for missing text") {
            assertTextVisible("Goodbye")
        }
    }

    @Test
    fun assertIdExistsPasses() = uiTest(
        content = {
            col { text { content = "Hi"; debugName = "greeting" } }
        }
    ) {
        assertIdExists("greeting")
    }

    @Test
    fun assertIdExistsFails() = uiTest(
        content = {
            col { text { content = "Hi" } }
        }
    ) {
        assertFailsWith<AssertionError>("Should fail for missing id") {
            assertIdExists("nonexistent")
        }
    }

    // --- find visibility filtering ---

    @Test
    fun findExcludesHiddenViewsByDefault() = uiTest(
        content = {
            col {
                textInput { debugName = "visible"; hint = "Visible" }
                textInput { debugName = "hidden"; hint = "Hidden"; shown = false }
            }
        }
    ) {
        val results = findAll("TextInput")
        assertTrue(results.any { it.name == "visible" }, "Should find visible input: $results")
        assertTrue(results.none { it.name == "hidden" }, "Should not find hidden input by default: $results")

        val allResults = findAll("TextInput", includeHidden = true)
        assertTrue(allResults.any { it.name == "visible" }, "includeHidden should still show visible: $allResults")
        assertTrue(allResults.any { it.name == "hidden" }, "includeHidden should show hidden input: $allResults")
    }

    @Test
    fun findClickableExcludesHiddenByDefault() = uiTest(
        content = {
            col {
                button { debugName = "visBtn"; text("Visible"); onClick { } }
                button { debugName = "hidBtn"; text("Hidden"); onClick { }; shown = false }
            }
        }
    ) {
        val results = findClickable("Btn")
        assertTrue(results.any { it.name == "visBtn" }, "Should find visible button: $results")
        assertTrue(results.none { it.name == "hidBtn" }, "Should not find hidden button by default: $results")
    }

    // --- path consistency: findAll paths work directly with click ---

    @Test
    fun findPathsWorkDirectlyWithClick() = uiTest(
        content = {
            // No debugName on any container — tests that numeric paths from findAll are usable
            col {
                col {
                    val counter = Signal(0)
                    button {
                        text("Go")
                        onClick { counter.value++ }
                    }
                    text {
                        debugName = "count"
                        ::content { "Count: ${counter()}" }
                    }
                }
            }
        }
    ) {
        val results = findAll("Button")
        assertTrue(results.isNotEmpty(), "Should find the Button: $results")
        val buttonPath = results.first().path

        // Path returned by findAll must work directly with click() without any fixup
        click(buttonPath)
        val snap = snapshot("count")
        assertTrue(snap.contains("Count: 1"), "Click via findAll path should increment counter: $snap")
    }
}
