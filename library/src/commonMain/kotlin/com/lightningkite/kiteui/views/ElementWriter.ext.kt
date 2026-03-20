package com.lightningkite.kiteui.views


/**
 * Returns a writer that will apply the given setup to any elements added.
 *
 * Example: `centered.text("Hello")` applies alignment setup before adding the text view.
 */
fun ElementWriter.beforeSetup(setup: Element.() -> Unit): ElementWriter = ElementWriter.BeforeSetup(this, setup)
fun ElementWriter.CanAddScrolling.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddScrolling = object : ElementWriter.CanAddScrolling, ElementWriter by this {
    override fun willAddChild(element: Element) {
        this@beforeSetup.willAddChild(element)
        element.setup()
    }
}
fun ElementWriter.CanAddSizing.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddSizing = object : ElementWriter.CanAddSizing, ElementWriter by this {
    override fun willAddChild(element: Element) {
        this@beforeSetup.willAddChild(element)
        element.setup()
    }
}
fun ElementWriter.CanAddTheme.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddTheme = object : ElementWriter.CanAddTheme, ElementWriter by this {
    override fun willAddChild(element: Element) {
        this@beforeSetup.willAddChild(element)
        element.setup()
    }
}
fun ElementWriter.CanAddShownWhen.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddShownWhen = object : ElementWriter.CanAddShownWhen, ElementWriter by this {
    override fun willAddChild(element: Element) {
        this@beforeSetup.willAddChild(element)
        element.setup()
    }
}
fun ElementWriter.CanAddWeight.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddWeight = object : ElementWriter.CanAddWeight, ElementWriter by this {
    override fun willAddChild(element: Element) {
        this@beforeSetup.willAddChild(element)
        element.setup()
    }
}
fun ElementWriter.CanAddAlignment.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddAlignment = object : ElementWriter.CanAddAlignment, ElementWriter by this {
    override fun willAddChild(element: Element) {
        this@beforeSetup.willAddChild(element)
        element.setup()
    }
}
fun ViewWriter.beforeSetup(setup: Element.() -> Unit): ViewWriter = object : ViewWriter, ElementWriter by this {
    override fun willAddChild(element: Element) {
        this@beforeSetup.willAddChild(element)
        element.setup()
    }
}

/**
 * Creates a new writer with a split context.
 *
 * Context splitting allows you to shadow context addons (like themes, navigators, etc.)
 * without affecting the parent scope. Changes made to context addons in the split
 * context will only apply to children added through this writer.
 */
fun ElementWriter.split(): ElementWriter = ElementWriter.Split(this)
fun ElementWriter.CanAddScrolling.split(): ElementWriter.CanAddScrolling = object : ElementWriter.CanAddScrolling, ElementWriter by this {
    override val context: ElementContext = this@split.context.split()
}
fun ElementWriter.CanAddSizing.split(): ElementWriter.CanAddSizing = object : ElementWriter.CanAddSizing, ElementWriter by this {
    override val context: ElementContext = this@split.context.split()
}
fun ElementWriter.CanAddTheme.split(): ElementWriter.CanAddTheme = object : ElementWriter.CanAddTheme, ElementWriter by this {
    override val context: ElementContext = this@split.context.split()
}
fun ElementWriter.CanAddShownWhen.split(): ElementWriter.CanAddShownWhen = object : ElementWriter.CanAddShownWhen, ElementWriter by this {
    override val context: ElementContext = this@split.context.split()
}
fun ElementWriter.CanAddWeight.split(): ElementWriter.CanAddWeight = object : ElementWriter.CanAddWeight, ElementWriter by this {
    override val context: ElementContext = this@split.context.split()
}
fun ElementWriter.CanAddAlignment.split(): ElementWriter.CanAddAlignment = object : ElementWriter.CanAddAlignment, ElementWriter by this {
    override val context: ElementContext = this@split.context.split()
}
fun ViewWriter.split(): ViewWriter = object : ViewWriter, ElementWriter by this {
    override val context: ElementContext = this@split.context.split()
}

inline fun ElementWriter.produceAtMostOne(action: ElementWriter.() -> Unit): Element? {
    var output: Element? = null
    val writer = object : ElementWriter by this {
        override fun addChild(element: Element) {
            if(output != null) throw IllegalStateException("Produced more than one view at this layer, but only one was expected.")
            this@produceAtMostOne.addChild(element)
            output = element
        }
    }
    action(writer)
    return output
}

inline fun ElementWriter.produceExactlyOne(action: ElementWriter.() -> Unit): Element =
    produceAtMostOne(action) ?: throw IllegalStateException("Produced no views at this layer, but expected one.")