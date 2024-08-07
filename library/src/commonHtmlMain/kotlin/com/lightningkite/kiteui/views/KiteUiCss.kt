package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.models.*
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.measureTime

class KiteUiCss(val dynamicCss: DynamicCss) {
    init {
        // basis rules
        //language=CSS
        @Suppress("CssUnresolvedCustomProperty")
        dynamicCss.rule(
            """
            /*noinspection ALL*/@media {
            html, body, div, span, applet, object, iframe, h1, h2, h3, h4, h5, h6, p, blockquote, pre, a, abbr, acronym, address, big, cite, code, del, dfn, em, img, ins, kbd, q, s, samp, small, strike, strong, sub, sup, tt, var, b, u, i, center, dl, dt, dd, ol, ul, li, fieldset, form, label, legend, table, caption, tbody, tfoot, thead, tr, th, td, article, aside, canvas, details, embed, figure, figcaption, footer, header, hgroup, menu, nav, output, ruby, section, summary, time, mark, audio, video {
                margin: 0;
                padding: 0;
                border: 0;
                font-size: 100%;
                font: inherit;
                vertical-align: baseline;
            }
            p :is(html, body, div, span, applet, object, iframe, h1, h2, h3, h4, h5, h6, p, blockquote, pre, a, abbr, acronym, address, big, cite, code, del, dfn, em, img, ins, kbd, q, s, samp, small, strike, strong, sub, sup, tt, var, b, u, i, center, dl, dt, dd, ol, ul, fieldset, form, label, legend, table, caption, tbody, tfoot, thead, tr, th, td, article, aside, canvas, details, embed, figure, figcaption, footer, header, hgroup, menu, nav, output, ruby, section, summary, time, mark, audio, video) {
                margin: revert;
                padding: revert;
                border: revert;
                font-size: revert;
                vertical-align: revert;
            }
            p li {
                margin-inline-start: 1em;
            }
            p a {
                text-decoration: revert;
                color: revert;
            }
            p a:visited {
                color: revert;
            }
    
            /* HTML5 display-role reset for older browsers */
            article, aside, details, figcaption, figure, footer, header, hgroup, menu, nav, section {
                display: block;
            }
    
            body {
                line-height: 1;
            }
    
            ol, ul {
                list-style: none;
            }
    
            blockquote, q {
                quotes: none;
            }
    
            blockquote:before, blockquote:after, q:before, q:after {
                content: '';
                content: none;
            }
    
            table {
                border-collapse: collapse;
                border-spacing: 0;
            }
                
            .useNavSpacing.useNavSpacing.useNavSpacing.useNavSpacing.useNavSpacing.useNavSpacing.useNavSpacing {
                --spacing: var(--navSpacing, 0px);
            }

            .icon {
                color: var(--icon-color, black);
            }

            .dismissBackground {
                border-radius: 0px;
                outline-width: 0px;
                background-color: color-mix(in srgb, color-mix(in srgb, var(--nearest-background-color, black) 50%, black) 50%, transparent);
            }

            .mightTransition:not(.isRoot):not(.swapImage):not(.unpadded), .padded:not(.unpadded):not(.swapImage) {
                padding: var(--spacing, 0px);
            }

            input[type="number"] {
                appearance: textfield;
            }

            input::-webkit-outer-spin-button, input::-webkit-inner-spin-button {
                appearance: none;
            }

            progress::-moz-progress-bar {
                height: 100%;
                background-color: currentcolor;
                border-radius: 1rem;
            }

            progress::-webkit-progress-bar {
                border-radius: 100px;
            }

            progress::-webkit-progress-value {
                height: 100%;
                background-color: currentcolor;
                border-radius: 1rem;
            }

            progress {
                height: 0.5rem;
                border: medium;
                border-radius: 1rem;
                padding: 0px !important;
                appearance: none;
            }

            @media (pointer: coarse) and (hover: none) {
                .touchscreenOnly {
                    visibility: visible;
                }
            }
            
            * {
                gap: var(--spacing, 0);
            }       

            .touchscreenOnly {
            }
            .disableContextMenu {
                -webkit-user-select: none !important;  
                -webkit-touch-callout: none !important;  
            }

            .viewPager > :not(.recyclerViewCap) {
                width: var(--pager-width, 0rem);
                height: var(--pager-height, 0rem);
                scroll-snap-align: center;
                scroll-snap-stop: always;
            }

            .viewPager {
                scroll-snap-type: x mandatory;
            }

            .recyclerViewGridSub > * {
                flex: 1 1 0px;
            }

            .contentScroll-H > .recyclerViewGridSub {
                display: flex;
                flex-direction: column;
                width: auto;
            }

            .contentScroll-V > .recyclerViewGridSub {
                display: flex;
                flex-direction: row;
                height: auto;
            }

            .contentScroll-H > * {
                max-width: unset;
                position: absolute;
                height: calc(100% - var(--parentSpacing, 0px) * var(--usePadding, 0) * 2);
                margin-top: calc(var(--parentSpacing, 0px) * var(--usePadding, 0));
                margin-bottom: calc(var(--parentSpacing, 0px) * var(--usePadding, 0));
                overflow-anchor: revert;
            }

            .contentScroll-V > * {
                position: absolute;
                max-height: unset;
                width: calc(100% - var(--parentSpacing, 0px) * var(--usePadding, 0) * 2);
                margin-left: calc(var(--parentSpacing, 0px) * var(--usePadding, 0));
                margin-right: calc(var(--parentSpacing, 0px) * var(--usePadding, 0));
                overflow-anchor: revert;
            }

            .contentScroll-H {
                width: 100%;
                height: 100%;
                position: relative;
                overflow-x: scroll;
                overflow-anchor: none;
                scrollbar-width: none;
            }

            .contentScroll-V {
                width: 100%;
                height: 100%;
                position: relative;
                overflow-y: scroll;
                overflow-anchor: none;
                scrollbar-width: none;
            }

            .contentScroll-H::-webkit-scrollbar {
                display: none;
            }

            .contentScroll-V::-webkit-scrollbar {
                display: none;
            }

            .recyclerView > * > * {
                overflow-anchor: none;
            }

            .recyclerView > * {
                overflow-anchor: none;
                scroll-behavior: auto !important;
            }

            .recyclerView {
                position: relative;
                padding: 0px !important;
                scroll-behavior: auto;
            }

            .dismissBackground + * {
                z-index: 999;
            }

            .dismissBackground {
                z-index: 998;
                pointer-events: auto;
            }

            .notransition, .notransition * {
                transition: none !important;
            }

            *.animatingShowHide {
                overflow: hidden;
            }

            * {
                transition-timing-function: linear;
                transition-delay: 0s;
                transition-property: color, background-image, background-color, border-color, outline-color, box-shadow, border-radius, opacity, backdrop-filter;
            }

            [hidden] {
                display: none !important;
            }

            @keyframes spin {
                0% {
                    transform: rotate(0deg);
                }
                100% {
                    transform: rotate(360deg);
                }
            }

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

            ::-webkit-scrollbar-corner {
                background: none;
            }

            ::-webkit-scrollbar-thumb {
                background: color-mix(in srgb, currentcolor 20%, transparent);
                border-radius: 4px;
            }

            ::-webkit-scrollbar {
                background: none;
            }

            .scroll-horizontal {
                overflow: auto hidden;
            }

            .scroll-horizontal > * {
                max-width: unset;
            }

            .scroll-vertical {
                overflow: hidden auto;
            }

            .scroll-vertical > * {
                max-height: unset;
            }

            iframe#webpack-dev-server-client-overlay {
                display: none !important;
            }

            .kiteui-separator {
                background-color: currentcolor;
                opacity: 0.25;
                min-width: 1px;
                min-height: 1px;
            }

            ::placeholder {
                color: currentcolor;
                opacity: 0.3;
            }

            input {
                min-height: 1.5rem;
                min-width: 1.5rem;
            }

            * {
                scrollbar-color: rgb(153, 153, 153) rgba(0, 0, 0, 0);
                scrollbar-width: thin;
                scrollbar-gutter: auto;
                flex-shrink: 0;
                max-width: calc(100%);
                max-height: calc(100%);
                min-height: 0px;
                min-width: 0px;
                padding: 0px;
            }

            .kiteui-label.kiteui-label {
                display: flex;
                flex-direction: column;
                align-items: stretch;
            }

            .crowd {
                padding: 0px !important;
            }

            :checked.radio::after {
                opacity: 1;
                transform: none;
            }

            .radio::after {
                position: absolute;
                border-radius: 999px;
                content: "";
                display: block;
                width: 1rem;
                height: 1rem;
                top: 0.15rem;
                left: 0.15rem;
                background-color: currentcolor;
                opacity: 0.4;
                transform: scale(0);
                transition-property: opacity, transform;
                transition-timing-function: linear;
            }

            .radio {
                appearance: none;
                width: 1.5rem;
                height: 1.5rem;
                position: relative;
                border-radius: 999px !important;
                padding: 0px !important;
                border-width: 0.1rem;
                border-style: solid;
            }

            :checked.checkbox::after {
                opacity: 1;
                transform: rotate(-45deg);
            }

            .checkbox::after {
                position: absolute;
                content: "";
                display: block;
                width: 0.8rem;
                height: 0.3rem;
                top: 0.3rem;
                left: 0.16rem;
                border-left-color: currentcolor;
                border-bottom: 0.2rem solid;
                border-left-style: solid;
                border-left-width: 0.2rem;
                opacity: 0.4;
                transform: rotate(-45deg) scale(0);
                transition-property: opacity, transform;
                transition-timing-function: linear;
            }

            .checkbox:checked {
                opacity: 1;
            }

            .checkbox {
                appearance: none;
                width: 1.5rem;
                height: 1.5rem;
                position: relative;
                padding: 0px !important;
                border-width: 0.1rem;
                border-style: solid;
                opacity: 0.75;
            }

            .switch:checked::before {
                left: calc(1.4rem);
            }

            .switch::before {
                position: absolute;
                content: "";
                left: calc(-0.1rem);
                top: calc(-0.1rem);
                display: block;
                height: 1.6rem;
                width: 1.6rem;
                max-width: unset;
                max-height: unset;
                cursor: pointer;
                border: 1px solid rgba(100, 116, 139, 0.527);
                border-radius: 9999px !important;
                background-color: rgb(255, 255, 255);
                box-shadow: rgba(100, 116, 139, 0.327) 0px 3px 10px;
                transition: 0.3s;
            }

            .switch:not(:checked) {
                background-color: rgb(204, 204, 204) !important;
                background-image: none !important;
            }

            .switch {
                position: relative;
                overflow: visible;
                padding: 0px !important;
                height: 1.5rem;
                width: 3rem;
                cursor: pointer;
                appearance: none;
                border-radius: 9999px !important;
                background-color: rgba(100, 116, 139, 0.377);
                transition: 0.3s;
            }

            .clickable {
                cursor: pointer;
            }

            button.loading > * {
                opacity: 0.15;
            }

            button {
                position: relative;
            }

            button.loading::after {
                opacity: 0.5 !important;
                content: "";
                pointer-events: none;
                position: absolute;
                top: calc(50% - 15px);
                left: calc(50% - 15px);
                width: 20px !important;
                height: 20px !important;
                background: none !important;
                box-shadow: none !important;
                border-style: solid !important;
                border-color: currentcolor currentcolor currentcolor transparent !important;
                border-width: 6px !important;
                border-radius: 50% !important;
                transition: 0.3s;
                animation: 2s linear infinite spin !important;
            }

            p.loading:not(.inclBack), h1.loading:not(.inclBack), h2.loading:not(.inclBack), h3.loading:not(.inclBack), h4.loading:not(.inclBack), h5.loading:not(.inclBack), h6.loading:not(.inclBack), img.loading:not(.inclBack), input.loading:not(.inclBack), select.loading:not(.inclBack), textarea.loading:not(.inclBack) {
                min-height: 1.4em;
                background: color-mix(in srgb, currentcolor 30%, transparent) !important;
                animation: 2s infinite flickerAnimation !important;
            }

            img {
                overflow: hidden;
            }
            
            .optColChild {
                display: block;
            }
            .optColChild:last-child {
                margin-bottom: 0px !important;
            }
            .weakFill {
                width: 100%;
                height: 100%;
            }

            .spinner {
                display: block;
                width: 32px !important;
                height: 32px !important;
                opacity: 0.5 !important;
                background: none !important;
                box-shadow: none !important;
                border-style: solid !important;
                border-color: currentcolor currentcolor currentcolor transparent !important;
                border-width: 5px !important;
                border-radius: 50% !important;
                animation: 2s linear infinite spin !important;
            }

            input:focus:not(.transition), select:focus:not(.transition), textarea:focus:not(.transition) {
                outline: none;
            }

            button, input, textarea, select {
                background: none;
                border-width: 0px;
                outline-width: 0px;
                font: unset;
                color: unset;
                text-align: start;
            }

            a:visited {
                color: unset;
            }

            a {
                text-decoration: none;
                color: unset;
            }

            body > div {
                height: 100%;
                max-width: 100vw;
            }

            body {
                height: 100svh;
                max-height: 100svh;
                max-width: 100vw;
                overflow: hidden;
            }

            .noInteraction.noInteraction {
                pointer-events: none;
            }
            .noInteraction > * {
                pointer-events: auto;
            }

            video.scaleType-NoScale {
                object-fit: none;
            }

            video.scaleType-Stretch {
                object-fit: fill;
            }

            video.scaleType-Crop {
                object-fit: cover;
            }

            video.scaleType-Fit {
                object-fit: contain;
            }

            .swapImage.scaleType-NoScale > img {
                object-fit: none;
            }

            .swapImage.scaleType-Stretch > img {
                object-fit: fill;
            }

            .swapImage.scaleType-Crop > img {
                object-fit: cover;
            }

            .swapImage.scaleType-Fit > img {
                object-fit: contain;
            }

            .swapImage > img {
                object-fit: contain;
                transition-duration: var(--transition-duration, 0.25s);
            }

            .swapImage, .icon {
                overflow: hidden;
                line-height: 0px;
                display: block;
            }
            .icon {
                border-radius: 0px !important;
            }

            .kiteui-space {
                display: inline-block;
                min-width: calc(var(--space-multiplier, 1.0) * var(--spacing, 0px));
            }

            :hover.visibleOnParentHover {
                visibility: visible;
            }

            :hover > .visibleOnParentHover {
                visibility: visible;
            }

            .visibleOnParentHover {
                visibility: hidden;
                width: auto;
                height: auto;
                max-width: unset;
                max-height: unset;
            }

            h1, h2, h3, h4, h5, h6, p, .subtext {
            }

            * {
                box-sizing: border-box;
                line-height: unset;
                --parentPadding: 0px;
            }
            }
        """.trimIndent()
        )
        try {
            dynamicCss.rule(
                """progress::-webkit-progress-value {
                    height: 100%;
                    background-color: currentColor;
                    border-radius: 1rem;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """progress::-webkit-progress-bar {
                    border-radius: 100px;
                    background: var(--nearest-background-color);
                    padding: 1px;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """progress::-moz-progress-bar {
                    height: 100%;
                    background-color: currentColor;
                    border-radius: 1rem;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """input::-webkit-outer-spin-button, input::-webkit-inner-spin-button {
                    -webkit-appearance: none;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """input[type=number] {
                    -moz-appearance: textfield
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
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

    private var cssGenTotal: Duration = 0.seconds
    private val themeInteractiveHandled = HashSet<String>()
    fun themeInteractive(theme: Theme): List<String> {
        if (!themeInteractiveHandled.add(theme.id)) return theme.classes
        measureTime {
            theme.derivedFrom?.let { themeInteractive(it) }
            theme(theme)
            val cs = theme.classSelector
            fun sub(subthemeGen: Semantic?, asSelectors: List<String>) {
                val subtheme = subthemeGen?.let { s -> theme[s] } ?: theme.withoutBack
                if (theme != subtheme.theme) {
                    theme(
                        subtheme.theme,
                        diff = theme,
                        asSelectors = asSelectors.flatMap { listOf("$it $cs", "$it$cs") },
                        includeMaybeTransition = subtheme.useBackground
                    )
                }
                val hov = subtheme[HoverSemantic]
                theme(
                    hov.theme,
                    diff = theme,
                    asSelectors = asSelectors.flatMap {
                        listOf(
                            ".clickable:hover$it $cs",
                            ".clickable:hover$it$cs",
                        )
                    },
                    includeMaybeTransition = hov.useBackground
                )
                val foc = subtheme[FocusSemantic]
                theme(
                    foc.theme,
                    diff = theme,
                    asSelectors = asSelectors.flatMap {
                        listOf(
                            ".clickable:focus-visible$it $cs",
                            ".clickable:focus-visible$it$cs",
                            "input:focus$it$cs",
                            "textarea:focus$it$cs",
                            "select:focus$it$cs",
                            ".hasNoBackField:focus-within$it$cs",
                        )
                    },
                    includeMaybeTransition = foc.useBackground
                )
                val dwn = subtheme[DownSemantic]
                theme(
                    dwn.theme,
                    diff = theme,
                    asSelectors = asSelectors.flatMap {
                        listOf(
                            ".clickable:active$it $cs",
                            ".clickable:active$it$cs",
                        )
                    },
                    includeMaybeTransition = dwn.useBackground
                )
                val dis = subtheme[DisabledSemantic]
                theme(
                    dis.theme,
                    diff = theme,
                    asSelectors = asSelectors.flatMap {
                        listOf(
                            ".clickable:disabled$it $cs",
                            ".clickable:disabled$it$cs",
                        )
                    },
                    includeMaybeTransition = dis.useBackground
                )
            }
            sub(null, asSelectors = listOf(""))
            sub(
                SelectedSemantic,
                asSelectors = listOf(".checked.checkResponsive"),
            )
            sub(
                UnselectedSemantic,
                asSelectors = listOf(".checkResponsive"),
            )
        }.also {
            cssGenTotal += it
        }
        dynamicCss.flush()
        return theme.classes
    }

    private val Theme.classes: List<String>
        get() = buildList<String> {
            add("t-${id}")
//            generateSequence(this@classes) { it.derivedFrom }
//                .toList()
//                .reversed()
//                .map { t -> (t.derivationId ?: t.id) }
//                .forEachIndexed { index, t ->
//                    add("t-$index$t")
//                }
        }
    private val Theme.classSelector get() = classes.joinToString("") { ".$it" }
    private inline fun <T> Theme.diff(diff: Theme? = null, getter: Theme.() -> T): T? =
        getter().takeUnless { diff?.getter() == it }

    fun theme(
        theme: Theme,
        diff: Theme? = null,
        asSelectors: List<String> = listOf(theme.classSelector),
        includeMaybeTransition: Boolean = false,
    ): List<String> {
        val classes = theme.classes

        fun sel(vararg plus: String): String {
            return asSelectors.asSequence().flatMap { plus.asSequence().map { p -> "$it$p" } }.joinToString(", ")
        }

        val directSel = sel("")

        val backSel = (if (includeMaybeTransition) sel(".mightTransition") else sel(".transition"))
        theme.diff(diff) { background }?.let {
            when (it) {
                is Color -> {
                    dynamicCss.add(backSel, "background-color", it.toCss())
                    dynamicCss.add(backSel, "background-image", "none")
                }

                is LinearGradient -> {
                    dynamicCss.add(backSel, "background-color", it.closestColor().toCss())
                    dynamicCss.add(
                        backSel, "background-image", "linear-gradient(${it.angle.plus(Angle.quarterTurn).turns}turn, ${
                            joinGradientStops(it.stops)
                        })"
                    )
                    dynamicCss.add(backSel, "background-attachment", (if (it.screenStatic) "fixed" else "unset"))
                }

                is RadialGradient -> {
                    dynamicCss.add(backSel, "background-color", it.closestColor().toCss())
                    dynamicCss.add(
                        backSel, "background-image", "radial-gradient(circle at center, ${
                            joinGradientStops(it.stops)
                        })"
                    )
                    dynamicCss.add(backSel, "background-attachment", (if (it.screenStatic) "fixed" else "unset"))
                }
            }
        }

        theme.diff(diff) { outlineWidth }?.let {
            dynamicCss.add(backSel, "outline-width", it.value)
            dynamicCss.add(backSel, "outline-style", if (it != 0.px) "solid" else "none")
            dynamicCss.add(backSel, "outline-offset", it.times(-1).value)
        }
        theme.diff(diff) { elevation }?.let {
            dynamicCss.add(backSel, "box-shadow", theme.elevation.toBoxShadow())
        }

        theme.diff(diff) { foreground }?.let { dynamicCss.add(directSel, "color", it.toCss()) }
        theme.diff(diff) { icon }?.let { dynamicCss.add(directSel, "--icon-color", it.toCss()) }
        theme.diff(diff) { spacing }?.let { dynamicCss.add(directSel, "--spacing", it.value) }
        theme.diff(diff) { navSpacing }?.let { dynamicCss.add(directSel, "--navSpacing", it.value) }
        theme.diff(diff) { font.size }?.let { dynamicCss.add(directSel, "font-size", it.value) }
        theme.diff(diff) { font.font }?.let { dynamicCss.add(directSel, "font-family", it.let { dynamicCss.font(it) }) }
        theme.diff(diff) { font.weight }?.let { dynamicCss.add(directSel, "font-weight", it.toString()) }
        theme.diff(diff) { font.italic }
            ?.let { dynamicCss.add(directSel, "font-style", it.let { if (it) "italic" else "normal" }) }
        theme.diff(diff) { font.allCaps }
            ?.let { dynamicCss.add(directSel, "text-transform", it.let { if (it) "uppercase" else "none" }) }
        theme.diff(diff) { font.lineSpacingMultiplier }?.let { dynamicCss.add(directSel, "line-height", it.toString()) }
        theme.diff(diff) { font.additionalLetterSpacing }
            ?.let { dynamicCss.add(directSel, "letter-spacing", it.toString()) }
        theme.diff(diff) { outline }?.let { dynamicCss.add(directSel, "outline-color", it.toCss()) }
        theme.diff(diff) { transitionDuration }?.let { dynamicCss.add(directSel, "transition-duration", it.toCss()) }
        theme.diff(diff) { transitionDuration }?.let { dynamicCss.add(directSel, "--transition-duration", it.toCss()) }
        theme.diff(diff) { background }
            ?.let { dynamicCss.add(directSel, "--nearest-background-color", it.closestColor().toCss()) }
        theme.diff(diff) { cornerRadii }?.let { dynamicCss.add(directSel, "border-radius", it.toRawCornerRadius()) }
        theme.diff(diff) { foreground }?.let {
            when (it) {
                is Color -> {
                    dynamicCss.add(directSel, "color", it.toCss())
                }

                is LinearGradient, is RadialGradient -> {
                    dynamicCss.add(directSel, "color", it.toCss())
                    dynamicCss.add(directSel, "background", "-webkit-${it.toCss()}")
                    dynamicCss.add(directSel, "-webkit-background-clip", "text")
                    dynamicCss.add(directSel, "-webkit-text-fill-color", "transparent")
                }
            }
        }

        return classes
    }

    val rowCollapsingToColumnHandled = HashSet<String>()
    fun rowCollapsingToColumn(breakpoints: List<Dimension>): String {
        val name = "rowCollapsingToColumn_${breakpoints.joinToString("_") { it.value.filter { it.isLetterOrDigit() } }}"
        if (rowCollapsingToColumnHandled.add(name)) {
            dynamicCss.rule(
                """
                .$name.rowCollapsing { display: flex }
            """
            )
            (-1..breakpoints.size-1).forEach { index ->
                val mediaQuery = listOfNotNull(
                    breakpoints.getOrNull(index)?.let {
                        "(min-width: ${it.value})"
                    },
                    breakpoints.getOrNull(index + 1)?.let {
                        "(max-width: ${it.value})"
                    },
                ).joinToString(" and ")
                if(index.rem(2).absoluteValue == 1) {
                    dynamicCss.rule(
                        """
                    @media $mediaQuery {
                        .$name.rowCollapsing {
                            flex-direction: column;
                        }
                        .$name.rowCollapsing > * {
                            display: block;
                            flex-grow: 0 !important;
                            flex-shrink: 0 !important;
                            flex-basis: auto !important;
                        }
                        .$name.rowCollapsing > .hStart {
                            align-self: start;
                        }
                        .$name.rowCollapsing > .hCenter {
                            align-self: center;
                        }
                        .$name.rowCollapsing > .hStretch {
                            align-self: stretch;
                        }
                        .$name.rowCollapsing > .hEnd {
                            align-self: end;
                        }
                    }
                """.trimIndent()
                    )
                } else {
                    dynamicCss.rule(
                        """
                    @media $mediaQuery {
                        .$name.rowCollapsing {
                            flex-direction: row;
                        }
                        .$name.rowCollapsing > * {
                            display: block;
                        }
                        .$name.rowCollapsing > .vStart {
                            align-self: start;
                        }
                        .$name.rowCollapsing > .vCenter {
                            align-self: center;
                        }
                        .$name.rowCollapsing > .vStretch {
                            align-self: stretch;
                        }
                        .$name.rowCollapsing > .vEnd {
                            align-self: end;
                        }
                    }
                """.trimIndent()
                    )
                }
            }
        }
        return name
    }

    inline fun apply(theme: Theme, out: (prop: String, value: String) -> Unit) {

    }
}
