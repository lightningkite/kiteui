package com.lightningkite.kiteui.models

import kotlin.js.JsName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

typealias ThemeDeriver = (Theme) -> Theme?

inline fun themeDeriver(noinline received: Theme.() -> Theme?): ThemeDeriver = received

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

    val card: ThemeDeriver = { it },
    val field: ThemeDeriver = { null },
    val button: ThemeDeriver = { null },
    val hover: ThemeDeriver = {
        it.copy(
            id = "hov",
            background = it.background.closestColor().highlight(0.2f),
            outline = it.background.closestColor().highlight(0.2f).highlight(0.1f),
            elevation = it.elevation * 2f,
        )
    },
    val focus: ThemeDeriver = {
        it.copy(
            id = "fcs",
            outlineWidth = outlineWidth + 2.dp
        )
    },
    val dialog: ThemeDeriver = {
        it.copy(
            id = "dlg",
            background = it.background.closestColor().lighten(0.1f),
            outline = it.outline.closestColor().lighten(0.1f),
            elevation = it.elevation * 2f,
        )
    },
    val down: ThemeDeriver = {
        it.copy(
            id = "dwn",
            background = it.background.closestColor().highlight(0.3f),
            outline = it.background.closestColor().highlight(0.3f).highlight(0.1f),
            elevation = it.elevation / 2f,
        )
    },
    val unselected: ThemeDeriver = { it },
    val selected: ThemeDeriver = { it.down(it) },
    val disabled: ThemeDeriver = {
        it.copy(
            id = "dis",
            foreground = it.foreground.applyAlpha(alpha = 0.25f),
            background = it.background.applyAlpha(alpha = 0.5f),
            outline = it.outline.applyAlpha(alpha = 0.25f),
        )
    },
    val invalid: ThemeDeriver = {
        it.copy(
            id = "inv",
            outline = Color.red,
            outlineWidth = 2.px,
        )
    },
    val mainContent: ThemeDeriver = { null },
    val bar: ThemeDeriver = {
        it.copy(
            id = "bar",
            foreground = it.background,
            background = it.foreground,
            outline = it.foreground.closestColor().highlight(1f)
        )
    },
    val nav: ThemeDeriver = bar,
    val important: ThemeDeriver = {
        it.copy(
            id = "imp",
            foreground = it.background,
            background = it.foreground,
            outline = it.foreground.closestColor().highlight(1f)
        )
    },
    val critical: ThemeDeriver = { it.important(it)?.let { it.important(it) } },
    val warning: ThemeDeriver = {
        it.copy(
            id = "wrn",
            background = Color.fromHex(0xFFe36e24.toInt()),
            outline = Color.fromHex(0xFFe36e24.toInt()).highlight(0.1f),
            foreground = Color.white
        )
    },
    val danger: ThemeDeriver = {
        it.copy(
            id = "dgr",
            background = Color.fromHex(0xFFB00020.toInt()),
            outline = Color.fromHex(0xFFB00020.toInt()).highlight(0.1f),
            foreground = Color.white
        )
    },
    val affirmative: ThemeDeriver = {
        it.copy(
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
        card: ThemeDeriver = this.card,
        field: ThemeDeriver = this.field,
        button: ThemeDeriver = this.button,
        hover: ThemeDeriver = this.hover,
        focus: ThemeDeriver = this.focus,
        dialog: ThemeDeriver = this.dialog,
        down: ThemeDeriver = this.down,
        unselected: ThemeDeriver = this.unselected,
        selected: ThemeDeriver = this.selected,
        disabled: ThemeDeriver = this.disabled,
        mainContent: ThemeDeriver = this.mainContent,
        bar: ThemeDeriver = this.bar,
        nav: ThemeDeriver = this.nav,
        important: ThemeDeriver = this.important,
        critical: ThemeDeriver = this.critical,
        warning: ThemeDeriver = this.warning,
        danger: ThemeDeriver = this.danger,
        affirmative: ThemeDeriver = this.affirmative,
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

