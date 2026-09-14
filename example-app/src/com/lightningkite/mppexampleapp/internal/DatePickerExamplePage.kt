package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.localDateField
import com.lightningkite.kiteui.views.direct.localDateTimeField
import com.lightningkite.kiteui.views.direct.localTimeField
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.lensing.lens
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object DatePickerExamplePage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit {
        col {
            h1 { content = "Date Picker"}
            space()

            val localDateFieldDate = Signal<LocalDate?>(null)
            field("Local Date Field") {
                localDateField {
                    this.range = object : ClosedRange<LocalDate> {
                        override val start =
                            Clock.System.now().plus(2.days).toLocalDateTime(TimeZone.currentSystemDefault()).date
                        override val endInclusive =
                            Clock.System.now().plus(8.days).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    content bind localDateFieldDate
                }
            }


            val localTimeDate = Signal<LocalTime?>(null)
            field("Local Time Field") {
                localTimeField {
                    this.range = object : ClosedRange<LocalTime> {
                        override val start =
                            Clock.System.now().minus(20.minutes).toLocalDateTime(TimeZone.currentSystemDefault()).time
                        override val endInclusive =
                            Clock.System.now().plus(20.minutes).toLocalDateTime(TimeZone.currentSystemDefault()).time
                    }
                    content bind localTimeDate
                }
            }



            val localDateTimeDate = Signal<LocalDateTime?>(null)
            field("Local Date Time Field") {
                localDateTimeField {
                    this.range = object : ClosedRange<LocalDateTime> {
                        override val start =
                            Clock.System.now().minus(2.days).toLocalDateTime(TimeZone.currentSystemDefault())
                        override val endInclusive =
                            Clock.System.now().plus(3.days).toLocalDateTime(TimeZone.currentSystemDefault())
                    }
                    content bind localDateTimeDate
                }
            }
        }
    }

}