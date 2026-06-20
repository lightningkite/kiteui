package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.markdown.markdown
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.extensions.debounce
import com.lightningkite.readable.Property

@Routable("markdown-rich-text-editor-demo")
object MarkdownRichTextEditorDemoPage : Page {

    private val sampleMarkdown = """
# Markdown Rich Text Editor Demo

This component allows **rich text editing** while maintaining a *Markdown* backing.

## Try it out!

- Type some text here.
- Use the toolbar to format.
- See the raw Markdown preview below.

### Support
1. Headers
2. Lists
3. **Bold** / *Italic* / ~~Strikethrough~~
4. `Code` and `Code Blocks`
5. [Links](https://example.com)
    """.trimIndent()

    override val title: Reactive<String> = Constant("Markdown Rich Text Editor")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val markdownSource = Property(sampleMarkdown)
        val debouncedSource = markdownSource.debounce(300)

        scrolling.col {
            padded.h1("Markdown Rich Text Editor Demo")

            rowCollapsingToColumn(60.rem) {
                // Left side: Rich Editor
                card.col {
                    h3("Rich Editor")
                    markdownRichTextEditor {
                        hint = "Start typing markdown..."
                        content bind markdownSource
                    }
                }

                // Right side: Raw Markdown Preview
                card.col {
                    h3("Raw Markdown Output")
                    text {
                        ::content { markdownSource() }
                    }
                }
            }

            // Bottom: Rendered Preview (using the existing markdown component)
            expanding.card.col {
                h3("Rendered Preview (Read-only)")
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
