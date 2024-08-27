package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.lensing.WritableList
import com.lightningkite.kiteui.reactive.lensing.WritableListWithoutMap
import com.lightningkite.kiteui.reactive.lensing.lensByElement
import com.lightningkite.kiteui.reactive.lensing.map
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.icon

@Routable("/nesting")
object NestingEditsScreen: Screen {
    private data class TestData(
        val name: String,
        val nested: List<TestData>,
        val depth: Int = 0
    ) {
        override fun toString(): String = buildString {
            appendLine(name)
            if (nested.isNotEmpty()) {
                for (child in nested) {
                    append("-".repeat((depth + 1)*2) + " " + child)
                }
            }
        }

        fun blankChild(): TestData = TestData("$name Child", emptyList(), depth + 1)
    }

    private class TestDataBuilder(val name: String, val depth: Int = 0) {
        private val children = ArrayList<TestDataBuilder>()

        fun child(name: String? = null, setup: TestDataBuilder.() -> Unit = {}) {
            val childName = name ?: "$depth-${children.size + 1}"

            TestDataBuilder(childName, depth + 1).also {
                it.setup()
                children.add(it)
            }
        }

        val data: TestData
            get() = TestData(
                name = name,
                nested = children.map { it.data },
                depth = depth
            )
    }
    private fun testData(setup: TestDataBuilder.() -> Unit): TestData = TestDataBuilder("Root").apply { setup() }.data

    private fun Writable<TestData>.getName() =
        map(
            get = { it.name },
            set = { old: TestData, value: String -> old.copy(name = value) }
        )

    private fun Writable<TestData>.getNested(): WritableListWithoutMap<TestData, TestData> =
        map(
            get = { it.nested },
            set = { old: TestData, it: List<TestData> -> old.copy(nested = it) }
        ).lensByElement { it }

    private fun RView.child(data: Writable<TestData>, remove: suspend () -> Unit) {
        card - col {
            spacing = 1.5.rem
            val name = data.getName()

            row {
                expanding - label {
                    content = "Name"

                    expanding - fieldTheme - textField {
                        content bind name
                    }
                }

                buttonTheme - button {
                    spacing = 0.5.rem
                    centered - icon(Icon.delete, "Remove Child")

                    onClick(remove)
                }
            }

            col {
                val nested = data.getNested()
                val dataExists = shared { nested().isNotEmpty() }

                h6("Children")
                col {
                    reactiveScope {
                        clearChildren()
                        if (dataExists()) {
                            forEachUpdating(nested) {
                                child(it.flatten()) {
//                                    nested.remove(it)
                                }
                            }
                        }
                    }
                }
                important - button {
                    spacing = 0.rem
                    centered - row {
                        spacing = 0.5.rem
                        centered - icon { source = Icon.add }
                        centered - text {
                            ::content { "Add Child to ${name()}" }
                        }
                    }

                    onClick {
                        nested.add(data().blankChild())
                    }
                }
            }
        }
    }

    private val data = testData {
        child("Trogdor")
        child("Peralta")
        child("Parent") {
            child() {
                child()
                child()
            }
            child {
                child()
                child()
            }
        }
    }

    private val draft = Draft(data)

    override fun ViewWriter.render() {
        row {
            weight(1f) - col {
                expanding - card - scrolls - col {
                    h3("Saved Value")

                    text { ::content { draft.published().toString() } }
                }

                expanding - card - scrolls - col {
                    h3("Unsaved")

                    text { ::content { draft().toString() } }
                }
            }

            weight(2f) - stack {
                scrolls - col {
                    themeChoice += ThemeDerivation {
                        it.copy(
                            id = "nesting",
                            spacing = 1.rem,
                            derivations = mapOf(
                                ImportantSemantic to {
                                    it.copy(
                                        id = "important",
                                        foreground = it.background,
                                        background = it.foreground,
                                    ).withBack
                                }
                            )
                        ).withBack
                    }
                    child(draft) {}
                }

                atBottomEnd - padded - card - onlyWhen { draft.changesMade() } - row {
                    important - button {
                        spacing = 0.5.rem
                        centered - icon(Icon.close, "Cancel")
                        onClick { draft.cancel() }
                    }
                    important - button {
                        spacing = 0.5.rem
                        centered - text("Save Changes")
                        onClick { draft.publish() }
                    }
                }
            }
        }
    }
}