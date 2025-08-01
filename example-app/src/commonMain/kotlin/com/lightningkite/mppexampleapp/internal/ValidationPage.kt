package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.models.InvalidSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import kotlin.coroutines.CoroutineContext

@Routable("validation")
public object ValidationPage : Page {
    public override val title: Readable<String>
        get() = super.title

    public data class Complex(
        public val x: Int = 0,
        public val y: String = ""
    )

    public override fun ViewWriter.render(): ViewModifiable = run {
        val original = Property(Complex())
        col {
            field("x") {
                textInput {
                    content bind original.lens(
                        get = { it.x },
                        modify = { o, it -> o.copy(x = it) }
                    ).validationLens(
                        get = { it.toString() },
                        check = { if(it > 100) throw PlainTextException("Too big!") },
                        modify = { _, it -> it.toInt() }
                    )
                }
            }
            field("y") {
                textInput {
                    content bind original.lens(
                        get = { it.y },
                        modify = { o, it -> o.copy(y = it) }
                    ).validationLens(get = { it }, check = {
                        if(it.isBlank()) throw PlainTextException("Cannot be blank")
                        if(it.length > 100) throw PlainTextException("Too long")
                    }, modify = { _, it -> it })
                }
            }
        }
    }
}
