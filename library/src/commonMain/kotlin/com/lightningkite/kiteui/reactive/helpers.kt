package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.utils.commaString
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.time.Duration

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

infix fun <T> Writable<T>.bind(master: Writable<T>) {
    with(CalculationContextStack.current()) {
        var setting = false
        launch {
            this@bind.set(master.await())
            master.addListener {
                if (setting) return@addListener
                master.state.onSuccess {
                    setting = true
                    this@with.launch {
                        this@bind.set(it)
                        setting = false
                    }
                }
            }.also { onRemove(it) }
            this@bind.addListener {
                if (setting) return@addListener
                this@bind.state.onSuccess {
                    setting = true
                    this@with.launch {
                        master.set(it)
                        setting = false
                    }
                }
            }.also { onRemove(it) }
        }

    }
}

infix fun <T> ImmediateWritable<T>.bind(master: Writable<T>) {
    with(CalculationContextStack.current()) {
        var setting = false
        launch {
            this@bind.set(master.await())
            master.addListener {
                if (setting) return@addListener
                master.state.onSuccess {
                    setting = true
                    this@bind.value = (it)
                    setting = false
                }
            }.also { onRemove(it) }
            this@bind.addListener {
                if (setting) return@addListener
                this@bind.state.onSuccess {
                    setting = true
                    this@with.launch {
                        master.set(it)
                        setting = false
                    }
                }
            }.also { onRemove(it) }
        }

    }
}

infix fun <T> Writable<T>.bind(master: ImmediateWritable<T>) {
    with(CalculationContextStack.current()) {
        var setting = false
        launch {
            this@bind.set(master.value)
            master.addListener {
                if (setting) return@addListener
                master.state.onSuccess {
                    setting = true
                    this@with.launch {
                        this@bind.set(it)
                        setting = false
                    }

                }
            }.also { onRemove(it) }
            this@bind.addListener {
                if (setting) return@addListener
                this@bind.state.onSuccess {
                    setting = true
                    master.value = it
                    setting = false

                }
            }.also { onRemove(it) }
        }
    }
}

infix fun <T> ImmediateWritable<T>.bind(master: ImmediateWritable<T>) {
    with(CalculationContextStack.current()) {
        var setting = false
        this@bind.value = master.value
        master.addListener {
            if (setting) {
                return@addListener
            }
            master.state.onSuccess {
                setting = true
                this@bind.value = it
                setting = false

            }
        }.also { onRemove(it) }
        this@bind.addListener {
            if (setting) {
                return@addListener
            }
            this@bind.state.onSuccess {
                setting = true
                master.value = it
                setting = false
            }
        }.also { onRemove(it) }
    }
}

fun <T> Readable<T>.withWrite(action: suspend Readable<T>.(T) -> Unit): Writable<T> =
    object : Writable<T>, Readable<T> by this {
        override suspend fun set(value: T) {
            action(this@withWrite, value)
        }
    }

fun <T, WRITE : Writable<T>> WRITE.interceptWrite(action: suspend WRITE.(T) -> Unit): Writable<T> =
    object : Writable<T>, Readable<T> by this {
        override suspend fun set(value: T) {
            action(this@interceptWrite, value)
        }
    }

// Lenses
infix fun <T> Writable<T>.equalTo(value: T): Writable<Boolean> = lens(
    get = { it == value },
    modify = { o, it ->  if(it) value else o }
)
infix fun <T> Writable<Set<T>>.contains(value: T): Writable<Boolean> = lens(
    get = { value in it },
    modify = { set, bool -> if (bool) set + value else set - value }
)

fun <T : Any> Writable<T>.nullable(): Writable<T?> =
    object : Readable<T?> by this, Writable<T?> {
        override suspend fun set(value: T?) {
            if (value != null) this@nullable.set(value)
        }
    }

fun <T : Any> Writable<T?>.notNull(default: T): Writable<T> = lens(
    get = { it ?: default },
    set = { it }
)

val <T: Any> Writable<T?>.waitForNotNull: Writable<T> get() =
    object : Writable<T>, Readable<T> by (this as Readable<T?>).waitForNotNull {
        override suspend fun set(value: T) = this@waitForNotNull.set(value)
    }

fun Writable<String?>.nullToBlank(): Writable<String> = lens(
    get = { it ?: "" },
    set = { it.takeUnless { it.isBlank() } }
)

@JvmName("writableStringAsDouble") fun Writable<String>.asDouble(): Writable<Double?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toDoubleOrNull() }, set = { it?.commaString() ?: "" })
@JvmName("writableStringAsFloat") fun Writable<String>.asFloat(): Writable<Float?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toFloatOrNull() }, set = { it?.toDouble()?.commaString() ?: "" })
@JvmName("writableStringAsByte") fun Writable<String>.asByte(): Writable<Byte?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toByteOrNull() }, set = { it?.toInt()?.commaString() ?: "" })
@JvmName("writableStringAsShort") fun Writable<String>.asShort(): Writable<Short?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toShortOrNull() }, set = { it?.toInt()?.commaString() ?: "" })
@JvmName("writableStringAsInt") fun Writable<String>.asInt(): Writable<Int?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toIntOrNull() }, set = { it?.commaString() ?: "" })
@JvmName("writableStringAsLong") fun Writable<String>.asLong(): Writable<Long?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toLongOrNull() }, set = { it?.commaString() ?: "" })
@JvmName("writableStringAsByteHex") fun Writable<String>.asByteHex(): Writable<Byte?> = lens(get = { it.toByteOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsUByteHex") fun Writable<String>.asUByteHex(): Writable<UByte?> = lens(get = { it.toUByteOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsShortHex") fun Writable<String>.asShortHex(): Writable<Short?> = lens(get = { it.toShortOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsUShortHex") fun Writable<String>.asUShortHex(): Writable<UShort?> = lens(get = { it.toUShortOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsIntHex") fun Writable<String>.asIntHex(): Writable<Int?> = lens(get = { it.toIntOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsUIntHex") fun Writable<String>.asUIntHex(): Writable<UInt?> = lens(get = { it.toUIntOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsLongHex") fun Writable<String>.asLongHex(): Writable<Long?> = lens(get = { it.toLongOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsULongHex") fun Writable<String>.asULongHex(): Writable<ULong?> = lens(get = { it.toULongOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableIntAsDoubleNullable") fun Writable<Int?>.asDouble(): Writable<Double?> = lens(get = { it?.toDouble() }, set = { it?.toInt() })

@JvmName("writableStringAsDouble") fun ImmediateWritable<String>.asDouble(): ImmediateWritable<Double?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toDoubleOrNull() }, set = { it?.commaString() ?: "" })
@JvmName("writableStringAsFloat") fun ImmediateWritable<String>.asFloat(): ImmediateWritable<Float?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toFloatOrNull() }, set = { it?.toDouble()?.commaString() ?: "" })
@JvmName("writableStringAsByte") fun ImmediateWritable<String>.asByte(): ImmediateWritable<Byte?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toByteOrNull() }, set = { it?.toInt()?.commaString() ?: "" })
@JvmName("writableStringAsShort") fun ImmediateWritable<String>.asShort(): ImmediateWritable<Short?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toShortOrNull() }, set = { it?.toInt()?.commaString() ?: "" })
@JvmName("writableStringAsInt") fun ImmediateWritable<String>.asInt(): ImmediateWritable<Int?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toIntOrNull() }, set = { it?.commaString() ?: "" })
@JvmName("writableStringAsLong") fun ImmediateWritable<String>.asLong(): ImmediateWritable<Long?> = lens(get = { it.filter { it.isDigit() || it == '.' }.toLongOrNull() }, set = { it?.commaString() ?: "" })
@JvmName("writableStringAsByteHex") fun ImmediateWritable<String>.asByteHex(): ImmediateWritable<Byte?> = lens(get = { it.toByteOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsUByteHex") fun ImmediateWritable<String>.asUByteHex(): ImmediateWritable<UByte?> = lens(get = { it.toUByteOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsShortHex") fun ImmediateWritable<String>.asShortHex(): ImmediateWritable<Short?> = lens(get = { it.toShortOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsUShortHex") fun ImmediateWritable<String>.asUShortHex(): ImmediateWritable<UShort?> = lens(get = { it.toUShortOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsIntHex") fun ImmediateWritable<String>.asIntHex(): ImmediateWritable<Int?> = lens(get = { it.toIntOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsUIntHex") fun ImmediateWritable<String>.asUIntHex(): ImmediateWritable<UInt?> = lens(get = { it.toUIntOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsLongHex") fun ImmediateWritable<String>.asLongHex(): ImmediateWritable<Long?> = lens(get = { it.toLongOrNull(16) }, set = { it?.toString(16) ?: "" })
@JvmName("writableStringAsULongHex") fun ImmediateWritable<String>.asULongHex(): ImmediateWritable<ULong?> = lens(get = { it.toULongOrNull(16) }, set = { it?.toString(16) ?: "" })

@JvmName("writableIntAsDoubleNullable") fun ImmediateWritable<Int?>.asDouble():ImmediateWritable<Double?> = lens(get = { it?.toDouble() }, set = { it?.toInt() })

suspend infix fun <T> Writable<T>.modify(action: suspend (T) -> T) { set(action(await())) }
suspend infix fun <T> ImmediateWritable<T>.modify(action: suspend (T) -> T) { value = action(value) }

fun CalculationContext.use(resourceUse: ResourceUse) {
    val x = resourceUse.start()
    onRemove(x)
}

fun <T> Readable<Writable<T>>.flatten(): Writable<T> = shared { this@flatten()() }
    .withWrite { this@flatten.state.onSuccess { s -> s set it } }

interface ReadableEmitter<T> {
    fun emit(value: T)
}
fun <T> CoroutineScope.readable(emitter: suspend ReadableEmitter<T>.() -> Unit): Readable<T> {
    val prop = LateInitProperty<T>()
    launch {
        emitter(object : ReadableEmitter<T> {
            override fun emit(value: T) {
                prop.value = value
            }
        })
    }
    return prop
}
fun <T> CoroutineScope.asyncReadable(action: suspend () -> T): Readable<T> {
    val prop = LateInitProperty<T>()
    launch {
        prop.value = action()
    }
    return prop
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Deferred<T>.readable() = object: BaseReadable<T>() {
    init {
        this@readable[Job]?.invokeOnCompletion {
            state = if(it == null) ReadableState(getCompleted()) else ReadableState.exception(it as? Exception ?: Exception("Must be exception, not throwable", it))
        }
    }
}
