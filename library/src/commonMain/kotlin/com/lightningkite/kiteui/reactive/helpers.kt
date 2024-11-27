package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.views.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.random.Random

@JsName("invokeAllSafeMutable")
@JvmName("invokeAllSafeMutable")
fun MutableList<() -> Unit>.invokeAllSafe() = toList().invokeAllSafe()
fun List<() -> Unit>.invokeAllSafe() = forEach {
    try {
        it()
    } catch (e: Exception) {
        if (e is CancelledException) return@forEach
        e.report()
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun <A> CalculationContext.oneAtATime(work: Boolean, action: suspend (A) -> Unit): (A) -> Unit {
    var lastJob: Job? = null
    var reportTo = RawReadable(ReadableState(Unit))
    if (work)
        coroutineContext[StatusListener]?.working(reportTo)
    else
        coroutineContext[StatusListener]?.loading(reportTo)

    return {
        lastJob?.cancel()
        lastJob = this.let { calculationContext ->
            var done = false
            val job = calculationContext.launch(
                start = if (calculationContext.coroutineContext[CoroutineDispatcher]?.isDispatchNeeded(
                        calculationContext.coroutineContext
                    ) == false
                ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
            ) {
                val result = readableState {
                    action(it)
                }
                done = true
                reportTo.state = result
            }

            if (done) {
                return@let null
            } else {
                // start load
                reportTo.state = ReadableState.notReady
                return@let job
            }
        }
    }
}

infix fun <T> Writable<T>.bind(master: Writable<T>) {
    var reportTo = RawReadable(ReadableState(Unit))
    with(CoroutineScopeStack.current()) {
        coroutineContext[StatusListener]?.loading(reportTo)
        launch {
            reportTo.state = ReadableState.notReady
            reportTo.state = readableState {
                var intendedValue: T = master.await()
                this@bind.set(intendedValue)
                val setReplica = this@with.oneAtATime(false) { value: T ->
                    this@bind.set(value)
                }
                val setMaster = this@with.oneAtATime(true) { value: T ->
                    master.set(value)
                }
                master.addListener {
                    master.state.onSuccess {
                        if (intendedValue != it) {
                            intendedValue = it
                            setReplica(it)
                        }
                    }
                }.also { this@with.onRemove(it) }
                this@bind.addListener {
                    this@bind.state.onSuccess {
                        if (intendedValue != it) {
                            intendedValue = it
                            setMaster(it)
                        }
                    }
                }.also { this@with.onRemove(it) }
            }
        }
    }
}

//infix fun <T> ImmediateWritable<T>.bind(master: Writable<T>) {
//    with(CoroutineScopeStack.current()) {
//        var setting = false
//        launch(key = this@bind) {
//            this@bind.set(master.await())
//            master.addListener {
//                if (setting) return@addListener
//                master.state.onSuccess {
//                    setting = true
//                    this@with.reporting(key = this@bind) {
//                        try {
//                            this@bind.value = (it)
//                        } finally {
//                            setting = false
//                        }
//                    }
//                }
//            }.also { onRemove(it) }
//            this@bind.addListener {
//                if (setting) return@addListener
//                this@bind.state.onSuccess {
//                    setting = true
//                    this@with.launch(key = this@bind) {
//                        try {
//                            master.set(it)
//                        } finally {
//                            setting = false
//                        }
//                    }
//                }
//            }.also { onRemove(it) }
//        }
//
//    }
//}
//
//infix fun <T> Writable<T>.bind(master: ImmediateWritable<T>) {
//    with(CoroutineScopeStack.current()) {
//        var setting = false
//        launch(key = this@bind) {
//            this@bind.set(master.value)
//            master.addListener {
//                if (setting) return@addListener
//                master.state.onSuccess {
//                    setting = true
//                    this@with.launch(key = this@bind) {
//                        try {
//                            this@bind.set(it)
//                        } finally {
//                            setting = false
//                        }
//                    }
//
//                }
//            }.also { onRemove(it) }
//            this@bind.addListener {
//                if (setting) return@addListener
//                this@bind.state.onSuccess {
//                    setting = true
//                    this@with.reporting(key = this@bind) {
//                        try {
//                            master.value = it
//                        } finally {
//                            setting = false
//                        }
//                    }
//                }
//            }.also { onRemove(it) }
//        }
//    }
//}
//
//infix fun <T> ImmediateWritable<T>.bind(master: ImmediateWritable<T>) {
//    with(CoroutineScopeStack.current()) {
//        var setting = false
//        this@bind.value = master.value
//        master.addListener {
//            if (setting) {
//                return@addListener
//            }
//            master.state.onSuccess {
//                setting = true
//                this@with.reporting(key = this@bind) {
//                    try {
//                        this@bind.value = it
//                    } finally {
//                        setting = false
//                    }
//                }
//            }
//        }.also { onRemove(it) }
//        this@bind.addListener {
//            if (setting) {
//                return@addListener
//            }
//            this@bind.state.onSuccess {
//                setting = true
//                this@with.reporting(key = this@bind) {
//                    try {
//                        master.value = it
//                    } finally {
//                        setting = false
//                    }
//                }
//            }
//        }.also { onRemove(it) }
//    }
//}

fun <T> Readable<T>.withWrite(action: suspend Readable<T>.(T) -> Unit): Writable<T> =
    object : Writable<T>, Readable<T> by this {
        override suspend fun set(value: T) {
            action(this@withWrite, value)
        }
    }

// Lenses
infix fun <T> Writable<T>.equalTo(value: T): Writable<Boolean> = lens(
    get = { it == value },
    modify = { o, it -> if (it) value else o }
)

infix fun <T> Writable<Set<T>>.contains(value: T): Writable<Boolean> = shared { value in this@contains() }.withWrite { on ->
    if (on) this@contains.set(this@contains.await() + value)
    else this@contains.set(this@contains.await() - value)
}

fun <T : Any> Writable<T>.nullable(): Writable<T?> = lens(
    get = { it },
    modify = { o, it -> it ?: o }
)

fun <T : Any> Writable<T?>.notNull(default: T): Writable<T> = lens(
    get = { it ?: default },
    set = { it }
)

val <T : Any> Writable<T?>.waitForNotNull: Writable<T>
    get() =
        object : Writable<T>, Readable<T> by (this as Readable<T?>).waitForNotNull {
            override suspend fun set(value: T) = this@waitForNotNull.set(value)
        }

fun Writable<String?>.nullToBlank(): Writable<String> = lens(
    get = { it ?: "" },
    set = { it.takeUnless { it.isBlank() } }
)

@JvmName("writableStringAsDouble")
fun Writable<String>.asDouble(): Writable<Double?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toDoubleOrNull() }, set = { it?.commaString() ?: "" })

@JvmName("writableStringAsFloat")
fun Writable<String>.asFloat(): Writable<Float?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toFloatOrNull() }, set = { it?.toDouble()?.commaString() ?: "" })

@JvmName("writableStringAsByte")
fun Writable<String>.asByte(): Writable<Byte?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toByteOrNull() }, set = { it?.toInt()?.commaString() ?: "" })

@JvmName("writableStringAsShort")
fun Writable<String>.asShort(): Writable<Short?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toShortOrNull() }, set = { it?.toInt()?.commaString() ?: "" })

@JvmName("writableStringAsInt")
fun Writable<String>.asInt(): Writable<Int?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toIntOrNull() }, set = { it?.commaString() ?: "" })

@JvmName("writableStringAsLong")
fun Writable<String>.asLong(): Writable<Long?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toLongOrNull() }, set = { it?.commaString() ?: "" })

@JvmName("writableStringAsByteHex")
fun Writable<String>.asByteHex(): Writable<Byte?> = lens(get = { it.toByteOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsUByteHex")
fun Writable<String>.asUByteHex(): Writable<UByte?> = lens(get = { it.toUByteOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsShortHex")
fun Writable<String>.asShortHex(): Writable<Short?> = lens(get = { it.toShortOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsUShortHex")
fun Writable<String>.asUShortHex(): Writable<UShort?> = lens(get = { it.toUShortOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsIntHex")
fun Writable<String>.asIntHex(): Writable<Int?> = lens(get = { it.toIntOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsUIntHex")
fun Writable<String>.asUIntHex(): Writable<UInt?> = lens(get = { it.toUIntOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsLongHex")
fun Writable<String>.asLongHex(): Writable<Long?> = lens(get = { it.toLongOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsULongHex")
fun Writable<String>.asULongHex(): Writable<ULong?> = lens(get = { it.toULongOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableIntAsDoubleNullable")
fun Writable<Int?>.asDouble(): Writable<Double?> = lens(get = { it?.toDouble() }, set = { it?.toInt() })

@JvmName("writableStringAsDouble")
fun ImmediateWritable<String>.asDouble(): ImmediateWritable<Double?> = lens(get = { it.filter { it.isDigit() || it == '-' || it == '.'}.toDoubleOrNull() }, set = { it?.commaString() ?: "" })

@JvmName("writableStringAsFloat")
fun ImmediateWritable<String>.asFloat(): ImmediateWritable<Float?> = lens(get = { it.filter { it.isDigit() || it == '-' || it == '.'}.toFloatOrNull() }, set = { it?.toDouble()?.commaString() ?: "" })

@JvmName("writableStringAsByte")
fun ImmediateWritable<String>.asByte(): ImmediateWritable<Byte?> = lens(get = { it.filter { it.isDigit() || it == '-' || it == '.'}.toByteOrNull() }, set = { it?.toInt()?.commaString() ?: "" })

@JvmName("writableStringAsShort")
fun ImmediateWritable<String>.asShort(): ImmediateWritable<Short?> = lens(get = { it.filter { it.isDigit() || it == '-' || it == '.'}.toShortOrNull() }, set = { it?.toInt()?.commaString() ?: "" })

@JvmName("writableStringAsInt")
fun ImmediateWritable<String>.asInt(): ImmediateWritable<Int?> = lens(get = { it.filter { it.isDigit() || it == '-' || it == '.'}.toIntOrNull() }, set = { it?.commaString() ?: "" })

@JvmName("writableStringAsLong")
fun ImmediateWritable<String>.asLong(): ImmediateWritable<Long?> = lens(get = { it.filter { it.isDigit() || it == '-' || it == '.'}.toLongOrNull() }, set = { it?.commaString() ?: "" })

@JvmName("writableStringAsByteHex")
fun ImmediateWritable<String>.asByteHex(): ImmediateWritable<Byte?> = lens(get = { it.toByteOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsUByteHex")
fun ImmediateWritable<String>.asUByteHex(): ImmediateWritable<UByte?> = lens(get = { it.toUByteOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsShortHex")
fun ImmediateWritable<String>.asShortHex(): ImmediateWritable<Short?> = lens(get = { it.toShortOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsUShortHex")
fun ImmediateWritable<String>.asUShortHex(): ImmediateWritable<UShort?> = lens(get = { it.toUShortOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsIntHex")
fun ImmediateWritable<String>.asIntHex(): ImmediateWritable<Int?> = lens(get = { it.toIntOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsUIntHex")
fun ImmediateWritable<String>.asUIntHex(): ImmediateWritable<UInt?> = lens(get = { it.toUIntOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsLongHex")
fun ImmediateWritable<String>.asLongHex(): ImmediateWritable<Long?> = lens(get = { it.toLongOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableStringAsULongHex")
fun ImmediateWritable<String>.asULongHex(): ImmediateWritable<ULong?> = lens(get = { it.toULongOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableIntAsDoubleNullable")
fun ImmediateWritable<Int?>.asDouble(): ImmediateWritable<Double?> = lens(get = { it?.toDouble() }, set = { it?.toInt() })

suspend infix fun <T> Writable<T>.modify(action: suspend (T) -> T) {
    set(action(await()))
}

suspend infix fun <T> ImmediateWritable<T>.modify(action: suspend (T) -> T) {
    value = action(value)
}

suspend fun Writable<Boolean>.toggle() { set(!awaitOnce()) }
fun ImmediateWritable<Boolean>.toggle() { value = !value }

fun CalculationContext.use(resourceUse: ResourceUse) {
    val x = resourceUse.beginUse()
    onRemove { x() }
}

fun <T, WRITE : Writable<T>> WRITE.interceptWrite(action: suspend WRITE.(T) -> Unit): Writable<T> =
    object : Writable<T>, Readable<T> by this {
        override suspend fun set(value: T) {
            action(this@interceptWrite, value)
        }
    }

fun <T> Readable<Writable<T>>.flatten(): Writable<T> = shared { this@flatten()() }
    .withWrite { this@flatten.state.onSuccess { s -> s set it } }


interface ReadableEmitter<T>: CoroutineScope {
    fun emit(value: T)
}

fun <T> CoroutineScope.readable(emitter: suspend ReadableEmitter<T>.() -> Unit): Readable<T> {
    val prop = LateInitProperty<T>()
    launch {
        emitter(object : ReadableEmitter<T>, CoroutineScope by this {
            override fun emit(value: T) {
                prop.value = value
            }
        })
    }
    return prop
}

fun <T> sharedProcess(scope: CoroutineScope = AppScope, emitter: suspend ReadableEmitter<T>.() -> Unit): Readable<T> {
    return object: BaseReadable<T>() {
        var job: Job? = null
        override fun activate() {
            state = ReadableState.notReady
            job = scope.launch {
                emitter(object : ReadableEmitter<T>, CoroutineScope by this@launch {
                    override fun emit(value: T) {
                        state = ReadableState(value)
                    }
                })
            }
        }
        override fun deactivate() {
            job?.cancel()
            job = null
        }
    }
}
fun <T> sharedProcessRaw(scope: CoroutineScope = AppScope, emitter: suspend ReadableEmitter<ReadableState<T>>.() -> Unit): Readable<T> {
    return object: BaseReadable<T>() {
        var job: Job? = null
        override fun activate() {
            job = scope.launch {
                emitter(object : ReadableEmitter<ReadableState<T>>, CoroutineScope by this@launch {
                    override fun emit(value: ReadableState<T>) {
                        state = value
                    }
                })
            }
        }
        override fun deactivate() {
            job?.cancel()
            job = null
        }
    }
}

fun <T> CoroutineScope.asyncReadable(action: suspend () -> T): Readable<T> {
    val prop = LateInitProperty<T>()
    launch {
        prop.value = action()
    }
    return prop
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Deferred<T>.readable() = object : BaseReadable<T>() {
    init {
        this@readable[Job]?.invokeOnCompletion {
            state = if (it == null) ReadableState(getCompleted()) else ReadableState.exception(it as? Exception ?: Exception("Must be exception, not throwable", it))
        }
    }
}
