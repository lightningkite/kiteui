package com.lightningkite.kiteui

import com.lightningkite.kiteui.markdown.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Robustness of the markdown parser against pathological input.
 *
 * Markdown is frequently rendered from user-supplied content, so a document that drives the
 * recursive block parser deep enough to exhaust the stack is a denial-of-service vector, not
 * just a crash.
 */
class MarkdownRobustnessTest {

    @Test
    fun deeplyNestedBlockquotesDoNotOverflowTheStack() {
        // Each ">" strips one level and recurses, so this would previously recurse 5000 deep.
        val hostile = ">".repeat(5000) + " boom"
        val nodes = MarkdownParser().parseBlocks(hostile)
        assertTrue(nodes.isNotEmpty(), "parser returned nothing for deeply nested input")
    }

    @Test
    fun deeplyNestedListItemsDoNotOverflowTheStack() {
        // Kept to ~90KB of source: indentation grows quadratically with nesting depth, so a
        // larger figure would exhaust memory on the input itself rather than on the parser,
        // which would make this test prove nothing about the parser.
        val hostile = (0 until 300).joinToString("\n") { "${"  ".repeat(it)}- item $it" }
        val nodes = MarkdownParser().parseBlocks(hostile)
        assertTrue(nodes.isNotEmpty(), "parser returned nothing for deeply nested list input")
    }

    @Test
    fun parserInstanceStaysUsableAfterHittingTheDepthLimit() {
        // The depth counter is instance state; if it leaked on the way out, a later parse on
        // the same instance would start pre-poisoned and truncate legitimate content.
        val parser = MarkdownParser()
        parser.parseBlocks(">".repeat(5000) + " boom")

        val normal = parser.parseBlocks("# Heading\n\nSome text.")
        assertTrue(normal.isNotEmpty(), "parser stopped producing output after a deep parse")

        val nested = parser.parseBlocks("> quoted\n>\n> > deeper")
        assertTrue(nested.isNotEmpty(), "nested content was dropped after a deep parse")
    }

    @Test
    fun ordinaryNestingIsStillParsedNormally() {
        // Guards against a depth limit set so low it truncates realistic documents.
        val doc = "> a\n> > b\n> > > c\n> > > > d"
        val nodes = MarkdownParser().parseBlocks(doc)
        assertTrue(nodes.isNotEmpty(), "ordinary nested blockquotes were dropped")
    }
}
