package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.core.*

@Routable("docs/recyclerView")
object RecyclerViewPage : DocPage {
    override val covers: List<String> = listOf("RecyclerView", "recyclerView", "horizontalRecyclerView", "list", "grid", "vertical", "horizontal")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            h1("RecyclerView")
            text("RecyclerView is a powerful component for displaying lists and grids of items efficiently. It reuses view elements as they scroll off-screen, making it ideal for displaying large datasets.")

            h2("Basic Usage")
            text("The most basic way to use a recyclerView is to create a vertical list of items:")
            example("""
                // Always use expanding or size constraints with recyclerView
                expanding.recyclerView {
                    children(
                        items = remember { (1..100).toList() },
                        id = { it } // Use the item itself as the ID
                    ) { value ->
                        card.text("Item")
                    }
                }
            """.trimIndent()) {
                expanding.recyclerView {
                    children(
                        items = remember { (1..20).toList() },
                        id = { it }
                    ) { _ ->
                        card.text("Item")
                    }
                }
            }

            h2("Horizontal RecyclerView")
            text("You can also create a horizontal recyclerView:")
            example("""
                // Horizontal recyclerView also needs size constraints
                expanding.horizontalRecyclerView {
                    children(
                        items = remember { (1..100).toList() },
                        id = { it }
                    ) { value ->
                        card.sizedBox(SizeConstraints(width = 10.rem)).centered.text("Item")
                    }
                }
            """.trimIndent()) {
                expanding.horizontalRecyclerView {
                    children(
                        items = remember { (1..20).toList() },
                        id = { it }
                    ) { _ ->
                        card.sizedBox(SizeConstraints(width = 10.rem)).centered.text("Item")
                    }
                }
            }

            h2("Grid Layout")
            text("RecyclerView can display items in a grid layout using placers:")
            example("""
                // Grid layout recyclerView also needs size constraints
                expanding.recyclerView {
                    placer = RecyclerViewPlacerVerticalGrid(2) // 2 columns
                    children(
                        items = remember { (1..100).toList() },
                        id = { it }
                    ) { value ->
                        card.text("Item")
                    }
                }
            """.trimIndent()) {
                expanding.recyclerView {
                    placer = RecyclerViewPlacerVerticalGrid(2)
                    children(
                        items = remember { (1..20).toList() },
                        id = { it }
                    ) { _ ->
                        card.text("Item")
                    }
                }
            }

            h2("Scrolling to a Specific Item")
            text("You can programmatically scroll to a specific item in the recyclerView:")
            example("""
                var recyclerView: RecyclerView? = null
                col {
                    button {
                        text("Scroll to Item 10")
                        onClick { recyclerView?.scrollToIndex(10, Align.Start, true) }
                    }
                    expanding.recyclerView {
                        recyclerView = this
                        children(
                            items = remember { (1..100).toList() },
                            id = { it }
                        ) { value ->
                            card.text("Item")
                        }
                    }
                }
            """.trimIndent()) {
                var recyclerView: RecyclerView? = null
                col {
                    button {
                        text("Scroll to Item 10")
                        onClick { recyclerView?.scrollToIndex(10, Align.Start, true) }
                    }
                    expanding.recyclerView {
                        recyclerView = this
                        children(
                            items = remember { (1..20).toList() },
                            id = { it }
                        ) { _ ->
                            card.text("Item")
                        }
                    }
                }
            }

            h2("Multiple Item Types")
            text("RecyclerView can display different types of items using childrenMultipleTypes:")
            example("""
                // Define item types outside the function
                sealed interface ListItem {
                    data class Text(val text: String) : ListItem
                    data class Image(val imageUrl: String) : ListItem
                }

                expanding.recyclerView {
                    childrenMultipleTypes(
                        items = remember { 
                            listOf(
                                ListItem.Text("Text Item 1"),
                                ListItem.Image("image1.jpg"),
                                ListItem.Text("Text Item 2")
                            ) 
                        },
                        id = { it.hashCode() }
                    ) {
                        type<ListItem.Text> { item ->
                            card.text { ::content { item().text } }
                        }
                        type<ListItem.Image> { item ->
                            card.image { source = ImageSource.Resource(item().imageUrl) }
                        }
                    }
                }
            """.trimIndent()) {
                // Simple example with a single type for demonstration
                expanding.recyclerView {
                    children(
                        items = remember { 
                            listOf(
                                "Text Item 1",
                                "Image Item",
                                "Text Item 2"
                            ) 
                        },
                        id = { it.hashCode() }
                    ) {
                        card.text { ::content { it() } }
                    }
                }
            }

            h2("Reactive Data")
            text("RecyclerView works well with reactive data sources:")
            example("""
                // Use a data class with a unique ID to prevent duplicate items
                data class ListItem(val id: Int, val text: String)

                val itemsList = Signal(listOf(
                    ListItem(1, "Item 1"),
                    ListItem(2, "Item 2"),
                    ListItem(3, "Item 3")
                ))
                var nextId = 4 // Track the next ID to use

                col {
                    button {
                        text("Add Item")
                        onClick { 
                            // Create a new item with a unique ID
                            val newItem = ListItem(nextId++, "New Item")
                            itemsList.value = itemsList.value + newItem
                        }
                    }
                    expanding.recyclerView {
                        children(
                            items = itemsList,
                            id = { it.id } // Use the unique ID field
                        ) { value ->
                            card.text { ::content { value().text } }
                        }
                    }
                }
            """.trimIndent()) {
                // Use a data class with a unique ID to prevent duplicate items
                data class ListItem(val id: Int, val text: String)

                val itemsList = Signal(listOf(
                    ListItem(1, "Item 1"),
                    ListItem(2, "Item 2"),
                    ListItem(3, "Item 3")
                ))
                var nextId = 4 // Track the next ID to use

                col {
                    button {
                        text("Add Item")
                        onClick { 
                            // Create a new item with a unique ID
                            val newItem = ListItem(nextId++, "New Item")
                            itemsList.value = itemsList.value + newItem
                        }
                    }
                    expanding.recyclerView {
                        children(
                            items = itemsList,
                            id = { it.id } // Use the unique ID field
                        ) { value ->
                            card.text { ::content { value().text } }
                        }
                    }
                }
            }

            h2("Sizing RecyclerView")
            text("RecyclerViews don't have a native size, so you must explicitly define their size using either a size constraint or the expanding modifier:")
            example("""
                col {
                    // Using expanding modifier
                    expanding.recyclerView {
                        children(
                            items = remember { (1..100).toList() },
                            id = { it }
                        ) { value ->
                            card.text("Item")
                        }
                    }

                    // Using size constraints
                    sizedBox(SizeConstraints(height = 20.rem)).recyclerView {
                        children(
                            items = remember { (1..100).toList() },
                            id = { it }
                        ) { value ->
                            card.text("Item")
                        }
                    }
                }
            """.trimIndent()) {
                col {
                    // Using expanding modifier
                    expanding.recyclerView {
                        children(
                            items = remember { (1..20).toList() },
                            id = { it }
                        ) { _ ->
                            card.text("Item")
                        }
                    }

                    // Using size constraints
                    sizedBox(SizeConstraints(height = 10.rem)).recyclerView {
                        children(
                            items = remember { (1..20).toList() },
                            id = { it }
                        ) { _ ->
                            card.text("Item")
                        }
                    }
                }
            }

            h2("Best Practices")
            text("When using RecyclerView, keep these best practices in mind:")
            col {
                gap = 0.5.rem
                text("1. Always provide a unique and stable ID for each item to ensure proper recycling.")
                text("2. Keep your item views lightweight to ensure smooth scrolling.")
                text("3. Use the appropriate placer for your layout needs (vertical, horizontal, grid).")
                text("4. Consider using pagination for very large datasets.")
                text("5. Avoid nesting recyclerViews when possible, as it can lead to performance issues.")
                text("6. Always specify a size for your recyclerView using either the expanding modifier or size constraints.")
            }
        }
    }
}
