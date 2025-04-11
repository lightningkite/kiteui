package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class KiteUiCss(val dynamicCss: DynamicCss) {
    init {
        // basis rules
        //language=CSS
        @Suppress("CssUnresolvedCustomProperty")
        dynamicCss.rule("""
            @media print {
                .do-not-print{
                    display: none !important;
                }
                .scroll-vertical {
                    overflow: hidden auto;
                }
                body > div {
                    height: unset;
                    max-width: 100vw;
                }
    
                body {
                    height: unset;
                    max-height: unset;
                    max-width: 100vw;
                    overflow: visible;
                }
                .kiteui-col > * {
                    flex-grow: 0 !important;
                    flex-shrink: 0 !important;
                    flex-basis: unset !important;
                }
            }
        """.trimIndent())
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
            
            p, h1, h2, h3, h4, h5, h6, .subtext {
                white-space: pre-wrap;
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

            .icon {
                color: var(--icon-color, black);
            }

            .padded:not(.swapImage) {
                padding: var(--padding, 0px);
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
                background: none;
                max-height: 0.25rem !important;
                border: medium;
                border-radius: 1rem;
                padding: 0px !important;
                appearance: none;
            }
            
            
            
           .progress-ring {
              width: 100%;
              justify-content: space-around;
            }
            
            .progress-ring-svg {
                display: block;
                margin: 10px auto;
            }

            .circle-background {
                          fill: none;
                          stroke: --background-color;
                          stroke-width: 3.8;
            }
            
            .circle-progress {
                fill: none;
                stroke-width:2.8;
                stroke-linecap: round;
                animation: progress 1s ease-out forwards;
                  stroke: currentcolor;
            }
         

            .progress-ring-content {
                text-anchor: middle;
                
            }
            
            .progress-ring-content {
              fill: #666;
              font-family: sans-serif;
              font-size: 0.5em;
              text-anchor: middle;
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

            .hideScrollbar {
                scrollbar-width: none;
            }
            .hideScrollbar::-webkit-scrollbar {
                display: none;
            }

            .suppress-overflow-anchors > * {
                overflow-anchor: none;
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
                transition-property: color, background-image, background-color, border-color, outline-color, outline-width, box-shadow, border-radius, opacity, backdrop-filter;
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
                background-color: var(--separator-color, currentcolor);
                min-width: 1px;
                min-height: 1px;
            }

            ::placeholder {
                color: currentcolor;
                opacity: 0.3;
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
                width: 50%;
                height: 50%;
                top: 25%;
                left: 25%;
                background-color: currentcolor;
                opacity: 0.4;
                transform: scale(0);
                transition-property: opacity, transform;
                transition-timing-function: linear;
                transition-duration: var(--transition-duration, 0.25s);
            }

            .radio.radio.radio {
                appearance: none;
                width: 25px;
                height: 25px;
                position: relative;
                border-radius: 999px !important;
                padding: 0px !important;
                border-width: 0.1rem;
                border-style: solid;
                outline: none;
                border-color: var(--icon-color, currentcolor);
            }

            :checked.checkbox::after {
                opacity: 1;
                transform: rotate(-45deg);
            }

            .checkbox::after {
                position: absolute;
                content: "";
                display: block;
                width: 50%;
                height: 20%;
                top: 25%;
                left: 20%;
                border-color: currentcolor;
                border-left-style: solid;
                border-left-width: 0.2rem;
                border-bottom-width: 0.2rem;
                border-bottom-style: solid;
                opacity: 0.4;
                transform: rotate(-45deg) scale(0);
                transition-property: opacity, transform;
                transition-timing-function: linear;
                transition-duration: var(--transition-duration, 0.25s);
            }

            .checkbox:checked {
                opacity: 1;
            }

            .checkbox.checkbox.checkbox {
                appearance: none;
                width: 25px;
                height: 25px;
                position: relative;
                padding: 0px !important;
                border-width: 0.1rem;
                border-style: solid;
                border-color: var(--icon-color, currentcolor);
                border-radius: 20%;
                outline: none;
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

            .switch:checked {
                background-color: #20a020 !important;
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
                background-color: color-mix(in srgb, currentcolor 20%, transparent) !important;
                background-image: none !important;
                transition: 0.3s;
            }

            .clickable {
                cursor: pointer;
            }

            button {
                position: relative;
            }

            button.working::after {
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
                opacity: 0.5;
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
                display: block;
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

            img.scaleType-NoScale {
                object-fit: none;
            }

            img.scaleType-Stretch {
                object-fit: fill;
            }

            img.scaleType-Crop {
                object-fit: cover;
            }

            img.scaleType-Fit {
                object-fit: contain;
            }

            .swapImage > img {
                object-fit: contain;
                transition-duration: var(--transition-duration, 0.25s);
              position: absolute;
                width: 100%;
              height: 100%;
            }
            .swapImage {
              position: relative;
            }

            .swapImage.loading.useLoading::before {
                display: block;
                content: " ";
                width: 32px !important;
                height: 32px !important;
                position: absolute;
                left: calc(50% - 32px);
                top: calc(50% - 32px);
                opacity: 0.5 !important;
                background: none !important;
                box-shadow: none !important;
                border-style: solid !important;
                border-color: currentcolor currentcolor currentcolor transparent !important;
                border-width: 5px !important;
                border-radius: 50% !important;
                animation: 2s linear infinite spin !important;
            }

            .swapImage, .icon {
                overflow: hidden;
                line-height: 0px;
                display: block;
            }
            .icon {
                border-radius: 0px !important;
            }
            
            .textarea-container {
              /* easy way to plop the elements on top of each other and have them both sized based on the tallest one's height */
              display: grid;
            }
            .textarea-container::after {
              /* Note the weird space! Needed to preventy jumpy behavior */
              content: attr(data-replicated-value) " ";

              /* This is how textarea text behaves */
              white-space: pre-wrap;

              /* Hidden from view, clicks, and screen readers */
              visibility: hidden;
              
              padding: 2px;
            }
            .textarea-container > textarea {
              /* You could leave this, but after a user resizes, then it ruins the auto sizing */
              resize: none;
            }
            .textarea-container > textarea,
            .textarea-container::after {
              /* Identical styling required!! */
              font: inherit;

              /* Place on top of each other */
              grid-area: 1 / 1 / 2 / 2;
            }

            .kiteui-space {
                display: inline-block;
                min-height: calc(var(--space-multiplier, 1.0) * var(--spacing, 0px));
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
        for(h in Align.entries + listOf(null))
            for(v in Align.entries + listOf(null))
                dynamicCss.rule("""
                    .snapTo-$h-$v > :not(:first-child) {
                        scroll-snap-align: ${listOfNotNull(h, v).joinToString(" "){ it.name.lowercase() }}
                    }
                """.trimIndent())
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
    fun themeInteractive(theme: Theme): String {
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
                        includeMaybeTransition = subtheme.drawBackground
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
                    includeMaybeTransition = hov.drawBackground,
                    mediaQuery = "(hover : hover)"
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
                    includeMaybeTransition = foc.drawBackground
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
                    includeMaybeTransition = dwn.drawBackground
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
                    includeMaybeTransition = dis.drawBackground,
                )
                val print = subtheme[PrintSemantic]
                theme(
                    print.theme,
                    diff = theme,
                    asSelectors = asSelectors.map { "$it$cs$cs" },
                    includeMaybeTransition = print.drawBackground,
                    mediaQuery = "print"
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

    private val Theme.classes: String
        get() = "t-${id}"
    private val Theme.classSelector get() = ".$classes"
    private inline fun <T> Theme.diff(diff: Theme? = null, getter: Theme.() -> T): T? =
        getter().takeUnless { diff?.getter() == it }

    fun theme(
        theme: Theme,
        diff: Theme? = null,
        asSelectors: List<String> = listOf(theme.classSelector),
        includeMaybeTransition: Boolean = false,
        mediaQuery: String = "",
    ): String {
        val classes = theme.classes

        fun sel(vararg plus: String): String {
            return asSelectors.asSequence().flatMap { plus.asSequence().map { p -> "$it$p" } }.joinToString(", ")
        }

        fun addToCss(selector: String, key: String, value: String) {
            dynamicCss.add(selector, key, value, mediaQuery)
        }

        val directSel = sel("")

        val backSel = (if (includeMaybeTransition) sel(".clickable") else sel(".transition"))

        theme.diff(diff) { background }?.let {
            if(diff?.background is FadingColor) addToCss(backSel, "animation", "none")
            when (it) {
                is Color -> {
                    addToCss(backSel, "background-color", it.toWeb())
                    addToCss(backSel, "background-image", "none")
                }
                is FadingColor -> {
                    dynamicCss.rule("""
                        @keyframes ${theme.id}-flickerAnimation {
                        0% {
                            background-color: ${it.base.toWeb()};
                        }
                        50% {
                            background-color: ${it.alternate.toWeb()};
                        }
                        100% {
                            background-color: ${it.base.toWeb()};
                        }
                    }
                    """.trimIndent())
                    addToCss(backSel, "animation", "2s infinite ${theme.id}-flickerAnimation")
                    addToCss(backSel, "background-color", it.base.toWeb())
                    addToCss(backSel, "background-image", "none")
                }

                is LinearGradient -> {
                    addToCss(backSel, "background-color", it.closestColor().toWeb())
                    addToCss(
                        backSel, "background-image", "linear-gradient(${it.angle.plus(Angle.quarterTurn).turns}turn, ${
                            joinGradientStops(it.stops)
                        })"
                    )
                    addToCss(backSel, "background-attachment", (if (it.screenStatic) "fixed" else "unset"))
                }

                is RadialGradient -> {
                    addToCss(backSel, "background-color", it.closestColor().toWeb())
                    addToCss(
                        backSel, "background-image", "radial-gradient(circle at center, ${
                            joinGradientStops(it.stops)
                        })"
                    )
                    addToCss(backSel, "background-attachment", (if (it.screenStatic) "fixed" else "unset"))
                }
            }
        }

        theme.diff(diff) { outlineWidth }?.let {
            addToCss(backSel, "outline-width", it.value)
            addToCss(backSel, "outline-style", if (it != 0.px) "solid" else "none")
            addToCss(backSel, "outline-offset", it.times(-1).value)
        }
        theme.diff(diff) { elevation }?.let {
            addToCss(backSel, "box-shadow", theme.elevation.toBoxShadow())
        }

        theme.diff(diff) { gap }?.let { addToCss(directSel, "--spacing", it.value) }
        theme.diff(diff) { padding }?.let { addToCss(directSel, "--padding", it.css()) }
        theme.diff(diff) { font.size }?.let { addToCss(directSel, "font-size", it.value) }
        theme.diff(diff) { font.font }?.let { addToCss(directSel, "font-family", it.let { dynamicCss.font(it) }) }
        theme.diff(diff) { font.weight }?.let { addToCss(directSel, "font-weight", it.toString()) }
        theme.diff(diff) { font.italic }
            ?.let { addToCss(directSel, "font-style", it.let { if (it) "italic" else "normal" }) }
        theme.diff(diff) { font.allCaps }
            ?.let { addToCss(directSel, "text-transform", it.let { if (it) "uppercase" else "none" }) }
        theme.diff(diff) { font.lineSpacingMultiplier }
            ?.let { addToCss(directSel, "line-height", it.toString()) }
        theme.diff(diff) { font.additionalLetterSpacing }
            ?.let { addToCss(directSel, "letter-spacing", it.value) }
        theme.diff(diff) {
            when {
                font.strikethrough && font.underline -> "underline line-through"
                font.strikethrough -> "line-through"
                font.underline -> "underline"
                else -> "none"
            }
        }
            ?.let { addToCss(directSel, "text-decoration-line", it) }
        theme.diff(diff) { font.lineSpacingMultiplier }?.let { addToCss(directSel, "line-height", it.toString()) }
        theme.diff(diff) { font.additionalLetterSpacing }
            ?.let { addToCss(directSel, "letter-spacing", it.toString()) }
        theme.diff(diff) { outline }?.let { addToCss(directSel, "outline-color", it.closestColor().toWeb()) }
        theme.diff(diff) { transitionDuration }?.let { addToCss(directSel, "transition-duration", it.toCss()) }
        theme.diff(diff) { transitionDuration }?.let { addToCss(directSel, "--transition-duration", it.toCss()) }
        theme.diff(diff) { background }
            ?.let { addToCss(directSel, "--nearest-background-color", it.closestColor().toWeb()) }
        theme.diff(diff) { cornerRadii }?.let { addToCss(backSel, "border-radius", it.toRawCornerRadius()) }
        theme.diff(diff) { foreground }?.let {
            addToCss(directSel, "color-scheme", if(it.closestColor().perceivedBrightness > 0.5) "dark" else "light")
            when (it) {
                is Color -> addToCss(directSel, "color", it.toWeb())
                is FadingColor -> addToCss(directSel, "color", it.base.toWeb())
                is LinearGradient -> {
                    addToCss(directSel, "color", "linear-gradient(${it.angle.plus(Angle.quarterTurn).turns}turn, ${joinGradientStops(it.stops)})")
                    addToCss(directSel, "background", "-webkit-linear-gradient(${it.angle.plus(Angle.quarterTurn).turns}turn, ${joinGradientStops(it.stops)})")
                    addToCss(directSel, "-webkit-background-clip", "text")
                    addToCss(directSel, "-webkit-text-fill-color", "transparent")
                }
                is RadialGradient -> {
                    addToCss(directSel, "color", "radial-gradient(circle at center, ${joinGradientStops(it.stops)})")
                    addToCss(directSel, "background", "-webkit-radial-gradient(circle at center, ${joinGradientStops(it.stops)})")
                    addToCss(directSel, "-webkit-background-clip", "text")
                    addToCss(directSel, "-webkit-text-fill-color", "transparent")
                }
            }
        }
        theme.diff(diff) { icon }?.let {
            when (it) {
                is Color -> addToCss(directSel, "--icon-color", it.toWeb())
                is FadingColor -> addToCss(directSel, "--icon-color", "")
                else -> addToCss(directSel, "--icon-color", it.closestColor().toWeb())
            }
        }
        theme.diff(diff) { separator }?.let {
            when (it) {
                is Color -> addToCss(directSel, "--separator-color", it.toWeb())
                is FadingColor -> addToCss(directSel, "--separator-color", "")
                else -> addToCss(directSel, "--separator-color", it.closestColor().toWeb())
            }
        }

        return classes
    }

    fun Edges.css() = "${top.value} ${right.value} ${bottom.value} ${left.value}"

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
                if(index.plus(2).rem(2) == 1) {
                    dynamicCss.rule(
                        """
                    @media $mediaQuery {
                        .$name.rowCollapsing {
                            flex-direction: column;
                        }
                        .$name.rowCollapsing > * {
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
