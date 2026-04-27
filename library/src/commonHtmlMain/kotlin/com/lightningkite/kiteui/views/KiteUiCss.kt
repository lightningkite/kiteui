package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class KiteUiCss(val dynamicCss: DynamicCss) {
    init {
        // basis rules
        //language=CSS
        try {
            @Suppress("CssUnresolvedCustomProperty")
            dynamicCss.rule(
                """
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
        """.trimIndent()
            )
        } catch(e: Exception) {
            Exception("Failed to add print ruleset", e).printStackTrace()
        }
        @Suppress("CssUnresolvedCustomProperty")
        dynamicCss.rule(
            """
            /*noinspection ALL*/@media {
            .kui {
                margin: 0;
                padding: 0;
                border: 0;
                font-size: 100%;
                font: inherit;
                vertical-align: baseline;
            }
            
            p.kui, h1.kui, h2.kui, h3.kui, h4.kui, h5.kui, h6.kui, .subtext {
                white-space: pre-wrap;
            }
            
            p.kui li.kui {
                margin-inline-start: 1em;
            }
            p.kui a.kui {
                text-decoration: revert;
                color: revert;
            }
            p.kui a.kui:visited {
                color: revert;
            }
    
            /* HTML5 display-role reset for older browsers */
            article.kui, aside.kui, details.kui, figcaption.kui, figure.kui, footer.kui, header.kui, hgroup.kui, menu.kui, nav.kui, section.kui {
                display: block;
            }
    
            body {
                line-height: 1;
            }
    
            ol.kui, ul.kui {
                list-style: none;
            }
    
            blockquote.kui, q.kui {
                quotes: none;
            }
    
            blockquote.kui:before, blockquote.kui:after, q.kui:before, q.kui:after {
                content: '';
                content: none;
            }
    
            table.kui {
                border-collapse: collapse;
                border-spacing: 0;
            }

            .kui.icon {
                color: var(--icon-color, black);
            }

            .kui.padded:not(.swapImage) {
                padding: var(--padding, 0px);
            }

            input.kui[type="number"] {
                appearance: textfield;
            }

            input.kui::-webkit-outer-spin-button, input::-webkit-inner-spin-button {
                appearance: none;
            }

            progress.kui::-moz-progress-bar {
                height: 100%;
                background-color: currentcolor;
                border-radius: 1rem;
            }

            progress.kui::-webkit-progress-bar {
                border-radius: 100px;
                background: transparent;
            }

            progress.kui::-webkit-progress-value {
                height: 100%;
                background-color: currentcolor;
                border-radius: 1rem;
            }

            progress.kui {
                background: none;
                height: 1rem;
                max-height: 1rem;
                width: 100%;
                border: medium;
                border-radius: 1rem;
                padding: 0px !important;
                appearance: none;
                overflow: visible !important;
                background-color: var(--nearest-background-color, transparent);
            }

            input.kui[type="range"].neumorphic-slider {
                -webkit-appearance: none;
                appearance: none;
                width: 100%;
                height: 1rem;
                max-height: 1rem;
                border: none;
                border-radius: 1rem;
                padding: 0px !important;
                overflow: visible !important;
                outline: none;
                background-color: var(--nearest-background-color, transparent);
            }

           .kui.progress-ring {
              width: 100%;
              justify-content: space-around;
            }
            
            .kui.progress-ring-svg {
                display: block;
                margin: 10px auto;
            }

            .circle-progress-background {
                          fill: none;
                          stroke: var(--nearest-background-color); !important;
                          stroke-width: 3;
            }
            
            .circle-progress {
                fill: none;
                stroke-width:2.8;
                stroke-linecap: round;
                animation: progress 1s ease-out forwards;
                  stroke: currentcolor;
            }

            @media (pointer: coarse) and (hover: none) {
                .touchscreenOnly {
                    visibility: visible;
                }
            }
            
            * {
                gap: var(--spacing, 0);
            }       

            .kui.touchscreenOnly {
            }
            .kui.disableContextMenu {
                -webkit-user-select: none !important;  
                -webkit-touch-callout: none !important;  
            }

            .kui.viewPager > :not(.recyclerViewCap) {
                width: var(--pager-width, 0rem);
                height: var(--pager-height, 0rem);
                scroll-snap-align: center;
                scroll-snap-stop: always;
            }

            .kui.viewPager {
                scroll-snap-type: x mandatory;
            }

            .kui.hideScrollbar {
                scrollbar-width: none;
            }
            .kui.hideScrollbar::-webkit-scrollbar {
                display: none;
            }

            .kui.suppress-overflow-anchors > * {
                overflow-anchor: none;
            }

            .kui.notransition, .kui.notransition * {
                transition: none !important;
            }

            .kui.animatingShowHide {
                overflow: hidden;
            }

            .kui {
                box-sizing: border-box;
                line-height: unset;
                --parentPadding: 0px;
                scrollbar-color: rgb(153, 153, 153) rgba(0, 0, 0, 0);
                scrollbar-width: thin;
                scrollbar-gutter: auto;
                flex-shrink: 0;
                max-width: calc(100%);
                max-height: calc(100%);
                min-height: 0px;
                min-width: 0px;
                padding: 0px;
                transition-timing-function: linear;
                transition-delay: 0s;
                transition-property: color, background-image, background-color, border-color, outline-color, outline-width, box-shadow, border-radius, opacity, backdrop-filter, transform;
            }
            
            .kui.transition {
                overflow: clip;
                overflow-clip-margin: 32px;
            }

            .kui[hidden] {
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

            .kui::-webkit-scrollbar-corner {
                background: none;
            }

            .kui::-webkit-scrollbar-thumb {
                background: color-mix(in srgb, currentcolor 20%, transparent);
                border-radius: 4px;
            }

            .kui::-webkit-scrollbar {
                background: none;
            }

            .kui.scroll-horizontal {
                overflow: auto clip;
                overflow-clip-margin: 32px;
                padding-top: var(--shadow-room, 0px);
                padding-bottom: var(--shadow-room, 0px);
                margin-top: calc(-1 * var(--shadow-room, 0px));
                margin-bottom: calc(-1 * var(--shadow-room, 0px));
            }

            .kui.scroll-horizontal:has(.outer-shadow) {
                padding-top: var(--shadow-room, 32px);
                padding-bottom: var(--shadow-room, 32px);
            }

            .kui.scroll-horizontal  * {
                max-width: unset;
            }
            .kui.scroll-horizontal * {
                max-width: 100;
            }

            .kui.scroll-vertical {
                overflow: clip auto;
                overflow-clip-margin: 32px;
            }

            .kui.scroll-vertical:has(.outer-shadow) {
                padding-left: var(--shadow-room, 32px);
                padding-right: var(--shadow-room, 32px);
            }

            .kui.scroll-vertical  * {
                max-height: unset;
            }
            .kui.has-set-height * {
                max-height: 100%;
            }

            iframe#webpack-dev-server-client-overlay {
                display: none !important;
            }

            .kui.kiteui-separator {
                background-color: var(--separator-color, currentcolor);
                min-width: 1px;
                min-height: 1px;
            }

            .kui::placeholder {
                color: currentcolor;
                opacity: 0.3;
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
                width: 1.75rem !important;
                height: 1.75rem !important;
                position: relative;
                border-radius: 999px !important;
                padding: 0px !important;
                border: none;
                outline-color: var(--icon-color, currentcolor);
                outline-offset: -0.1rem;
                background-color: var(--nearest-background-color, transparent);
                overflow: visible !important;
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
                width: 1.75rem !important;
                height: 1.75rem !important;
                position: relative;
                padding: 0px !important;
                border: none;
                outline-color: var(--icon-color, currentcolor);
                outline-offset: -0.1rem;
                border-radius: 20%;
                background-color: var(--nearest-background-color, transparent);
                overflow: visible !important;
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
                background-color: var(--nearest-background-color, rgb(255, 255, 255));
                box-shadow: rgba(100, 116, 139, 0.327) 0px 3px 10px;
                transition: 0.3s;
            }

            .switch:checked {
                background-color: #20a020;
            }

            .switch {
                position: relative;
                overflow: visible !important;
                padding: 0px !important;
                height: 1.5rem !important;
                width: 3rem !important;
                cursor: pointer;
                appearance: none;
                border-radius: 9999px !important;
                background-color: color-mix(in srgb, currentcolor 20%, transparent);
                background-image: none;
                transition: 0.3s;
            }

            .clickable {
                cursor: pointer;
            }

            button.kui {
                position: relative;
            }

            button.kui.working::after {
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
            
            .popover {
                transition-property: color, background-image, background-color, border-color, outline-color, outline-width, box-shadow, border-radius, opacity, backdrop-filter;
            }

            img.kui {
                overflow: hidden;
            }
            
            .optimized > * {
                display: block;
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

            input.kui:focus:not(.transition), select.kui:focus:not(.transition), textarea.kui:focus:not(.transition) {
                outline: none;
            }

            button.kui, input.kui, textarea.kui, select.kui {
                background: none;
                border-width: 0px;
                outline-width: 0px;
                font: unset;
                color: unset;
                text-align: start;
            }

            select.kui {
                overflow: visible !important;
            }

            a.kui:visited {
                color: unset;
            }

            a.kui {
                text-decoration: none;
                color: unset;
                display: block;
            }

            body > div.kui {
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
            
            .clickable.draggable {
                cursor: default;
            }
            .draggable {
                cursor: grab;
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
            .icon.icon > svg {
                display: block;
                max-width: 100%;
                max-height: 100%;
            }
            
            .textarea-container {
              /* easy way to plop the elements on top of each other and have them both sized based on the tallest one's height */
              display: grid;
              position: relative;
              overflow-y: scroll;
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
              position: absolute;
              left: 0;
              right: 0;
              top: 0;
              bottom: 0;
            }
            .textarea-container > textarea,
            .textarea-container::after {
              /* Identical styling required!! */
              font: inherit;

              /* Place on top of each other */
              grid-area: 1 / 1 / 2 / 2;
            }

            .kiteui-space {
                display: block;
                min-height: calc(var(--space-multiplier, 1.0) * var(--spacing, 0px));
                min-width: calc(var(--space-multiplier, 1.0) * var(--spacing, 0px));
            }

            .kui:hover.visibleOnParentHover {
                visibility: visible;
            }

            .kui:hover > .visibleOnParentHover {
                visibility: visible;
            }

            .visibleOnParentHover {
                visibility: hidden;
                width: auto;
                height: auto;
                max-width: unset;
                max-height: unset;
            }

            }
        """.trimIndent()
        )
        for(h in Align.entries + listOf(null))
            for(v in Align.entries + listOf(null))
                dynamicCss.rule("""
                    .kui.snapTo-$h-$v > :not(:first-child) {
                        scroll-snap-align: ${listOfNotNull(h, v).joinToString(" "){ it.name.lowercase() }}
                    }
                """.trimIndent())
        try {
            dynamicCss.rule(
                """progress.kui::-webkit-progress-value {
                    height: 100%;
                    background-color: currentColor;
                    border-radius: 1rem;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """progress.kui::-webkit-progress-bar {
                    border-radius: 100px;
                    background: var(--nearest-background-color);
                    padding: 1px;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """progress.kui::-moz-progress-bar {
                    height: 100%;
                    background-color: currentColor;
                    border-radius: 1rem;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """input.kui[type="range"].neumorphic-slider::-webkit-slider-runnable-track {
                    height: 1rem;
                    border-radius: 1rem;
                    background: linear-gradient(to right, currentcolor var(--slider-progress, 50%), transparent var(--slider-progress, 50%));
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """input.kui[type="range"].neumorphic-slider::-webkit-slider-thumb {
                    -webkit-appearance: none;
                    appearance: none;
                    width: 1.5rem;
                    height: 1.5rem;
                    border-radius: 50%;
                    background-color: color-mix(in srgb, var(--nearest-background-color, #e0e0e0) 90%, black);
                    box-shadow: -4px -4px 8px rgba(255,255,255,0.7), 4px 4px 8px rgba(0,0,0,0.15);
                    margin-top: -0.25rem;
                    cursor: pointer;
                    border: none;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """input.kui[type="range"].neumorphic-slider::-moz-range-track {
                    height: 1rem;
                    border-radius: 1rem;
                    background: linear-gradient(to right, currentcolor var(--slider-progress, 50%), transparent var(--slider-progress, 50%));
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """input.kui[type="range"].neumorphic-slider::-moz-range-thumb {
                    width: 1.5rem;
                    height: 1.5rem;
                    border-radius: 50%;
                    background-color: color-mix(in srgb, var(--nearest-background-color, #e0e0e0) 90%, black);
                    box-shadow: -4px -4px 8px rgba(255,255,255,0.7), 4px 4px 8px rgba(0,0,0,0.15);
                    cursor: pointer;
                    border: none;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """input.kui::-webkit-outer-spin-button, input.kui::-webkit-inner-spin-button {
                    -webkit-appearance: none;
                }"""
            )
        } catch (e: Throwable) { /*squish*/
        }
        try {
            dynamicCss.rule(
                """input.kui[type=number] {
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
        if (value.roughPx == 0.0)
            return "none"

        return listOf(
            "0px ${this.times(3.0/2).value} ${this.div(2).value} -${this.value} rgba(0, 0, 0, 0.2)",
            "0px ${this.value} ${this.value} 0px rgba(0, 0, 0, 0.14)",
            "0px ${this.div(2).value} ${this.times(5.0/2).value} 0px rgba(0, 0, 0, 0.12)",
        ).joinToString(", ")
    }

    private fun List<Shadow>.toBoxShadow(): String {
        if (isEmpty()) return "none"
        return joinToString(", ") { shadow ->
            val inset = if (shadow.inset) "inset " else ""
            "$inset${shadow.offsetX.value} ${shadow.offsetY.value} ${shadow.blurRadius.value} ${shadow.spreadRadius.value} ${shadow.color.toWeb()}"
        }
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
                asSelectors = listOf(".checked.checkResponsive", ":checked.checkResponsive"),
            )
            sub(
                UnselectedSemantic,
                asSelectors = listOf(".checkResponsive:not(:checked):not(.checked)"),
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

        val directSel = sel(".kui")

        val backSel = (if (includeMaybeTransition) sel(".kui.clickable") else sel(".kui.transition"))

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
            addToCss(backSel, "outline-width", it.value.toString())
            addToCss(backSel, "outline-style", if (it != 0.px) "solid" else "none")
            // hack!  this makes button bars possible, though it does change where the outline goes.
            // TODO: please please figure out a better way
            addToCss(backSel, "outline-offset", it.times(-1).coerceAtLeast(theme.padding.top.times(-1)).value.toString())
//            addToCss(backSel, "outline-offset", it.times(-1).value.toString())
        }
        // Handle shadows - use explicit shadows if set, otherwise fall back to elevation
        theme.diff(diff) { shadows }?.let { shadows ->
            if (shadows != null && shadows.isNotEmpty()) {
                addToCss(backSel, "box-shadow", shadows.toBoxShadow())
                // Allow neumorphic shadows to paint beyond this container's bounds
                addToCss(backSel, "overflow", "visible")
                // Outer shadows must paint above sibling elements (e.g. bar shadow over content area)
                if (shadows.any { !it.inset }) {
                    addToCss(backSel, "position", "relative")
                    addToCss(backSel, "z-index", "1")
                }
                // Set shadow room as CSS variable so scroll containers can add padding
                val maxExtent = shadows.filter { !it.inset }.maxOfOrNull {
                    it.blurRadius.value.roughPx + it.spreadRadius.value.roughPx +
                            maxOf(abs(it.offsetX.value.roughPx), abs(it.offsetY.value.roughPx))
                } ?: 0.0
                if (maxExtent > 0.0) {
                    addToCss(directSel, "--shadow-room", "${maxExtent.roundToInt()}px")
                }
            } else {
                // shadows explicitly set to null or empty - check elevation
                addToCss(backSel, "box-shadow", theme.elevation.toBoxShadow())
                addToCss(directSel, "--shadow-room", "0px")
            }
        } ?: theme.diff(diff) { elevation }?.let {
            // shadows unchanged, but elevation changed
            if (theme.shadows == null || theme.shadows!!.isEmpty()) {
                addToCss(backSel, "box-shadow", theme.elevation.toBoxShadow())
            }
        }

        theme.diff(diff) { gap }?.let { addToCss(directSel, "--spacing", it.value.toString()) }
        theme.diff(diff) { padding }?.let { addToCss(directSel, "--padding", it.css()) }
        theme.diff(diff) { font.size }?.let { addToCss(directSel, "font-size", it.value.toString()) }
        theme.diff(diff) { font.font }?.let { addToCss(directSel, "font-family", it.let { dynamicCss.font(it) }) }
        theme.diff(diff) { font.weight }?.let { addToCss(directSel, "font-weight", it.toString()) }
        theme.diff(diff) { font.italic }
            ?.let { addToCss(directSel, "font-style", it.let { if (it) "italic" else "normal" }) }
        theme.diff(diff) { font.allCaps }
            ?.let { addToCss(directSel, "text-transform", it.let { if (it) "uppercase" else "none" }) }
        theme.diff(diff) { font.lineSpacingMultiplier }
            ?.let { addToCss(directSel, "line-height", it.toString()) }
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
        theme.diff(diff) { blurBackground }?.let {

            if(it.value != DimensionRaw.zero) {
                val filterValue = "blur(${it.value})"
                if (filterValue.isNotEmpty()) {
                    addToCss(backSel, "backdrop-filter", filterValue)
                    addToCss(backSel, "-webkit-backdrop-filter", filterValue)
                } else {
                    addToCss(backSel, "backdrop-filter", "none")
                    addToCss(backSel, "-webkit-backdrop-filter", "none")
                }
            } else {
                addToCss(backSel, "backdrop-filter", "none")
                addToCss(backSel, "-webkit-backdrop-filter", "none")
            }
        }
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
        
        theme.diff(diff) { transform }?.let {
            if (it != null) {
                val transformParts = mutableListOf<String>()
                
                // Add translation transforms
                if (it.translationX != 0.0 || it.translationY != 0.0 || it.translationZ != 0.0) {
                    val translateParts = mutableListOf<String>()
                    if (it.translationX != 0.0) translateParts.add("${it.translationX}px")
                    if (it.translationY != 0.0) translateParts.add("${it.translationY}px")
                    if (it.translationZ != 0.0) translateParts.add("${it.translationZ}px")
                    
                    transformParts.add("translate3d(${translateParts.joinToString(", ")})")
                }
                
                // Add rotation transforms
                if (it.rotation != 0.0) {
                    transformParts.add("rotate(${it.rotation}deg)")
                }
                if (it.rotationX != 0.0) {
                    transformParts.add("rotateX(${it.rotationX}deg)")
                }
                if (it.rotationY != 0.0) {
                    transformParts.add("rotateY(${it.rotationY}deg)")
                }
                
                // Add scale transforms
                if (it.scaleX != 1.0 || it.scaleY != 1.0) {
                    transformParts.add("scale(${it.scaleX}, ${it.scaleY})")
                }
                
                if (transformParts.isNotEmpty()) {
                    addToCss(backSel, "transform", transformParts.joinToString(" "))
                }
            } else {
                addToCss(backSel, "transform", "none")
            }
        }

        return classes
    }

    fun Edges.css() = "${top.value} ${right.value} ${bottom.value} ${left.value}"

    val rowCollapsingToColumnHandled = HashSet<String>()
    fun rowCollapsingToColumn(breakpoints: List<Dimension>): String {
        val name = "rowCollapsingToColumn_${breakpoints.joinToString("_") { it.value.roughPx.roundToInt().toString() }}"
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
