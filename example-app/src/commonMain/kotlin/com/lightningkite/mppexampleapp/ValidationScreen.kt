package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.InvalidSemantic
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.validation.InvalidException
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

@Routable("validation")
object ValidationScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        val rawData = Property<Int>(0)
        val rawDataAsString = rawData.map(
            get = {
                println("Getting $it")
                it.toString()
            },
            set = { _, it ->
                println("SETTING TO $it")
                if (it.isBlank()) throw InvalidException("Required", "This field is required.", 1)
                it.toIntOrNull() ?: throw InvalidException("Not a Number", "This must be an integer.", 0)
            }
        )
        val tooShort = shared { if(rawDataAsString().length < 8) throw InvalidException("Too Short", "It needs to be longer", 2) }

        col {
            fieldTheme - textField {
                dynamicTheme { if(allValid(tooShort, rawDataAsString)) null else InvalidSemantic }
                content bind rawDataAsString
            }
            text {
                ::content {
                    rawDataAsString.watchState().let { it.exception?.message ?: it.getOrNull().toString() }
                }
            }
            button {
                ::enabled { allValid(rawDataAsString, tooShort) }
                text("GO!")
            }
        }
    }
}

suspend fun allValid(vararg readables: Readable<*>): Boolean = readables.all { it.watchState().success }
