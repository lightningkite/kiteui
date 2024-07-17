package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

class KiteUiCss(val dynamicCss: DynamicCss) {
    init {
        // basis rules
        dynamicCss.style(
            "*", mapOf(
                "box-sizing" to "border-box",
                "line-height" to "unset",
                "--parentPadding" to "0px",
            )
        )
        dynamicCss.style("h1, h2, h3, h4, h5, h6, p, .subtext", mapOf("whitespace" to "pre-wrap"))
//        dynamicCss.style("h2", mapOf("font-size" to "1.6rem", "whitespace" to "pre-wrap"))
//        dynamicCss.style("h3", mapOf("font-size" to "1.4rem", "whitespace" to "pre-wrap"))
//        dynamicCss.style("h4", mapOf("font-size" to "1.3rem", "whitespace" to "pre-wrap"))
//        dynamicCss.style("h5", mapOf("font-size" to "1.2rem", "whitespace" to "pre-wrap"))
//        dynamicCss.style("h6", mapOf("font-size" to "1.1rem", "whitespace" to "pre-wrap"))
//        dynamicCss.style("p", mapOf("font-size" to "1rem", "whitespace" to "pre-wrap"))
//        dynamicCss.style(".subtext", mapOf("font-size" to "0.8rem", "opacity" to "0.8", "whitespace" to "pre-wrap"))
//        style.visibility = if (value) "visible" else "hidden"
        dynamicCss.style(
            ".visibleOnParentHover",
            mapOf(
                "visibility" to "hidden",
                "width" to "auto",
                "height" to "auto",
                "max-width" to "unset",
                "max-height" to "unset",
            )
        )
        dynamicCss.style(":hover>.visibleOnParentHover", mapOf("visibility" to "visible"))
        dynamicCss.style(":hover.visibleOnParentHover", mapOf("visibility" to "visible"))
        dynamicCss.style(
            ".kiteui-space",
            mapOf("min-width" to "calc(var(--space-multiplier, 1.0) * var(--spacing, 0px))")
        )

        dynamicCss.style(".swapImage", mapOf("overflow" to "hidden"))
        dynamicCss.style(
            ".swapImage > img",
            mapOf("object-fit" to "contain", "transition-duration" to "var(--transition-duration, 0.25s)")
        )
        dynamicCss.style(".swapImage.scaleType-Fit > img", mapOf("object-fit" to "contain"))
        dynamicCss.style(".swapImage.scaleType-Crop > img", mapOf("object-fit" to "cover"))
        dynamicCss.style(".swapImage.scaleType-Stretch > img", mapOf("object-fit" to "fill"))
        dynamicCss.style(".swapImage.scaleType-NoScale > img", mapOf("object-fit" to "none"))
        dynamicCss.style("video.scaleType-Fit", mapOf("object-fit" to "contain"))
        dynamicCss.style("video.scaleType-Crop", mapOf("object-fit" to "cover"))
        dynamicCss.style("video.scaleType-Stretch", mapOf("object-fit" to "fill"))
        dynamicCss.style("video.scaleType-NoScale", mapOf("object-fit" to "none"))

        dynamicCss.style(
            ".noInteraction.noInteraction", mapOf(
                "pointer-events" to "none"
            )
        )
        dynamicCss.style(
            ".noInteraction > *", mapOf(
                "pointer-events" to "auto"
            )
        )

        dynamicCss.style(
            "body", mapOf(
                "height" to "100svh",
                "max-height" to "100svh",
                "max-width" to "100vw",
                "overflow" to "hidden"
            )
        )

        dynamicCss.style(
            "body > div", mapOf(
                "height" to "100%",
                "max-width" to "100vw",
            )
        )

        dynamicCss.style(
            "a", mapOf(
                "text-decoration" to "none",
                "color" to "unset",
            )
        )

        dynamicCss.style(
            "a:visited", mapOf(
                "color" to "unset",
            )
        )

        dynamicCss.style(
            "button, input, textarea, select", mapOf(
                "background" to "none",
                "border-width" to "0",
                "outline-width" to "0",
                "font" to "unset",
                "color" to "unset",
                "text-align" to "start",
            )
        )

//        DynamicCss.style(
//            "input.sameThemeText, textarea.sameThemeText", mapOf(
//                "border-bottom-style" to "solid",
//                "border-bottom-width" to "1px",
//                "border-radius" to "0",
//            )
//        )
//
//        DynamicCss.style(
//            "input.sameThemeText:focus, textarea.sameThemeText:focus", mapOf(
//                "border-radius" to "0",
//            )
//        )
//
//        DynamicCss.style(
//            "input:not(.mightTransition).editable.editable, textarea:not(.mightTransition).editable.editable, select:not(.mightTransition).editable.editable", mapOf(
//                "border-bottom-color" to "currentColor",
//                "border-bottom-width" to "1px",
//                "border-bottom-style" to "solid",
//            )
//        )
//
//        DynamicCss.style(
//            "input:not(.mightTransition).editable.editable:focus, textarea:not(.mightTransition).editable.editable:focus, select:not(.mightTransition).editable.editable:focus", mapOf(
//                "border-bottom-color" to "currentColor",
//                "border-bottom-width" to "2px",
//                "border-bottom-style" to "solid",
//                "outline" to "none",
//            )
//        )

        dynamicCss.style(
            "input:focus, textarea:focus", mapOf(
                "outline" to "none",
            )
        )

        dynamicCss.style(
            ".toggle-button", mapOf(
                "display" to "flex",
                "align-items" to "stretch",
            )
        )

        dynamicCss.style(
            ".toggle-button > span", mapOf(
                "flex-grow" to "1",
                "flex-shrink" to "1",
                "display" to "block",
            )
        )

        dynamicCss.style(
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


        val rowForceFlex = ".kiteui-row"
//        val rowForceFlex = ".childHasWeight"
        """
    .kiteui-col:not(.childHasWeight) > .kiteui-col.childHasWeight,
    .kiteui-col:not(.childHasWeight) > .kiteui-row$rowForceFlex,
    .kiteui-col:not(.childHasWeight) > .kiteui-label,
    .kiteui-col:not(.childHasWeight) > .kiteui-stack:has(> .vCenter:only-child),
    .kiteui-col:not(.childHasWeight) > .rowCollapsing {
        display: block flex;
    }
    .kiteui-row:not($rowForceFlex) > .kiteui-col.childHasWeight,
    .kiteui-row:not($rowForceFlex) > .kiteui-row$rowForceFlex,
    .kiteui-row:not($rowForceFlex) > .kiteui-label,
    .kiteui-row:not($rowForceFlex) > .kiteui-stack:has(> .vCenter:only-child),
    .kiteui-row:not($rowForceFlex) > .rowCollapsing {
        display: inline-flex;
    }
    .kiteui-col:not(.childHasWeight) > .kiteui-stack:has(> :not(:only-child)) {
        display: block grid;
    }
    .kiteui-row:not($rowForceFlex) > .kiteui-stack:has(> :not(:only-child)) {
        display: inline-grid;
    }


    .kiteui-col.childHasWeight > .hEnd {
        align-self: end;
    }
    .kiteui-col.childHasWeight > .hStretch {
        align-self: stretch;
    }
    .kiteui-col.childHasWeight > .hCenter {
        align-self: center;
    }
    .kiteui-col.childHasWeight > .hStart {
        align-self: start;
    }
    .kiteui-col.childHasWeight {
        display: flex;
        flex-direction: column;
    }



    .kiteui-col:not(.childHasWeight) > * {
        display: block;
        width: calc(100%);
    }
    .kiteui-col:not(.childHasWeight) > :not(:last-child) {
        margin-bottom: var(--parentSpacing, 0);
    }
    .kiteui-col:not(.childHasWeight) > .hEnd {
        width: fit-content;
        margin-left: auto;
    }
    .kiteui-col:not(.childHasWeight) > .hCenter {
        width: fit-content;
        margin-left: auto;
        margin-right: auto;
    }
    .kiteui-col:not(.childHasWeight) > .hStart {
        width: fit-content;
        margin-right: auto;
    }


    .kiteui-row:not($rowForceFlex) > * {
        display: inline-block;
        height: calc(100% - var(--parentPadding, 0) * 2);
        vertical-align: top;
    }
    .kiteui-row:not($rowForceFlex) > :not(:last-child) {
        margin-right: var(--parentSpacing, 0);
    }
    .kiteui-row:not($rowForceFlex) {
        white-space: nowrap;
    }
    .kiteui-row:not($rowForceFlex)::before {
      content: "\200B";
      display: inline-block;
      height: 100%; 
      vertical-align: middle;
    }
    .kiteui-row:not($rowForceFlex) > .vEnd {
        vertical-align: bottom;
        height: fit-content;
    }
    .kiteui-row:not($rowForceFlex) > .vCenter {
        vertical-align: middle;
        height: fit-content;
    }
    .kiteui-row:not($rowForceFlex) > .vStart {
        vertical-align: top;
        height: fit-content;
    }


    .kiteui-row$rowForceFlex > .vEnd {
        align-self: end;
    }
    .kiteui-row$rowForceFlex > .vStretch {
        align-self: stretch;
    }
    .kiteui-row$rowForceFlex > .vCenter {
        align-self: center;
    }
    .kiteui-row$rowForceFlex > .vStart {
        align-self: start;
    }
    .kiteui-row$rowForceFlex {
        display: flex;
        flex-direction: row;
    }
    
    
    .kiteui-stack > .vEnd:not(:only-child) {
        align-self: end;
    }

    .kiteui-stack > .vStretch:not(:only-child) {
        align-self: stretch;
    }

    .kiteui-stack > .vCenter:not(:only-child) {
        align-self: center;
    }
    .kiteui-stack > .vStart:not(:only-child) {
        align-self: start;
    }
    .kiteui-stack > .hEnd:not(:only-child) {
        justify-self: end;
    }
    .kiteui-stack > .hStretch:not(:only-child) {
        justify-self: stretch;
    }

    .kiteui-stack > .hCenter:not(:only-child) {
        justify-self: center;
    }

    .kiteui-stack > .hStart:not(:only-child) {
        justify-self: start;
    }



    .kiteui-stack > :not(:only-child) {
        grid-column-start: 1;
        grid-column-end: 1;
        grid-row-start: 1;
        grid-row-end: 1;
        align-self: stretch;
        justify-self: stretch;
    }

    .kiteui-stack:has(> :not(:only-child)) {
        display: grid;
        grid-template-columns: 100%;
        grid-template-rows: 100%;
    }

    .kiteui-stack:has(> :only-child) {
        line-height: 0px !important;
    }

    .kiteui-stack > :only-child {
        height: 100%;
        width: 100%;
        vertical-align: top;
        display: block;
    }

    .kiteui-stack > .vEnd:only-child {
        height: unset;
        vertical-align: top;
    }

    .kiteui-stack > .vStart:only-child {
        height: unset;
        vertical-align: bottom;
    }

    .kiteui-stack > .hEnd:only-child {
        width: unset;
        margin-left: auto;
    }

    .kiteui-stack > .hCenter:only-child {
        width: unset;
        margin-left: auto;
        margin-right: auto;
    }

    .kiteui-stack > .hStart:only-child {
        width: unset;
        margin-right: auto;
    }
    
    .kiteui-stack:has(> .vCenter:only-child) {
          display: flex;
          align-items: center;
          justify-content: center;
    }
    .kiteui-stack > .vCenter:only-child {
        height: fit-content;
    }
    
        """.trimIndent().split('}').filter { it.isNotBlank() }.forEach { dynamicCss.rule("$it}") }

        dynamicCss.style(
            "img", mapOf(
                "overflow" to "hidden",
            )
        )

        dynamicCss.style(
            "p.loading:not(.inclBack), h1.loading:not(.inclBack), h2.loading:not(.inclBack), h3.loading:not(.inclBack), h4.loading:not(.inclBack), h5.loading:not(.inclBack), h6.loading:not(.inclBack), img.loading:not(.inclBack), input.loading:not(.inclBack), select.loading:not(.inclBack), textarea.loading:not(.inclBack)",
            mapOf(
                "min-height" to "1.4em",
                "background" to "color-mix(in srgb, currentColor, transparent 70%) !important",
                "animation" to "flickerAnimation 2s infinite !important",
                "animation-timing-function" to "linear",
            )
        )

        dynamicCss.style(
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
        dynamicCss.style("button", mapOf("position" to "relative"))
        dynamicCss.style(
            "button.loading > *", mapOf(
                "opacity" to "0.15",
            )
        )

        dynamicCss.style(
            ".clickable", mapOf(
                "cursor" to "pointer",
            )
        )

        dynamicCss.style(
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

        dynamicCss.style(
            ".switch:not(:checked)", mapOf(
                "background-color" to "rgb(204, 204, 204) !important",
                "background-image" to "none !important",
            )
        )

        dynamicCss.style(
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

        dynamicCss.style(
            ".switch:checked:before", mapOf(
                "left" to "calc(3rem - 1.6rem)",
            )
        )

        dynamicCss.style(
            ".checkbox", mapOf(
                "appearance" to "none",
                "width" to "1.5rem",
                "height" to "1.5rem",
                "position" to "relative",
                "padding" to "0px !important",
                "border-width" to "0.1rem",
                "border-style" to "solid",
                "opacity" to "0.75",
            )
        )
        dynamicCss.style(
            ".checkbox:checked", mapOf(
                "opacity" to "1",
            )
        )
        dynamicCss.style(
            ".checkbox::after", mapOf(
                "position" to "absolute",
                "content" to "\"\"",
                "display" to "block",
                "width" to "0.8rem",
                "height" to "0.3rem",
                "top" to "0.3rem",
                "left" to "0.16rem",
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
        dynamicCss.style(
            ":checked.checkbox::after", mapOf(
                "opacity" to "1",
                "transform" to "rotate(-45deg)"
            )
        )
        dynamicCss.style(
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
        dynamicCss.style(
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
        dynamicCss.style(
            ":checked.radio::after", mapOf(
                "opacity" to "1",
                "transform" to "none"
            )
        )

        dynamicCss.style(
            ".crowd", mapOf(
                "padding" to "0 !important",
            )
        )

        dynamicCss.style(
            ".kiteui-label.kiteui-label", mapOf(
                "display" to "flex",
                "flex-direction" to "column",
                "align-items" to "stretch",
            )
        )

        dynamicCss.style(
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

        dynamicCss.style(
            "input", mapOf(
                "min-height" to "1.5rem",
                "min-width" to "1.5rem",
            )
        )

        dynamicCss.style(
            "::placeholder", mapOf(
                "color" to "currentColor",
                "opacity" to "0.3",
            )
        )

        dynamicCss.style(
            ".kiteui-separator", mapOf(
                "background-color" to "currentColor",
                "opacity" to "0.25",
                "min-width" to "1px",
                "min-height" to "1px",
            )
        )

        dynamicCss.style(
            "iframe#webpack-dev-server-client-overlay", mapOf(
                "display" to "none !important"
            )
        )


        dynamicCss.style(
            ".recycler", mapOf(
                "overflow-y" to "auto"
            )
        )

        dynamicCss.style(
            ".recycler > *", mapOf(
                "max-height" to "unset",
            )
        )

        dynamicCss.style(
            ".recycler-horz", mapOf(
                "display" to "flex",
                "flex-direction" to "row",
                "overflow-x" to "auto",
            )
        )

        dynamicCss.style(
            ".recycler-horz > *", mapOf(
                "max-width" to "unset",
            )
        )

        dynamicCss.style(
            ".recycler-grid", mapOf(
                "display" to "flex",
                "flex-direction" to "row",
                "overflow-x" to "auto",
            )
        )

        dynamicCss.style(
            ".scroll-vertical > *", mapOf(
                "max-height" to "unset",
            )
        )

        dynamicCss.style(
            ".scroll-vertical", mapOf(
                "overflow-y" to "auto",
                "overflow-x" to "hidden",
            )
        )

        dynamicCss.style(
            ".scroll-horizontal > *", mapOf(
                "max-width" to "unset",
            )
        )

        dynamicCss.style(
            ".scroll-horizontal", mapOf(
                "overflow-x" to "auto",
                "overflow-y" to "hidden",
            )
        )

        dynamicCss.style(
            "::-webkit-scrollbar", mapOf(
                "background" to "#0000",
            )
        )

        dynamicCss.style(
            "::-webkit-scrollbar-thumb", mapOf(
                "background" to "color-mix(in srgb, currentColor 20%, transparent)",
                "-webkit-border-radius" to "4px",
            )
        )

        dynamicCss.style(
            "::-webkit-scrollbar-corner", mapOf(
                "background" to "#0000"
            )
        )

        dynamicCss.rule(
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
        dynamicCss.rule(
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

//        DynamicCss.style(
//            ".kiteui-row > [hidden]", mapOf(
//                "width" to "0px !important",
//                "transform" to "scaleX(0)",
//            )
//        )
//        DynamicCss.style(
//            ".kiteui-col > [hidden]", mapOf(
//                "height" to "0px !important",
//                "transform" to "scaleY(0)",
//            )
//        )
//        DynamicCss.style(
//            ".kiteui-stack > [hidden]", mapOf(
////                "width" to "0px !important",
////                "height" to "0px !important",
//                "transform" to "scale(0, 0)",
//            )
//        )
        dynamicCss.style(
            "[hidden]", mapOf(
                "display" to "none !important",
            )
        )

        dynamicCss.style(
            ":not(.unkiteui)", mapOf(
                "transition-timing-function" to "linear",
                "transition-delay" to "0s",
                "transition-property" to "color, background-image, background-color, border-color outline-color, box-shadow, border-radius, opacity, backdrop-filter",
            )
        )

        dynamicCss.style(
            ":not(.unkiteui).animatingShowHide", mapOf(
                "overflow" to "hidden",
                "minWidth" to "0px",
                "minHeight" to "0px",
            )
        )

        dynamicCss.style(
            ".notransition, .notransition *", mapOf(
                "transition" to "none !important"
            )
        )

        dynamicCss.style(
            ".dismissBackground", mapOf(
                "z-index" to "998",
                "pointer-events" to "auto"
            )
        )
        dynamicCss.style(
            ".dismissBackground + *", mapOf(
                "z-index" to "999",
            )
        )
//        recyclerView
        dynamicCss.style(
            ".recyclerView", mapOf(
                "position" to "relative",
                "padding" to "0 !important"
            )
        )

        dynamicCss.style(
            ".contentScroll-V::-webkit-scrollbar", mapOf(
                "display" to "none"
            )
        )
        dynamicCss.style(
            ".contentScroll-H::-webkit-scrollbar", mapOf(
                "display" to "none"
            )
        )
        dynamicCss.style(
            ".contentScroll-V", mapOf(
                "width" to "100%",
                "height" to "100%",
                "position" to "relative",
                "overflow-y" to "scroll",
                "overflow-anchor" to "none",
                "scrollbar-width" to "none",
            )
        )
        dynamicCss.style(
            ".contentScroll-H", mapOf(
                "width" to "100%",
                "height" to "100%",
                "position" to "relative",
                "overflow-x" to "scroll",
                "overflow-anchor" to "none",
                "scrollbar-width" to "none",
            )
        )
        dynamicCss.style(
            ".contentScroll-V > *", mapOf(
                "position" to "absolute",
                "max-height" to "unset",
                "width" to "calc(100% - var(--parentSpacing, 0px) * var(--usePadding, 0) * 2)",
                "margin-left" to "calc(var(--parentSpacing, 0px) * var(--usePadding, 0))",
                "margin-right" to "calc(var(--parentSpacing, 0px) * var(--usePadding, 0))",
                "overflow-anchor" to "revert",
            )
        )
        dynamicCss.style(
            ".contentScroll-H > *", mapOf(
                "max-width" to "unset",
                "position" to "absolute",
                "height" to "calc(100% - var(--parentSpacing, 0px) * var(--usePadding, 0) * 2)",
                "margin-top" to "calc(var(--parentSpacing, 0px) * var(--usePadding, 0))",
                "margin-bottom" to "calc(var(--parentSpacing, 0px) * var(--usePadding, 0))",
                "overflow-anchor" to "revert",
            )
        )
        dynamicCss.style(
            ".contentScroll-V > .recyclerViewGridSub", mapOf(
                "display" to "flex",
                "flex-direction" to "row",
                "gap" to "var(--spacing, 0)",
                "height" to "auto"
            )
        )
        dynamicCss.style(
            ".contentScroll-H > .recyclerViewGridSub", mapOf(
                "display" to "flex",
                "flex-direction" to "column",
                "gap" to "var(--spacing, 0)",
                "width" to "auto"
            )
        )
        dynamicCss.style(
            ".recyclerViewGridSub > *", mapOf(
                "flex-grow" to "1",
                "flex-shrink" to "1",
                "flex-basis" to "0",
            )
        )
//        contentScroll
//        content
//        barScroll
//        barContent
        dynamicCss.style(
            ".viewPager", mapOf(
                "scroll-snap-type" to "x mandatory"
            )
        )
        dynamicCss.style(
            ".viewPager > :not(.recyclerViewCap)", mapOf(
                "width" to "var(--pager-width, 0rem)",
                "height" to "var(--pager-height, 0rem)",
                "scroll-snap-align" to "center",
                "scroll-snap-stop" to "always",
            )
        )
        dynamicCss.style(
            ".touchscreenOnly", mapOf(
                "visibility" to "gone"
            )
        )
        dynamicCss.rule(
            """
            @media (pointer: coarse) and (hover: none) {
                .touchscreenOnly {
                    visibility: visible
                }
            }
        """.trimIndent()
        )
        dynamicCss.style(
            "progress", mapOf(
                "height" to "0.5rem",
                "border" to "none",
                "border-radius" to "1rem",
                "padding" to "0px !important",
                "appearance" to "none",
                "background" to "color-mix(in srgb, currentColor 20%, transparent)",
            )
        )
        try {
            dynamicCss.style(
                "progress::-webkit-progress-value", mapOf(
                    "height" to "100%",
                    "background-color" to "currentColor",
                    "border-radius" to "1rem",
                )
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.style(
                "progress::-moz-progress-bar", mapOf(
                    "height" to "100%",
                    "background-color" to "currentColor",
                    "border-radius" to "1rem",
                )
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.style(
                "input::-webkit-outer-spin-button, input::-webkit-inner-spin-button", mapOf(
                    "-webkit-appearance" to "none",
                )
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.style(
                "input[type=number]", mapOf(
                    "-moz-appearance" to "textfield"
                )
            )
        } catch (e: Throwable) { /*squish*/
        }
        dynamicCss.style(
            ".mightTransition:not(.isRoot):not(.swapImage):not(.unpadded):not(.toggle-button.unpadded > *), .padded:not(.unpadded):not(.toggle-button.unpadded > *):not(.swapImage)",
            mapOf(
                "padding" to "var(--spacing, 0px)",
            )
        )
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

        dynamicCss.rule(buildString {
            append("@keyframes transition-${transition.name}-enter { from { ")
            extracted(transition.enter)
        }, 0)
        dynamicCss.rule(buildString {
            append("@keyframes transition-${transition.name}-exit { from { ")
            extracted(transition.exit)
        }, 0)
        return "transition-${transition.name}"
    }

    private fun Dimension.toBoxShadow(): String {
        if (value == "0px")
            return "none"
        val offsetX = 0.px.value
        val offsetY = value
        val blur = (this * 2).value
        val spread = 0.px.value
        return "$offsetX $offsetY $blur $spread #00000099"
    }

    private fun Duration.toCss() = this.toDouble(DurationUnit.SECONDS).toString() + "s"
    private fun Paint.toCss() = when (this) {
        is Color -> this.toWeb()
        is LinearGradient -> "linear-gradient(${angle.plus(Angle.quarterTurn).turns}turn, ${joinGradientStops(stops)})"
        is RadialGradient -> "radial-gradient(circle at center, ${joinGradientStops(stops)})"
    }

    private fun BackdropFilter.toCss(): String = when (this) {
        is BackdropFilter.Blur -> "blur(${amount.value})"
    }

    private fun joinGradientStops(stops: List<GradientStop>): String {
        return stops.joinToString {
            "${it.color.toWeb()} ${it.ratio * 100}%"
        }
    }

    fun themeInteractive(theme: Theme): List<String> {
        var current = theme.derivedFrom
        while (current != null) {
            theme(current)
            current = current.derivedFrom
        }
        val cs = theme.classSelector
        theme(theme)
        fun sub(subtheme: Theme, asSelectors: List<String>) {
            theme(subtheme, asSelectors = asSelectors.map { "$it$cs" })
            theme(subtheme.down(), asSelectors = asSelectors.flatMap {
                listOf(
                    ".clickable:active $it$cs",
                    ".clickable:active$it$cs",
                )
            }, includeMaybeTransition = true)
            theme(subtheme.hover(), asSelectors = asSelectors.flatMap {
                listOf(
                    ".clickable:hover $it$cs",
                    ".clickable:hover$it$cs",
                )
            }, includeMaybeTransition = true)
            theme(subtheme.focus(), asSelectors = asSelectors.flatMap {
                listOf(
                    ".clickable:focus-visible $it$cs",
                    ".clickable:focus-visible$it$cs",
                    "input:focus$it$cs",
                    "textarea:focus$it$cs",
                    "select:focus$it$cs",
                    "$it:has(> input:focus-visible:not(.mightTransition))$cs",
                    "$it:has(> textarea:focus-visible:not(.mightTransition))$cs",
                    "$it:has(> select:focus-visible:not(.mightTransition))$cs",
                )
            }, includeMaybeTransition = true)
            theme(subtheme.disabled(), asSelectors = asSelectors.flatMap {
                listOf(
                    ".clickable:disabled $it$cs",
                    ".clickable:disabled$it$cs",
                )
            })
        }
        sub(theme, asSelectors = listOf(""))
        sub(theme.selected(), asSelectors = listOf(":has(> input:checked).checkResponsive"))
        sub(theme.unselected(), asSelectors = listOf(":has(> input:not(:checked)).checkResponsive"))
        return theme.classes
    }

    private val Theme.classes: List<String>
        get() = buildList<String> {
            generateSequence(this@classes) { it.derivedFrom }
                .toList()
                .reversed()
                .forEachIndexed { index, t ->
                    add("t-" + (t.derivationId?.let { index.toString() + it } ?: t.id))
                }
        }
    private val Theme.classSelector get() = classes.joinToString("") { ".$it" }

    private val themeHandled = HashSet<String>()
    fun theme(
        theme: Theme,
        asSelectors: List<String> = listOf(theme.classSelector),
        includeMaybeTransition: Boolean = false,
        mediaQuery: String? = null,
    ): List<String> {
        val classes = theme.classes
        val includeSelectors = asSelectors.filter { themeHandled.add(it) }
        if (includeSelectors.isEmpty()) return classes

        fun sel(vararg plus: String): String {
            return includeSelectors.asSequence().flatMap { plus.asSequence().map { p -> "$it$p" } }.joinToString(", ")
        }

        fun l(theme: Theme) = listOf(
//            sel(
//                ".mightTransition:not(.isRoot):not(.swapImage):not(.unpadded):not(.toggle-button.unpadded > *) > *",
//                ".padded:not(.unpadded):not(.toggle-button.unpadded > *):not(.swapImage) > *"
//            ) to mapOf(
//                "--parentPadding" to theme.spacing.value,
//            ),
//            sel(
//                " > *",
//            ) to mapOf(
//                "--parentSpacing" to theme.spacing.value,
//            ),
            sel(".useNavSpacing") to mapOf(
                "--spacing" to theme.navSpacing.value,
                "gap" to "var(--spacing, 0.0)",
            ),
//            sel(
//                ".useNavSpacing > *",
//            ) to mapOf(
//                "--parentSpacing" to theme.navSpacing.value,
//            ),
            sel(".mightTransition", ".transition", ".swapImage") to mapOf(
                "border-radius" to theme.cornerRadii.toRawCornerRadius(),
            ),
            sel(".icon") to mapOf(
                "color" to theme.icon.toCss()
            ),
            (if (includeMaybeTransition) sel(".mightTransition") else sel(".transition")) to (when (val it =
                theme.background) {
                is Color -> mapOf(
                    "background-color" to it.toCss(),
                    "background-image" to "none",
                )

                is LinearGradient -> mapOf(
                    "background-color" to it.closestColor().toCss(),
                    "background-image" to "linear-gradient(${it.angle.plus(Angle.quarterTurn).turns}turn, ${
                        joinGradientStops(
                            it.stops
                        )
                    })",
                    "background-attachment" to (if (it.screenStatic) "fixed" else "unset"),
                )

                is RadialGradient -> mapOf(
                    "background-color" to it.closestColor().toCss(),
                    "background-image" to "radial-gradient(circle at center, ${joinGradientStops(it.stops)})",
                    "background-attachment" to (if (it.screenStatic) "fixed" else "unset"),
                )
            } + (if (theme.backdropFilters.isNotEmpty()) mapOf(
                "backdrop-filter" to theme.backdropFilters.joinToString(" ") { it.toCss() }) else emptyMap())),

            (if (includeMaybeTransition) sel(".mightTransition") else sel(".transition")) to mapOf(
                "outline-width" to theme.outlineWidth.value,
                "outline-offset" to (theme.outlineWidth * -1).value,
                "box-shadow" to theme.elevation.toBoxShadow(),
                "outline-style" to if (theme.outlineWidth != 0.px) "solid" else "none",
            ),
            sel("") to mapOf(
                "color" to theme.foreground.toCss(),
                "--spacing" to theme.spacing.value,
                "gap" to "var(--spacing, 0.0)",
                "font-size" to theme.font.size.value,
                "font-family" to dynamicCss.font(theme.font.font),
                "font-weight" to theme.font.weight.toString(),
                "font-style" to if (theme.font.italic) "italic" else "normal",
                "text-transform" to if (theme.font.allCaps) "uppercase" else "none",
                "line-height" to theme.font.lineSpacingMultiplier.toString(),
                "letter-spacing" to theme.font.additionalLetterSpacing.toString(),
                "outline-color" to theme.outline.toCss(),
                "transition-duration" to theme.transitionDuration.toCss(),
                "--transition-duration" to theme.transitionDuration.toCss(),
            ) + when (val it = theme.foreground) {
                is Color -> mapOf("color" to it.toCss())
                is LinearGradient, is RadialGradient -> mapOf(
                    "color" to it.toCss(),
                    "background" to "-webkit-${it.toCss()}",
                    "-webkit-background-clip" to "text",
                    "-webkit-text-fill-color" to "transparent",
                )
            },
            sel(".dismissBackground") to mapOf(
                "border-radius" to "0",
                "outline-width" to "0",
                "backdrop-filter" to "blur(5px)",
                "-webkit-backdrop-filter" to "blur(5px)",
            ) + when (val it = theme.background.darken(0.5f).applyAlpha(0.5f)) {
                is Color -> mapOf("background-color" to it.toCss())
                is LinearGradient -> mapOf(
                    "background-image" to "linear-gradient(${it.angle.plus(Angle.quarterTurn).turns}turn, ${
                        joinGradientStops(
                            it.stops
                        )
                    })",
                    "background-attachment" to (if (it.screenStatic) "fixed" else "unset"),
                )

                is RadialGradient -> mapOf(
                    "background-image" to "radial-gradient(circle at center, ${joinGradientStops(it.stops)})",
                    "background-attachment" to (if (it.screenStatic) "fixed" else "unset"),
                )
            }
        )

        var skipped = 0
        dynamicCss.styles(
            mediaQuery = mediaQuery,
            styles = l(theme).let {
                val diffTheme = theme.derivedFrom
                if (diffTheme != null) {
                    val toRemove = l(diffTheme)
                    it.zip(toRemove) { a, b ->
                        val m: Map<String, String> = a.second.filter {
                            val x = it.value != b.second[it.key]
                            if (!x) skipped++
                            x
                        }
                        a.first to m
                    }.filter { it.second.isNotEmpty() }
                } else it
            }.groupBy { it.first }.mapValues { it.value.flatMap { it.second.entries }.associate { it.key to it.value } }
        )

        return classes
    }

    val rowCollapsingToColumnHandled = HashSet<String>()
    fun rowCollapsingToColumn(breakpoint: Dimension): String {
        val name = "rowCollapsingToColumn_${breakpoint.value.filter { it.isLetterOrDigit() }}"
        if (rowCollapsingToColumnHandled.add(name)) {
            dynamicCss.style(
                ".$name", mapOf(
                    "display" to "flex"
                )
            )
            dynamicCss.rule(
                """
                    @media (min-width: ${breakpoint.value}) {
                        .$name {
                            flex-direction: row;
                        }
                        .$name > .vStart {
                            align-self: start;
                        }
                        .$name > .vCenter {
                            align-self: center;
                        }
                        .$name > .vStretch {
                            align-self: stretch;
                        }
                        .$name > .vEnd {
                            align-self: end;
                        }
                    }
                """.trimIndent()
            )
            dynamicCss.rule(
                """
                    @media (max-width: ${breakpoint.value}) {
                        .$name {
                            flex-direction: column;
                        }
                        .$name > * {
                            flex-grow: 0 !important;
                            flex-shrink: 0 !important;
                            flex-basis: auto !important;
                        }
                        .$name > .hStart {
                            align-self: start;
                        }
                        .$name > .hCenter {
                            align-self: center;
                        }
                        .$name > .hStretch {
                            align-self: stretch;
                        }
                        .$name > .hEnd {
                            align-self: end;
                        }
                    }
                """.trimIndent()
            )
        }
        return name
    }
}
