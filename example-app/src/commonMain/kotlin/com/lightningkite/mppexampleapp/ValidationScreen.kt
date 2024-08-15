//package com.lightningkite.mppexampleapp
//
//import com.lightningkite.kiteui.*
//import com.lightningkite.kiteui.models.InvalidSemantic
//import com.lightningkite.kiteui.models.rem
//import com.lightningkite.kiteui.navigation.Screen
//import com.lightningkite.kiteui.reactive.*
//import com.lightningkite.kiteui.views.*
//import com.lightningkite.kiteui.views.direct.*
//import kotlin.coroutines.CoroutineContext
//
//@Routable("validation")
//object ValidationScreen : Screen {
//    override val title: Readable<String>
//        get() = super.title
//
//    override fun ViewWriter.render() {
//        with(Form()) {
//            col {
//                val showValidations = Property(false)
//                val rawData = Property<Int>(0)
//                val rawDataAsString = rawData.validationLens(
//                    during = this,
//                    get = {
//                        println("Getting $it")
//                        it.toString()
//                    },
//                    set = {
//                        println("SETTING TO $it")
//                        if (it.isBlank()) throw InvalidException("Required", "This field is required.", 1)
//                        it.toIntOrNull() ?: throw InvalidException("Not a Number", "This must be an integer.", 0)
//                    }
//                )
//                val tooShort = shared {
//                    if (rawData().toString().length < 5) throw InvalidException(
//                        "Too Short",
//                        "It needs to be longer",
//                        2
//                    )
//                }
//
//                val data = Property(beeMovieScript.split('\n').take(20).withIndex().toList())
//                val childElements = data.lensByElement(identity = { it.index }) {
//                    val lensed = it.lens(
//                        get = { it.value },
//                        set = { old, it -> old.copy(value = it) }
//                    )
//                    it to .validation(during = this) {
//                        if (it.isBlank()) throw InvalidException("Required", "This cannot be blank", 1)
//                    }
//                }
//
//                fieldTheme - textField {
//                    validates(tooShort, rawDataAsString) { showValidations() }
//                    content bind rawDataAsString
//                }
//                text {
//                    ::content {
//                        exceptionMessage(rawDataAsString, tooShort)
//                    }
//                }
//
//                expanding - recyclerView {
//                    children(childElements) { itemObs ->
//                        row {
//                            centered - sizeConstraints(width = 5.rem) - text {
//                                ::content { itemObs().first().index.toString() }
//                            }
//                            expanding - fieldTheme - textField {
//                                val x = shared { itemObs().second }//.flatten()
//                                validates(x)
//                                content bind x
//                            }
//                        }
//                    }
//                }
//                button {
//                    text("Reset")
//                    onClick {
//                        rawData.value = 1234567
//                    }
//                }
//                button {
//                    text("GO!")
//                    onClick {
//                        showValidations.value = true
//                    }
//                }
//            }
//        }
//    }
//}
//
//class Form {
//    val issues = Property<Map<Any?, Exception>>(mapOf())
//    fun clear(key: Any?) {
//        if(issues.value.containsKey(key)) issues.value -= key
//    }
//    fun report(key: Any?, exception: Exception) {
//        issues.value += (key to exception)
//    }
//    fun <A, B> Writable<A>.validationLens(
//        during: CalculationContext,
//        get: (A) -> B,
//        set: (B) -> A
//    ): Writable<B> {
//        val result = TempStateWritable<A, B>(
//            source = this@validationLens,
//            get = get,
//            trySet = { value ->
//                try {
//                    this.set(set(value))
//                    clear(this@validationLens)
//                    clear(this)
//                } catch (e: Exception) {
//                    report(this@validationLens, e)
//                    report(this, e)
//                }
//            }
//        )
//        during.onRemove {
//            clear(this@validationLens)
//            clear(this)
//        }
//        return result
//    }
//
//    fun <A> Writable<A>.validation(during: CalculationContext, check: (A) -> Unit) {
//        during.reactiveScope {
//            try {
//                check(this@validation())
//                clear(this@validation)
//            } catch(e: Exception) {
//                report(this@validation, e)
//            }
//        }
//        during.onRemove {
//            clear(this@validation)
//        }
//    }
//
//    fun issues(vararg checkables: Any?) = issues.lens { it.filterKeys { it in checkables } }
//
//    private suspend fun Readable<Map<Any?, Exception>>.internal_allValid() = invoke().let { it.isEmpty() }
//    private suspend fun Readable<Map<Any?, Exception>>.internal_exceptions() = invoke().let { it.values }
//    private suspend fun Readable<Map<Any?, Exception>>.internal_exceptionMessage() = invoke().let { it.values.joinToString("\n") { it.message ?: ""} }
//    suspend fun allValid(vararg checkables: Any?): Boolean = issues(*checkables).internal_allValid()
//    suspend fun exceptions(vararg checkables: Any?): Collection<Exception> = issues(*checkables).internal_exceptions()
//    suspend fun exceptionMessage(vararg checkables: Any?): String = issues(*checkables).internal_exceptionMessage()
//    suspend fun allValid(): Boolean = issues.internal_allValid()
//    suspend fun exceptions(): Collection<Exception> = issues.internal_exceptions()
//    suspend fun exceptionMessage(): String = issues.internal_exceptionMessage()
//
//    fun RView.validates(vararg checkables: Any?, show: suspend () -> Boolean = { true }) =
//        dynamicTheme { if (allValid(*checkables) || !show()) null else InvalidSemantic }
//}
//
//class InvalidException(
//    val title: String,
//    val description: String,
//    val code: Int
//) : Exception(title)
//
//
//private class TempStateWritable<A, B>(
//    val source: Writable<A>,
//    val get: (A) -> B,
//    val trySet: suspend Writable<A>.(B) -> Unit
//) : Writable<B> {
//    private var _state: ReadableState<B> = ReadableState.notReady
//    override var state: ReadableState<B>
//        get() {
//            @Suppress("UNCHECKED_CAST")
//            if (myListen == null) _state = source.state.map { get(it) }
//            return _state
//        }
//        private set(value) {
//            if (_state != value) {
//                _state = value
//                myListeners.invokeAllSafe()
//            }
//        }
//
//    private val myListeners = ArrayList<() -> Unit>()
//    private var myListen: (() -> Unit)? = null
//    override fun addListener(listener: () -> Unit): () -> Unit {
//        myListeners.add(listener)
//        if (myListeners.size == 1) {
//            myListen = source.addListener {
//                @Suppress("UNCHECKED_CAST")
//                state = source.state.map { get(it) }
//            }
//            state = source.state.map { get(it) }
//        }
//        return {
//            myListeners.remove(listener)
//            if (myListeners.isEmpty()) {
//                myListen?.invoke()
//                myListen = null
//            }
//        }
//    }
//
//    /**
//     * Queues changes
//     */
//    override suspend fun set(value: B) {
//        state = ReadableState(value)
//        trySet(source, value)
//    }
//}