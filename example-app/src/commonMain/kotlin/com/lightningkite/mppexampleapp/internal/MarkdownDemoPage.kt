// by Claude
package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.markdown.markdown
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.extensions.debounce
import com.lightningkite.readable.Property

@Routable("markdown-demo")
object MarkdownDemoPage : Page {

    private val sampleMarkdown = """
# Markdown Demo

This is a **live editor** for testing the KiteUI markdown renderer.

## Features

- **Bold** and *italic* text
- ~~Strikethrough~~ text
- `Inline code`
- [Links](https://example.com)

### Lists

1. First ordered item
2. Second ordered item
3. Third ordered item

- Unordered item
- Another item
- Nested items work too

### Code Block

```kotlin
fun main() {
    println("Hello, KiteUI!")
}
```

### Blockquote

> This is a blockquote.
> It can span multiple lines.

### Table

| Feature | Status |
|---------|--------|
| Bold | ✓ |
| Italic | ✓ |
| Links | ✓ |
| Tables | ✓ |

### Image with Size

![KiteUI Logo|64](https://example.com/logo.png)

### Clickable Card

::: card href="https://example.com"
This entire card is a **clickable link**!

Click anywhere to navigate.
:::

### Custom Block

::: note
This is a custom block without a link.
:::

---

*Edit the text on the left to see changes here!*
    """.trimIndent()



    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val markdownSource = Property(sampleMarkdown)
        val debouncedSource = markdownSource.debounce(300)

        col {
            padded.h1("Markdown Editor Demo")

            expanding.rowCollapsingToColumn(60.rem) {
                // Left side: Editor
                expanding.card.col {
                    h3("Editor")
                    expanding.textArea {
                        content bind markdownSource
                    }
                }

                // Right side: Preview
                expanding.card.scrolling.col {
                    h3("Preview")
                    swapView {
                        swapping(current = { debouncedSource() }) { md ->
                            col {
                                markdown(md)
                            }
                        }
                    }
                }
            }
        }
    }
}
