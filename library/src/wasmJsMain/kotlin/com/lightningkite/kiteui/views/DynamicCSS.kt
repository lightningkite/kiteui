package com.lightningkite.kiteui.views

import org.w3c.dom.HTMLElement
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PlatformNavigator
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.css.get
import org.w3c.dom.events.Event
import kotlin.time.Duration

object DynamicCSS {
    val customStyleSheet: CSSStyleSheet by lazy {
        val sheet = document.createElement("style") as HTMLStyleElement
        sheet.title = "generated-css"
        document.head!!.appendChild(sheet)
        document.styleSheets.let {
            for (i in 0 until it.length) {
                val copy = it.get(i)!!
                if (copy.title == sheet.title) return@let copy as CSSStyleSheet
            }
            throw IllegalStateException()
        }
    }

    init {
        // basis rules
        style(":root", mapOf(
            "--usePadding" to "0",
        ))
        style("*", mapOf(
            "box-sizing" to "border-box",
            "line-height" to "unset",
            "overflow-x" to "visible",
        ))
        style("h1", mapOf("font-size" to "2rem"))
        style("h2", mapOf("font-size" to "1.6rem"))
        style("h3", mapOf("font-size" to "1.4rem"))
        style("h4", mapOf("font-size" to "1.3rem"))
        style("h5", mapOf("font-size" to "1.2rem"))
        style("h6", mapOf("font-size" to "1.1rem"))
        style(".subtext", mapOf("font-size" to "0.8rem"))
//        style.visibility = if (value) "visible" else "hidden"
        style(
            ".visibleOnParentHover",
            mapOf(
                "visibility" to "hidden",
                "width" to "max-content",
                "height" to "max-content",
                "max-width" to "unset",
                "max-height" to "unset",
            )
        )
        style(":hover>.visibleOnParentHover", mapOf("visibility" to "visible"))
        style(":hover.visibleOnParentHover", mapOf("visibility" to "visible"))

        style(".swapImage", mapOf(
            "display" to "grid",
            "grid-template-columns" to "100%",
            "grid-template-rows" to "100%",
            "overflow" to "hidden",
        ))
        style(".swapImage > *", mapOf(
            "grid-column-start" to "1",
            "grid-column-end" to "1",
            "grid-row-start" to "1",
            "grid-row-end" to "1",
            "align-self" to "stretch",
            "justify-self" to "stretch",
            "object-fit" to "contain",
        ))
        style(".swapImage.scaleType-Fit > img", mapOf("object-fit" to "contain"))
        style(".swapImage.scaleType-Crop > img", mapOf("object-fit" to "cover"))
        style(".swapImage.scaleType-Stretch > img", mapOf("object-fit" to "fill"))
        style(".swapImage.scaleType-NoScale > img", mapOf("object-fit" to "none"))

        style(".noInteraction.noInteraction", mapOf(
            "pointer-events" to "none"
        ))
        style(".noInteraction > *", mapOf(
            "pointer-events" to "auto"
        ))

        style(
            "body", mapOf(
                "height" to "100svh",
                "max-height" to "100svh",
                "max-width" to "100vw",
                "overflow" to "hidden"
            )
        )

        style(
            "body > div", mapOf(
                "height" to "100%",
                "max-width" to "100vw",
            )
        )

        style(
            "a", mapOf(
                "text-decoration" to "none",
                "color" to "unset",
            )
        )

        style(
            "a:visited", mapOf(
                "color" to "unset",
            )
        )

        style(
            "button, input, textarea, select", mapOf(
                "background" to "none",
                "border-width" to "0",
                "outline-width" to "0",
                "font" to "unset",
                "color" to "unset",
                "text-align" to "start",
            )
        )

//        style(
//            "input.sameThemeText, textarea.sameThemeText", mapOf(
//                "border-bottom-style" to "solid",
//                "border-bottom-width" to "1px",
//                "border-radius" to "0",
//            )
//        )
//
//        style(
//            "input.sameThemeText:focus, textarea.sameThemeText:focus", mapOf(
//                "border-radius" to "0",
//            )
//        )
//
//        style(
//            "input:not(.mightTransition).editable.editable, textarea:not(.mightTransition).editable.editable, select:not(.mightTransition).editable.editable", mapOf(
//                "border-bottom-color" to "currentColor",
//                "border-bottom-width" to "1px",
//                "border-bottom-style" to "solid",
//            )
//        )
//
//        style(
//            "input:not(.mightTransition).editable.editable:focus, textarea:not(.mightTransition).editable.editable:focus, select:not(.mightTransition).editable.editable:focus", mapOf(
//                "border-bottom-color" to "currentColor",
//                "border-bottom-width" to "2px",
//                "border-bottom-style" to "solid",
//                "outline" to "none",
//            )
//        )

        style(
            "input:focus textarea:focus", mapOf(
                "outline" to "inherit",
            )
        )

        style(
            ".toggle-button", mapOf(
                "display" to "flex",
                "align-items" to "stretch",
            )
        )

        style(
            ".toggle-button > span", mapOf(
                "flex-grow" to "1",
                "flex-shrink" to "1",
            )
        )

        style(
            ".spinner", mapOf(
                "width" to "32px !important",
                "height" to "32px !important",
                "opacity" to "0.5 !important",
                "background" to "none !important",
                "box-shadow" to "none !important",
                "border-style" to "solid !important",
                "border-color" to "currentColor currentColor currentColor transparent !important",
                "border-width" to "5px !important",
                "border-radius" to "50% !important",
                "animation" to "spin 2s infinite linear !important",
            )
        )
        style(".kiteui-swap", mapOf(
            "display" to "grid",
            "grid-template-columns" to "100%",
            "grid-template-rows" to "100%",
        ))
        style(".kiteui-swap > *", mapOf(
            "grid-column-start" to "1",
            "grid-column-end" to "1",
            "grid-row-start" to "1",
            "grid-row-end" to "1",
            "align-self" to "stretch",
            "justify-self" to "stretch",
        ))


//        style(
//            ".kiteui-swap", mapOf(
//                "display" to "block",
//                "position" to "relative",
//            )
//        )
//
//        style(
//            ".kiteui-swap > *", mapOf(
//                "position" to "absolute",
//                "top" to "0",
//                "left" to "0",
//                "right" to "0",
//                "bottom" to "0",
//                "max-width" to "100%",
//                "max-height" to "100%",
//            )
//        )

        style(".hidingContainer", mapOf(
            "display" to "grid",
            "grid-template-columns" to "100%",
            "grid-template-rows" to "100%",
        ))
        style(".hidingContainer > *", mapOf(
            "grid-column-start" to "1",
            "grid-column-end" to "1",
            "grid-row-start" to "1",
            "grid-row-end" to "1",
            "align-self" to "stretch",
            "justify-self" to "stretch",
        ))

        style(
            ".kiteui-stack", mapOf(
                "display" to "grid",
                "grid-template-columns" to "100%",
                "grid-template-rows" to "100%",
            )
        )

        style(
            ".kiteui-stack > *", mapOf(
                "grid-column-start" to "1",
                "grid-column-end" to "1",
                "grid-row-start" to "1",
                "grid-row-end" to "1",
                "align-self" to "stretch",
                "justify-self" to "stretch",
            )
        )

        style(
            ".kiteui-stack > .hStart", mapOf(
                "justify-self" to "start",
            )
        )

        style(
            ".kiteui-stack > .hCenter", mapOf(
                "justify-self" to "center",
            )
        )

        style(
            ".kiteui-stack > .hStretch", mapOf(
                "justify-self" to "stretch",
            )
        )

        style(
            ".kiteui-stack > .hEnd", mapOf(
                "justify-self" to "end",
            )
        )

        style(
            ".kiteui-stack > .vStart", mapOf(
                "align-self" to "start",
            )
        )

        style(
            ".kiteui-stack > .vCenter", mapOf(
                "align-self" to "center",
            )
        )

        style(
            ".kiteui-stack > .vStretch", mapOf(
                "align-self" to "stretch",
            )
        )

        style(
            ".kiteui-stack > .vEnd", mapOf(
                "align-self" to "end",
            )
        )

        style(
            ".kiteui-row", mapOf(
                "display" to "flex",
                "flex-direction" to "row",
            )
        )
//        style(
//            ".kiteui-row > *", mapOf(
//                "max-width" to "unset",
//            )
//        )

        style(
            ".kiteui-row > .vStart", mapOf(
                "align-self" to "start",
            )
        )

        style(
            ".kiteui-row > .vCenter", mapOf(
                "align-self" to "center",
            )
        )

        style(
            ".kiteui-row > .vStretch", mapOf(
                "align-self" to "stretch",
            )
        )

        style(
            ".kiteui-row > .vEnd", mapOf(
                "align-self" to "end",
            )
        )

        style(
            ".kiteui-col", mapOf(
                "display" to "flex",
                "flex-direction" to "column",
            )
        )

//        style(
//            ".kiteui-col > *", mapOf(
//                "max-height" to "unset",
//            )
//        )

        style(
            ".kiteui-col > .hStart", mapOf(
                "align-self" to "start",
            )
        )

        style(
            ".kiteui-col > .hCenter", mapOf(
                "align-self" to "center",
            )
        )

        style(
            ".kiteui-col > .hStretch", mapOf(
                "align-self" to "stretch",
            )
        )

        style(
            ".kiteui-col > .hEnd", mapOf(
                "align-self" to "end",
            )
        )

        style(
            "img", mapOf(
                "overflow" to "hidden",
            )
        )

        style(
            "p.loading:not(.inclBack), h1.loading:not(.inclBack), h2.loading:not(.inclBack), h3.loading:not(.inclBack), h4.loading:not(.inclBack), h5.loading:not(.inclBack), h6.loading:not(.inclBack), img.loading:not(.inclBack), input.loading:not(.inclBack), select.loading:not(.inclBack), textarea.loading:not(.inclBack)",
            mapOf(
                "min-height" to "1em",
                "background" to "color-mix(in srgb, currentColor, transparent 70%) !important",
                "animation" to "flickerAnimation 2s infinite !important",
                "animation-timing-function" to "linear",
            )
        )

        style(
            "button.loading:after", mapOf(
                "opacity" to "0.5 !important",
                "content" to "\"\"",
                "pointer-events" to "none",
                "position" to "absolute",
                "top" to "calc(50% - 15px)",
                "left" to "calc(50% - 15px)",
                "width" to "20px !important",
                "height" to "20px !important",
                "background" to "none !important",
                "box-shadow" to "none !important",
                "border-style" to "solid !important",
                "border-color" to "currentColor currentColor currentColor transparent !important",
                "border-width" to "6px !important",
                "border-radius" to "50% !important",
                "transition" to "all .3s ease",
                "animation" to "spin 2s infinite linear !important",
            )
        )
        style("button", mapOf("position" to "relative"))
        style(
            "button.loading > *", mapOf(
                "opacity" to "0.15",
            )
        )

        style(
            ".clickable", mapOf(
                "cursor" to "pointer",
            )
        )

        style(
            ".switch", mapOf(
                "position" to "relative",
                "overflow" to "visible",
                "padding" to "0 !important",
                "height" to "1.5rem",
                "width" to "3rem",
                "cursor" to "pointer",
                "appearance" to "none",
                "-webkit-appearance" to "none",
                "border-radius" to "9999px !important",
                "background-color" to "rgba(100, 116, 139, 0.377)",
                "transition" to "all .3s ease",
            )
        )

        style(
            ".switch:checked", mapOf(
                "background-color" to "rgba(236, 72, 153, 1)",
            )
        )

        style(
            ".switch::before", mapOf(
                "position" to "absolute",
                "content" to "\"\"",
                "left" to "calc(1.5rem - 1.6rem)",
                "top" to "calc(1.5rem - 1.6rem)",
                "display" to "block",
                "height" to "1.6rem",
                "width" to "1.6rem",
                "max-width" to "unset",
                "max-height" to "unset",
                "cursor" to "pointer",
                "border" to "1px solid rgba(100, 116, 139, 0.527)",
                "border-radius" to "9999px !important",
                "background-color" to "rgba(255, 255, 255, 1)",
                "box-shadow" to "0 3px 10px rgba(100, 116, 139, 0.327)",
                "transition" to "all .3s ease",
            )
        )

        style(
            ".switch:hover::before", mapOf(
                "box-shadow" to "0 0 0px 8px rgba(0, 0, 0, .15)"
            )
        )

        style(
            ".switch:checked:hover::before", mapOf(
                "box-shadow" to "0 0 0px 8px rgba(236, 72, 153, .15)"
            )
        )

        style(
            ".switch:checked:before", mapOf(
                "left" to "calc(3rem - 1.6rem)",
                "border-color" to "rgba(236, 72, 153, 1)",
            )
        )

        style(
            ".checkbox", mapOf(
                "appearance" to "none",
                "width" to "1.5rem",
                "height" to "1.5rem",
                "position" to "relative",
                "padding" to "0px !important",
//                "border-width" to "0.25rem",
//                "border-style" to "solid",
            )
        )
        style(
            ".checkbox::after", mapOf(
                "position" to "absolute",
                "content" to "\"\"",
                "display" to "block",
                "width" to "0.8rem",
                "height" to "0.3rem",
                "top" to "0.4rem",
                "left" to "0.25rem",
                "border-left-color" to "currentColor",
                "border-bottom-color" to "currentColor",
                "border-left-style" to "solid",
                "border-bottom-style" to "solid",
                "border-left-width" to "0.2rem",
                "border-bottom-width" to "0.2rem",
                "opacity" to "0.4",
                "transform" to "rotate(-45deg) scale(0,0)",
                "transition-property" to "opacity, transform",
                "transition-timing-function" to "linear",
            )
        )
        style(
            ":checked.checkbox::after", mapOf(
                "opacity" to "1",
                "transform" to "rotate(-45deg)"
            )
        )
        style(
            ".radio", mapOf(
                "appearance" to "none",
                "width" to "1.5rem",
                "height" to "1.5rem",
                "position" to "relative",
                "border-radius" to "999px !important",
                "padding" to "0px !important",
                "border-width" to "0.1rem",
                "border-style" to "solid",
            )
        )
        style(
            ".radio::after", mapOf(
                "position" to "absolute",
                "border-radius" to "999px",
                "content" to "\"\"",
                "display" to "block",
                "width" to "1rem",
                "height" to "1rem",
                "top" to "0.15rem",
                "left" to "0.15rem",
                "background-color" to "currentColor",
                "opacity" to "0.4",
                "transform" to "scale(0,0)",
                "transition-property" to "opacity, transform",
                "transition-timing-function" to "linear",
            )
        )
        style(
            ":checked.radio::after", mapOf(
                "opacity" to "1",
                "transform" to "none"
            )
        )

        style(
            ".crowd", mapOf(
                "padding" to "0 !important",
            )
        )

        style(
            ".kiteui-label.kiteui-label", mapOf(
                "display" to "flex",
                "flex-direction" to "column",
                "align-items" to "stretch",
            )
        )

        style(
            "*", mapOf(
                "scrollbar-color" to "#999 #0000",
                "scrollbar-width" to "thin",
                "scrollbar-gutter" to "auto",
                "flex-shrink" to "0",
                "max-width" to "calc(100%)",
                "max-height" to "calc(100%)",
                "min-height" to "0",
                "min-width" to "0",
                "padding" to "0",
            )
        )

        style(
            "input", mapOf(
                "min-height" to "1.5rem",
                "min-width" to "1.5rem",
            )
        )

        style(
            "::placeholder", mapOf(
                "color" to "currentColor",
                "opacity" to "0.3",
            )
        )

        style(
            ".kiteui-separator", mapOf(
                "background-color" to "currentColor",
                "opacity" to "0.25",
                "min-width" to "1px",
                "min-height" to "1px",
            )
        )

        style(
            "iframe#webpack-dev-server-client-overlay", mapOf(
                "display" to "none !important"
            )
        )

        style(
            ".icon", mapOf(
                "display" to "grid",
                "grid-template-columns" to "100%",
                "grid-template-rows" to "100%",
            )
        )

        style(
            ".icon > *", mapOf(
                "grid-column-start" to "1",
                "grid-column-end" to "1",
                "grid-row-start" to "1",
                "grid-row-end" to "1",
                "align-self" to "stretch",
                "justify-self" to "stretch",
            )
        )

        style(
            ".recycler", mapOf(
                "overflow-y" to "auto"
            )
        )

        style(
            ".recycler > *", mapOf(
                "max-height" to "unset",
            )
        )

        style(
            ".recycler-horz", mapOf(
                "display" to "flex",
                "flex-direction" to "row",
                "overflow-x" to "auto",
            )
        )

        style(
            ".recycler-horz > *", mapOf(
                "max-width" to "unset",
            )
        )

        style(
            ".recycler-grid", mapOf(
                "display" to "flex",
                "flex-direction" to "row",
                "overflow-x" to "auto",
            )
        )

        style(
            ".scroll-vertical > *", mapOf(
                "max-height" to "unset",
            )
        )

        style(
            ".scroll-vertical", mapOf(
                "overflow-y" to "auto",
                "overflow-x" to "hidden",
            )
        )

        style(
            ".scroll-horizontal > *", mapOf(
                "max-width" to "unset",
            )
        )

        style(
            ".scroll-horizontal", mapOf(
                "overflow-x" to "auto",
                "overflow-y" to "hidden",
            )
        )

        style(
            "::-webkit-scrollbar", mapOf(
                "background" to "#0000",
            )
        )

        style(
            "::-webkit-scrollbar-thumb", mapOf(
                "background" to "color-mix(in srgb, currentColor 20%, transparent)",
                "-webkit-border-radius" to "4px",
            )
        )

        style(
            "::-webkit-scrollbar-corner", mapOf(
                "background" to "#0000"
            )
        )

        rule(
            """
            @keyframes flickerAnimation {
                0% {
                    opacity: 1;
                }
                50% {
                    opacity: 0.5;
                }
                100% {
                    opacity: 1;
                }
            }
        """.trimIndent()
        )
        rule(
            """
            @keyframes spin {
                from {
                    transform: rotate(0deg);
                }
                to {
                    transform: rotate(360deg);
                }
            }
        """.trimIndent()
        )

//        style(
//            ".kiteui-row > [hidden]", mapOf(
//                "width" to "0px !important",
//                "transform" to "scaleX(0)",
//            )
//        )
//        style(
//            ".kiteui-col > [hidden]", mapOf(
//                "height" to "0px !important",
//                "transform" to "scaleY(0)",
//            )
//        )
//        style(
//            ".kiteui-stack > [hidden]", mapOf(
////                "width" to "0px !important",
////                "height" to "0px !important",
//                "transform" to "scale(0, 0)",
//            )
//        )
        style(
            "[hidden]", mapOf(
                "display" to "none !important",
            )
        )

        style(
            ":not(.unkiteui)", mapOf(
                "transition-duration" to "0.15s",
                "transition-timing-function" to "linear",
                "transition-delay" to "0s",
                "transition-property" to "color, background-image, background-color, outline-color, box-shadow, border-radius, opacity, backdrop-filter",
            )
        )

        style(
            ":not(.unkiteui).animatingShowHide", mapOf(
                "overflow" to "hidden",
                "minWidth" to "0px",
                "minHeight" to "0px",
            )
        )

        style(
            ".notransition, .notransition *", mapOf(
                "transition" to "none !important"
            )
        )

        style(
            ".dismissBackground", mapOf(
                "z-index" to "998",
                "pointer-events" to "auto"
            )
        )
        style(
            ".dismissBackground + *", mapOf(
                "z-index" to "999",
            )
        )
//        recyclerView
        style(".recyclerView", mapOf(
            "position" to "relative",
            "padding" to "0 !important"
        ))

        style(".contentScroll-V::-webkit-scrollbar", mapOf(
            "display" to "none"
        ))
        style(".contentScroll-H::-webkit-scrollbar", mapOf(
            "display" to "none"
        ))
        style(".contentScroll-V",  mapOf(
            "width" to "100%",
            "height" to "100%",
            "position" to "relative",
            "overflow-y" to "scroll",
            "overflow-anchor" to "none",
            "scrollbar-width" to "none",
        ))
        style(".contentScroll-H",  mapOf(
            "width" to "100%",
            "height" to "100%",
            "position" to "relative",
            "overflow-x" to "scroll",
            "overflow-anchor" to "none",
            "scrollbar-width" to "none",
        ))
        style(".contentScroll-V > *",  mapOf(
            "position" to "absolute",
            "max-height" to "unset",
            "width" to "calc(100% - var(--parentSpacing, 0px) * var(--usePadding, 0) * 2)",
            "margin-left" to "calc(var(--parentSpacing, 0px) * var(--usePadding, 0))",
            "margin-right" to "calc(var(--parentSpacing, 0px) * var(--usePadding, 0))",
            "overflow-anchor" to "revert",
        ))
        style(".contentScroll-H > *",  mapOf(
            "max-width" to "unset",
            "position" to "absolute",
            "height" to "calc(100% - var(--parentSpacing, 0px) * var(--usePadding, 0) * 2)",
            "margin-top" to "calc(var(--parentSpacing, 0px) * var(--usePadding, 0))",
            "margin-bottom" to "calc(var(--parentSpacing, 0px) * var(--usePadding, 0))",
            "overflow-anchor" to "revert",
        ))
        style(".contentScroll-V > .recyclerViewGridSub",  mapOf(
            "display" to "flex",
            "flex-direction" to "row",
            "gap" to "var(--spacing, 0)",
        ))
        style(".contentScroll-H > .recyclerViewGridSub",  mapOf(
            "display" to "flex",
            "flex-direction" to "column",
            "gap" to "var(--spacing, 0)",
        ))
        style(".recyclerViewGridSub > *",  mapOf(
            "flex-grow" to "1",
            "flex-shrink" to "1",
            "flex-basis" to "0",
        ))
//        contentScroll
//        content
//        barScroll
//        barContent
//        style(".viewPager", mapOf(
//            "scroll-snap-type" to "x mandatory",
//            "scroll-behavior" to "smooth"
//        ))
        style(".viewPager > *", mapOf(
            "width" to "var(--pager-width, 0rem)",
            "height" to "var(--pager-height, 0rem)",
            "scroll-snap-align" to "center",
        ))
        style(".touchscreenOnly", mapOf(
            "visibility" to "gone"
        ))
        rule("""
            @media (pointer: coarse) and (hover: none) {
                .touchscreenOnly {
                    visibility: visible
                }
            }
        """.trimIndent())

        style(
            "progress", mapOf(
                "height" to "0.5rem",
                "background-color" to "green",
            )
        )
        style(
            "progress::-webkit-progress-bar", mapOf(
                "height" to "0.5rem",
                "background-color" to "blue",
            )
        )
        style(
            "progress::-webkit-progress-value", mapOf(
                "height" to "0.5rem",
                "background-color" to "red",
            )
        )
    }

    fun rule(rule: String, index: Int = 0): Int {
        return customStyleSheet.insertRule(rule, index)
    }

    private val transitionHandled = HashSet<String>()
    fun transition(transition: ScreenTransition): String {
        if (!transitionHandled.add(transition.name)) return "transition-${transition.name}"
        fun StringBuilder.extracted(part: ScreenTransitionPart) {
            for ((key, value) in part.from) append("$key: $value; ")
            append("} to { ")
            for ((key, value) in part.to) append("$key: $value; ")
            append("}")
        }

        rule(buildString {
            append("@keyframes transition-${transition.name}-enter { from { ")
            extracted(transition.enter)
        }, 0)
        rule(buildString {
            append("@keyframes transition-${transition.name}-exit { from { ")
            extracted(transition.exit)
        }, 0)
        return "transition-${transition.name}"
    }

    private val fontHandled = HashSet<String>()
    fun font(font: Font): String {
        if (!fontHandled.add(font.cssFontFamilyName)) return font.cssFontFamilyName
        if (font.url != null) {
            document.head!!.appendChild((document.createElement("link") as HTMLLinkElement).apply {
                rel = "stylesheet"
                type = "text/css"
                href = font.url
            })
        }
        if (font.direct != null) {
            font.direct.normal?.let {
                rule("@font-face {font-family: '${font.cssFontFamilyName}';font-style: normal;font-weight: normal;src:url('${PlatformNavigator.basePath + it}');}")
            }
            font.direct.bold?.let {
                rule("@font-face {font-family: '${font.cssFontFamilyName}';font-style: normal;font-weight: bold;src:url('${PlatformNavigator.basePath + it}');}")
            }
            font.direct.italic?.let {
                rule("@font-face {font-family: '${font.cssFontFamilyName}';font-style: italic;font-weight: normal;src:url('${PlatformNavigator.basePath + it}');}")
            }
            font.direct.boldItalic?.let {
                rule("@font-face {font-family: '${font.cssFontFamilyName}';font-style: italic;font-weight: bold;src:url('${PlatformNavigator.basePath + it}');}")
            }
        }
        return font.cssFontFamilyName
    }

    private val styleOnces = HashSet<String>()
    fun styleIfMissing(selector: String, map: Map<String, String>) {
        if(styleOnces.add(selector)) {
            val wrapSelector = selector//":not(.unkiteui) $selector"
            rule(
                """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }""",
                0
            )
        }
    }

    fun style(selector: String, map: Map<String, String>) {
        val wrapSelector = selector//":not(.unkiteui) $selector"
        rule(
            """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }""",
            0
        )
    }

    fun tempStyle(selector: String, map: Map<String, String>): () -> Unit {
        val wrapSelector = selector//":not(.unkiteui) $selector"
        val content = """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }"""
        rule(
            content,
            0
        )
        val rule = customStyleSheet.cssRules.get(0)
        return {
            customStyleSheet.cssRules.let {
                (
                        0..<it.length).find { index -> it.get(index) === rule }
            }?.let {
                customStyleSheet.deleteRule(it)
            }
        }
    }

    private fun Dimension.toBoxShadow(): String {
        if (value == "0px")
            return "none"
        val offsetX = 0.px.value
        val offsetY = value
        val blur = (this * 2).value
        val spread = 0.px.value
        return "$offsetX $offsetY $blur $spread #77777799"
    }

    private fun Paint.toCss() = when (this) {
        is Color -> this.toWeb()
        is LinearGradient -> "linear-gradient(${angle.plus(Angle.quarterTurn).turns}turn, ${joinGradientStops(stops)})"
        is RadialGradient -> "radial-gradient(circle at center, ${joinGradientStops(stops)})"
    }

    private fun joinGradientStops(stops: List<GradientStop>): String {
        return stops.joinToString {
            "${it.color.toWeb()} ${it.ratio * 100}%"
        }
    }


    fun themeInteractive(theme: Theme): String {
        theme(
            theme.down(),
            listOf(".clickable:active .theme-${theme.id}", ".clickable:active.theme-${theme.id}"),
            includeMaybeTransition = true
        )
        theme(
            theme.hover(),
            listOf(".clickable:hover .theme-${theme.id}", ".clickable:hover.theme-${theme.id}"),
            includeMaybeTransition = true
        )
        theme(
            theme.focus(),
            listOf(".clickable:focus-visible .theme-${theme.id}", ".clickable:focus-visible.theme-${theme.id}"),
            includeMaybeTransition = true
        )
        theme(
            theme.disabled(),
            listOf(".clickable:disabled:disabled .theme-${theme.id}", ".clickable:disabled:disabled.theme-${theme.id}"),
            includeMaybeTransition = false
        )

        theme(
            theme.selected(),
            listOf(
                "input:checked.checkResponsive .theme-${theme.id}",
                "input:checked.checkResponsive.theme-${theme.id}",
                "input:checked+.checkResponsive .theme-${theme.id}",
                "input:checked+.checkResponsive.theme-${theme.id}",
            ),
            includeMaybeTransition = true
        )
        theme(
            theme.selected().hover(),
            listOf(
                "input:checked.checkResponsive:hover .theme-${theme.id}",
                "input:checked.checkResponsive:hover.theme-${theme.id}",
                "input:checked+.checkResponsive:hover .theme-${theme.id}",
                "input:checked+.checkResponsive:hover.theme-${theme.id}",
            ),
            includeMaybeTransition = true
        )
        theme(
            theme.selected().focus(),
            listOf(
                "input:checked.checkResponsive:focus-visible .theme-${theme.id}",
                "input:checked.checkResponsive:focus-visible.theme-${theme.id}",
                "input:checked+.checkResponsive:focus-visible .theme-${theme.id}",
                "input:checked+.checkResponsive:focus-visible.theme-${theme.id}",
            ),
            includeMaybeTransition = true
        )
        theme(
            theme.selected().disabled(),
            listOf(
                "input:checked.checkResponsive:disabled .theme-${theme.id}",
                "input:checked.checkResponsive:disabled.theme-${theme.id}",
                "input:checked+.checkResponsive:disabled .theme-${theme.id}",
                "input:checked+.checkResponsive:disabled.theme-${theme.id}",
            ),
            includeMaybeTransition = true
        )

        return theme(theme)
    }

    private val themeHandled = HashSet<String>()
    fun theme(
        theme: Theme,
        asSelectors: List<String> = listOf(".theme-${theme.id}"),
        includeMaybeTransition: Boolean = false
    ): String {
        val includeSelectors = asSelectors.filter { themeHandled.add(it) }
        if (includeSelectors.isEmpty()) return "theme-${theme.id}"
        fun sel(vararg plus: String): String {
            return includeSelectors.asSequence().flatMap { plus.asSequence().map { p -> "$it$p" } }.joinToString(", ")
        }
        style(
            sel(".mightTransition:not(.isRoot):not(.swapImage)", ".forcePadding:not(.cancelForcePadding):not(.swapImage)"), mapOf(
                "padding" to "var(--spacing, 0px)",
                "--usePadding" to "1",
            )
        )
        style(
            sel(".mightTransition:not(.isRoot):not(.swapImage) > *", ".forcePadding:not(.cancelForcePadding):not(.swapImage) > *"), mapOf(
                "--parentSpacing" to theme.spacing.value,
            )
        )
        style(
            if (includeMaybeTransition) sel(".mightTransition") else sel(".transition"),
            when (val it = theme.background) {
                is Color -> mapOf("background-color" to it.toCss())
                is LinearGradient -> mapOf(
                    "background-image" to "linear-gradient(${it.angle.plus(Angle.quarterTurn).turns}turn, ${joinGradientStops(it.stops)})",
                    "background-attachment" to (if (it.screenStatic) "fixed" else "unset"),
                )

                is RadialGradient -> mapOf(
                    "background-image" to "radial-gradient(circle at center, ${joinGradientStops(it.stops)})",
                    "background-attachment" to (if (it.screenStatic) "fixed" else "unset"),
                )
            }
        )
        style(
            if (includeMaybeTransition) sel(".mightTransition") else sel(".transition"),
            mapOf(
                "outline-width" to theme.outlineWidth.value,
                "box-shadow" to theme.elevation.toBoxShadow(),
                "outline-style" to if (theme.outlineWidth != 0.px) "solid" else "none",
            )
        )
        style(
            sel(".mightTransition", ".swapImage"), mapOf(
                "border-radius" to when(val it = theme.cornerRadii) {
                    is CornerRadii.Constant -> "calc(min(var(--parentSpacing, 0px), ${it.value.value}))"
                    is CornerRadii.ForceConstant -> it.value.value
                    is CornerRadii.RatioOfSize -> "${it.ratio.times(100).toInt()}%"
                    is CornerRadii.RatioOfSpacing -> "calc(var(--parentSpacing, 0px) * ${it.value})"
                },
            )
        )
        style(
            sel(".title"), mapOf(
                "font-family" to font(theme.title.font),
                "font-weight" to if (theme.title.bold) "bold" else "normal",
                "font-style" to if (theme.title.italic) "italic" else "normal",
                "text-transform" to if (theme.title.allCaps) "uppercase" else "none",
                "line-height" to theme.title.lineSpacingMultiplier.toString(),
                "letter-spacing" to theme.title.additionalLetterSpacing.toString(),
            )
        )
        style(
            sel(".icon"), mapOf(
                "color" to theme.icon.toCss()
            )
        )
        style(
            sel(""), mapOf(
                "color" to theme.foreground.toCss(),
                "--spacing" to theme.spacing.value,
                "gap" to "var(--spacing, 0.0)",
                "font-family" to font(theme.body.font),
                "font-weight" to if (theme.body.bold) "bold" else "normal",
                "font-style" to if (theme.body.italic) "italic" else "normal",
                "text-transform" to if (theme.body.allCaps) "uppercase" else "none",
                "line-height" to theme.body.lineSpacingMultiplier.toString(),
                "letter-spacing" to theme.body.additionalLetterSpacing.toString(),
                "outline-color" to theme.outline.toCss(),
            )
        )
        style(
            sel(".dismissBackground"), mapOf(
                "border-radius" to "0",
                "outline-width" to "0",
                "backdrop-filter" to "blur(5px)",
            ) + when (val it = theme.background.applyAlpha(0.5f)) {
                is Color -> mapOf("background-color" to it.toCss())
                is LinearGradient -> mapOf(
                    "background-image" to "linear-gradient(${it.angle.plus(Angle.quarterTurn).turns}turn, ${joinGradientStops(it.stops)})",
                    "background-attachment" to (if (it.screenStatic) "fixed" else "unset"),
                )

                is RadialGradient -> mapOf(
                    "background-image" to "radial-gradient(circle at center, ${joinGradientStops(it.stops)})",
                    "background-attachment" to (if (it.screenStatic) "fixed" else "unset"),
                )
            }
        )

        return "theme-${theme.id}"
    }
}

private fun animate(element: HTMLElement, keyframes: JsArray<JsAny>, options: JsAny): Animation = js("element.animate(keyframes, options)")
fun HTMLElement.animate(keyframes: JsArray<JsAny>, options: JsAny): Animation = animate(this, keyframes, options)
//    this.asDynamic().animate(keyframes, options) as Animation

private fun getAnimations(element: HTMLElement): JsArray<Animation> = js("element.getAnimations()")
external interface Animation: JsAny {
    var oncancel: ((Event) -> Unit)?
    var onfinish: ((Event) -> Unit)?
    var onremove: ((Event) -> Unit)?
    fun cancel()
    fun commitStyles()
    fun finish()
    fun pause()
    fun play()
    fun reverse()
    var currentTime: Double
    var startTime: Double
}