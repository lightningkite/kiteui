package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.readable.invoke
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

@Routable("experiment")
object ExperimentPage : Page {
    override val title: Readable<String>
        get() = super.title

    @QueryParameter
    val elementCount = Property(7)

    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            val selected = Property<DateOptions?>(DateOptions.Yesterday)
            val display = Property("")
            fun render(option: DateOptions?): String = option?.name ?: "Anytime"
//            menuButton {
//                row {
//                    expanding - centered - text {
//                        ::content { display() }
//                    }
//                }
//                requireClick = true
//                opensMenu {
//                    stack{}
////                    sizeConstraints(
////                        width = 14.rem,
////                        height = 14.rem
////                    ) -
//                }
//            }
            expanding - ListSemantic.onNext - recyclerView {
                children(
                    items = Constant(DateOptions.entries.toList() + listOf(null)),
                    id = { it },
                    render = { option ->
                        card - button {
                            dynamicTheme {
                                if (selected() == option()) SelectedSemantic
                                else null
                            }
                            atStart - text {
                                ::content { render(option()) }
                            }
                            onClick {
                                selected set option()
                                closePopovers()
                            }
                        }
                    }
                )

                reactive {
                    display.value = render(selected())
                }
            }
        }
    }
}

private data class Wrapper<T>(val value: T)

data class FilterDate(
    val type: DateOptions,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
)

enum class DateOptions(private val str: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    ThisWeek("This Week"),
    Last7Days("Last 7 Days"),
    ThisMonth("This Month"),
    Last30Days("Last 30 Days"),
    Custom("Custom");

    override fun toString(): String {
        return str
    }
}