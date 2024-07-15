package com.lightningkite.kiteui.models

import kotlin.js.JsName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Theme(
    val id: String,
    val title: FontAndStyle = FontAndStyle(systemDefaultFont),
    val body: FontAndStyle = FontAndStyle(systemDefaultFont),
    val elevation: Dimension = 1.px,
    val cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
    val spacing: Dimension = 1.rem,
    val navSpacing: Dimension = 0.rem,
    val foreground: Paint = Color.black,
    val iconOverride: Paint? = null,
    val outline: Paint = Color.black,
    val outlineWidth: Dimension = 0.px,
    val background: Paint = Color.white,
    val backdropFilters: List<BackdropFilter> = listOf(),
    val bodyTransitions: ScreenTransitions = ScreenTransitions.Fade,
    val dialogTransitions: ScreenTransitions = ScreenTransitions.Fade,
    val transitionDuration: Duration = 0.15.seconds,

    val card: (Theme.() -> Theme) = { this },
    val field: (Theme.() -> Theme) = { this },
    val button: (Theme.() -> Theme) = { this },
    val hover: (Theme.() -> Theme) = {
        copy(
            id = "hov",
            background = this.background.closestColor().highlight(0.2f),
            outline = this.background.closestColor().highlight(0.2f).highlight(0.1f),
            elevation = this.elevation * 2f,
        )
    },
    val focus: (Theme.() -> Theme) = {
        copy(
            id = "fcs",
            outlineWidth = outlineWidth + 2.dp
        )
    },
    val dialog: (Theme.() -> Theme) = {
        copy(
            id = "dlg",
            background = this.background.closestColor().lighten(0.1f),
            outline = this.outline.closestColor().lighten(0.1f),
            elevation = this.elevation * 2f,
        )
    },
    val down: (Theme.() -> Theme) = {
        copy(
            id = "dwn",
            background = this.background.closestColor().highlight(0.3f),
            outline = this.background.closestColor().highlight(0.3f).highlight(0.1f),
            elevation = this.elevation / 2f,
        )
    },
    val unselected: (Theme.() -> Theme) = { this },
    val selected: (Theme.() -> Theme) = { this.down(this) },
    val disabled: (Theme.() -> Theme) = {
        copy(
            id = "dis",
            foreground = this.foreground.applyAlpha(alpha = 0.25f),
            background = this.background.applyAlpha(alpha = 0.5f),
            outline = this.outline.applyAlpha(alpha = 0.25f),
        )
    },
    val invalid: (Theme.() -> Theme) = {
        copy(
            id = "inv",
            outline = Color.red,
            outlineWidth = 2.px,
        )
    },
    val mainContent: (Theme.() -> Theme?) = { null },
    val bar: (Theme.() -> Theme?) = {
        copy(
            id = "bar",
            foreground = this.background,
            background = this.foreground,
            outline = this.foreground.closestColor().highlight(1f)
        )
    },
    val nav: (Theme.() -> Theme?) = bar,
    val important: (Theme.() -> Theme) = {
        copy(
            id = "imp",
            foreground = this.background,
            background = this.foreground,
            outline = this.foreground.closestColor().highlight(1f)
        )
    },
    val critical: (Theme.() -> Theme) = { this.important(this).let { it.important(it) } },
    val warning: (Theme.() -> Theme) = {
        copy(
            id = "wrn",
            background = Color.fromHex(0xFFe36e24.toInt()),
            outline = Color.fromHex(0xFFe36e24.toInt()).highlight(0.1f),
            foreground = Color.white
        )
    },
    val danger: (Theme.() -> Theme) = {
        copy(
            id = "dgr",
            background = Color.fromHex(0xFFB00020.toInt()),
            outline = Color.fromHex(0xFFB00020.toInt()).highlight(0.1f),
            foreground = Color.white
        )
    },
    val affirmative: (Theme.() -> Theme) = {
        copy(
            id = "afr",
            background = Color.fromHex(0xFF20a020.toInt()),
            outline = Color.fromHex(0xFF20a020.toInt()).highlight(0.1f),
            foreground = Color.white
        )
    },
) {
    val icon: Paint get() = iconOverride ?: foreground

    private var mainContentCache: Theme? = null
    @JsName("mainContentDirect")
    fun mainContent() = mainContentCache ?: mainContent(this).also { mainContentCache = it }
    private var cardCache: Theme? = null
    @JsName("cardDirect")
    fun card() = cardCache ?: card(this).also { cardCache = it }
    private var dialogCache: Theme? = null
    @JsName("dialogDirect")
    fun dialog() = dialogCache ?: dialog(this).also { dialogCache = it }
    private var fieldCache: Theme? = null
    @JsName("fieldDirect")
    fun field() = fieldCache ?: field(this).also { fieldCache = it }
    private var buttonCache: Theme? = null
    @JsName("buttonDirect")
    fun button() = buttonCache ?: button(this).also { buttonCache = it }
    private var hoverCache: Theme? = null
    @JsName("hoverDirect")
    fun hover() = hoverCache ?: hover(this).also { hoverCache = it }
    private var focusCache: Theme? = null
    @JsName("focusDirect")
    fun focus() = focusCache ?: focus(this).also { focusCache = it }
    private var downCache: Theme? = null
    @JsName("downDirect")
    fun down() = downCache ?: down(this).also { downCache = it }
    private var selectedCache: Theme? = null
    @JsName("selectedDirect")
    fun selected() = selectedCache ?: selected(this).also { selectedCache = it }
    private var unselectedCache: Theme? = null
    @JsName("unselectedDirect")
    fun unselected() = unselectedCache ?: unselected(this).also { unselectedCache = it }
    private var disabledCache: Theme? = null
    @JsName("disabledDirect")
    fun disabled() = disabledCache ?: disabled(this).also { disabledCache = it }
    private var invalidCache: Theme? = null
    @JsName("invalidDirect")
    fun invalid() = invalidCache ?: invalid(this).also { invalidCache = it }
    @JsName("barDirect")
    inline fun bar() = bar(this)
    private var importantCache: Theme? = null
    @JsName("importantDirect")
    fun important() = importantCache ?: important(this).also { importantCache = it }
    private var criticalCache: Theme? = null
    @JsName("criticalDirect")
    fun critical() = criticalCache ?: critical(this).also { criticalCache = it }
    private var navCache: Theme? = null
    @JsName("navDirect")
    fun nav() = navCache ?: nav(this).also { navCache = it }
    private var warningCache: Theme? = null
    @JsName("warningDirect")
    fun warning() = warningCache ?: warning(this).also { warningCache = it }
    private var dangerCache: Theme? = null
    @JsName("dangerDirect")
    fun danger() = dangerCache ?: danger(this).also { dangerCache = it }
    private var affirmativeCache: Theme? = null
    @JsName("affirmativeDirect")
    fun affirmative() = affirmativeCache ?: affirmative(this).also { affirmativeCache = it }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is Theme && this.id == other.id
    }

    fun copy(
        id: String,
        title: FontAndStyle = this.title,
        body: FontAndStyle = this.body,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        spacing: Dimension = this.spacing,
        navSpacing: Dimension = this.navSpacing,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
        backdropFilters: List<BackdropFilter> = this.backdropFilters,
        bodyTransitions: ScreenTransitions = this.bodyTransitions,
        dialogTransitions: ScreenTransitions = this.dialogTransitions,
        transitionDuration: Duration = this.transitionDuration,
        card: (Theme.() -> Theme) = this.card,
        field: (Theme.() -> Theme) = this.field,
        button: (Theme.() -> Theme) = this.button,
        hover: (Theme.() -> Theme) = this.hover,
        focus: (Theme.() -> Theme) = this.focus,
        dialog: (Theme.() -> Theme) = this.dialog,
        down: (Theme.() -> Theme) = this.down,
        unselected: (Theme.() -> Theme) = this.unselected,
        selected: (Theme.() -> Theme) = this.selected,
        disabled: (Theme.() -> Theme) = this.disabled,
        mainContent: (Theme.() -> Theme?) = this.mainContent,
        bar: (Theme.() -> Theme?) = this.bar,
        nav: (Theme.() -> Theme?) = this.nav,
        important: (Theme.() -> Theme) = this.important,
        critical: (Theme.() -> Theme) = this.critical,
        warning: (Theme.() -> Theme) = this.warning,
        danger: (Theme.() -> Theme) = this.danger,
        affirmative: (Theme.() -> Theme) = this.affirmative,
    ): Theme = Theme(
        id = "${this.id}-$id",
        title = title,
        body = body,
        elevation = elevation,
        cornerRadii = cornerRadii,
        spacing = spacing,
        navSpacing = navSpacing,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        backdropFilters = backdropFilters,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        card = card,
        field = field,
        button = button,
        hover = hover,
        focus = focus,
        dialog = dialog,
        down = down,
        unselected = unselected,
        selected = selected,
        disabled = disabled,
        mainContent = mainContent,
        bar = bar,
        nav = nav,
        important = important,
        critical = critical,
        warning = warning,
        danger = danger,
        affirmative = affirmative,
    )

    fun copy(
        title: FontAndStyle = this.title,
        body: FontAndStyle = this.body,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        spacing: Dimension = this.spacing,
        navSpacing: Dimension = this.navSpacing,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
        backdropFilters: List<BackdropFilter> = this.backdropFilters,
        bodyTransitions: ScreenTransitions = this.bodyTransitions,
        dialogTransitions: ScreenTransitions = this.dialogTransitions,
        transitionDuration: Duration = this.transitionDuration,
    ): Theme = Theme(
        id = "cp${
            run {
                var out = 0
                out = out * 31 + title.hashCode()
                out = out * 31 + body.hashCode()
                out = out * 31 + elevation.hashCode()
                out = out * 31 + cornerRadii.hashCode()
                out = out * 31 + spacing.hashCode()
                out = out * 31 + foreground.hashCode()
                out = out * 31 + iconOverride.hashCode()
                out = out * 31 + outline.hashCode()
                out = out * 31 + outlineWidth.hashCode()
                out = out * 31 + background.hashCode()
                out
                buildString {
                    append(((out shr 12).mod(64)).let { shortCodeChars[it] })
                    append(((out shr 6).mod(64)).let { shortCodeChars[it] })
                    append((out.mod(64)).let { shortCodeChars[it] })
                }
            }
        }",
        title = title,
        body = body,
        elevation = elevation,
        cornerRadii = cornerRadii,
        spacing = spacing,
        navSpacing = navSpacing,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        backdropFilters = backdropFilters,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        card = card,
        field = field,
        button = button,
        hover = hover,
        focus = focus,
        dialog = dialog,
        down = down,
        unselected = unselected,
        selected = selected,
        disabled = disabled,
        mainContent = mainContent,
        bar = bar,
        nav = nav,
        important = important,
        critical = critical,
        warning = warning,
        danger = danger,
        affirmative = affirmative,
    )

    companion object {
        val placeholder = Theme("placeholder")
        val shortCodeChars = "1234567890QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm-_"
    }
}

