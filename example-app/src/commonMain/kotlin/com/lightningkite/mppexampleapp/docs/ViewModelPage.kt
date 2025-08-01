package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.signal.Constant
import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.titledSection
import com.lightningkite.mppexampleapp.widgets.code

@Routable("docs/viewmodel")
public object ViewModelPage: DocPage {

    public override val title: Readable<String>
        get() = Constant("ViewModels in KiteUI")

    public override val covers: List<String> = listOf(
        "ViewModel",
    )

    public override fun ViewWriter.render(): ViewModifiable = run {
        article {
            titledSection("ViewModels") {
                text("One question I've received about KiteUI is how one does ViewModels.")
                text("The answer is simple - there's no need for an interface because you can simply do what you need with basic classes.")
                text("Here's the common example in Kotlin Multiplatform Compose:")
                code {
                    content = """
                        class OrderViewModel : ViewModel() {
                           private val _uiState = MutableStateFlow(OrderUiState(pickupOptions = pickupOptions()))
                           public val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()
                           public suspend fun submit() = ...
                           // ...
                        }
                    """.trimIndent()
                }
                text("A direct translation would be this:")
                code {
                    content = """
                        class OrderViewModel {
                           private val _uiState = Property(OrderUiState(pickupOptions = pickupOptions()))
                           public val uiState: Readable<OrderUiState> get() = _uiState
                           public suspend fun submit() = ...
                           // ...
                        }
                    """.trimIndent()
                }
                text("However, it would be even more accurate to state that the intention is that you do this:")
                code {
                    content = """
                        class OrderViewPage: Page {
                            private val _uiState = Property(OrderUiState(pickupOptions = pickupOptions()))
                            public val uiState: Readable<OrderUiState> get() = _uiState
                            public suspend fun submit() = ...
                            // ...
                            
                            public override fun ViewWriter.render2() = col {
                                text {
                                    ::content { "My UI state is: " + uiState().toString() }
                                }
                            }
                        }
                    """.trimIndent()
                }
                text("This reduces the file count in what I believe is a useful way.  We now tightly associate the page's data and its render within the same file, while also being able to unit test the model without issue.")
                text("However, if keeping those in the same file really bothers you, as mentioned above, there's no reason you can't keep them isolated by using a separate class.")
            }
        }
    }

}