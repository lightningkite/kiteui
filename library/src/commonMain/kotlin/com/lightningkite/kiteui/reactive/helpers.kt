package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.utils.commaString
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import kotlin.js.JsName
import kotlin.jvm.JvmName

infix fun <T> Writable<T>.equalTo(value: T): Writable<Boolean> = object : Writable<Boolean> {
    override val state: ReadableState<Boolean> get() = this@equalTo.state.map { it == value }
    override fun addListener(listener: () -> Unit): () -> Unit = this@equalTo.addListener(listener)
    val target = value
    override suspend fun set(value: Boolean) {
        if (value) this@equalTo.set(target)
    }
}

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

infix fun <T> Writable<Set<T>>.contains(value: T): Writable<Boolean> = shared { value in await() }.withWrite { on ->
    if (on) this@contains.set(this@contains.await() + value)
    else this@contains.set(this@contains.await() - value)
}

fun <T> Readable<T>.withWrite(action: suspend Readable<T>.(T) -> Unit): Writable<T> =
    object : Writable<T>, Readable<T> by this {
        override suspend fun set(value: T) {
            action(this@withWrite, value)
        }
    }

fun <T : Any> Writable<T>.nullable(): Writable<T?> = shared { this@nullable.await() }
    .withWrite { it?.let { this@nullable set it } }

fun <T : Any> Writable<T?>.notNull(default: T): Writable<T> = shared { this@notNull.await() ?: default }
    .withWrite { this@notNull set it }

@JvmName("writableStringAsDouble")
fun Writable<String>.asDouble(): Writable<Double?> =
    shared { this@asDouble.await().filter { it.isDigit() || it == '.' }.toDoubleOrNull() }
        .withWrite { this@asDouble set (it?.commaString() ?: "") }

@JvmName("immediateWritableStringAsDouble")
fun ImmediateWritable<String>.asDouble(): ImmediateWritable<Double?> = object : ImmediateWritable<Double?> {
    override suspend fun set(value: Double?) {
        this@asDouble.set(value?.commaString() ?: "")
    }

    override var value: Double?
        get() = this@asDouble.value.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        set(value) {
            this@asDouble.value = (value?.commaString() ?: "")
        }

    override fun addListener(listener: () -> Unit): () -> Unit = this@asDouble.addListener(listener)
}

@JvmName("writableIntAsDouble")
fun Writable<Int?>.asDouble(): Writable<Double?> = shared { this@asDouble.await()?.toDouble() }
    .withWrite { this@asDouble set it?.toInt() }

@JvmName("writableIntAsString")
fun Writable<Int>.asString(): Writable<String> = shared { this@asString.await().toString() }
    .withWrite { it.toIntOrNull()?.let { this@asString.set(it) } }

@JvmName("writableLongAsString")
fun Writable<Long>.asString(): Writable<String> = shared { this@asString.await().toString() }
    .withWrite { it.toLongOrNull()?.let { this@asString.set(it) } }

@JvmName("writableFloatAsString")
fun Writable<Float>.asString(): Writable<String> = shared { this@asString.await().toString() }
    .withWrite { it.toFloatOrNull()?.let { this@asString.set(it) } }

@JvmName("writableDoubleAsString")
fun Writable<Double>.asString(): Writable<String> = shared { this@asString.await().toString() }
    .withWrite { it.toDoubleOrNull()?.let { this@asString.set(it) } }

@JvmName("writableIntNullableAsString")
fun Writable<Int?>.asString(): Writable<String> = shared { this@asString.await()?.toString() ?: "" }
    .withWrite { this@asString.set(it.toIntOrNull()) }

@JvmName("writableLongNullableAsString")
fun Writable<Long?>.asString(): Writable<String> = shared { this@asString.await()?.toString() ?: "" }
    .withWrite { this@asString.set(it.toLongOrNull()) }

@JvmName("writableFloatNullableAsString")
fun Writable<Float?>.asString(): Writable<String> = shared { this@asString.await()?.toString() ?: "" }
    .withWrite { this@asString.set(it.toFloatOrNull()) }

@JvmName("writableDoubleNullableAsString")
fun Writable<Double?>.asString(): Writable<String> = shared { this@asString.await()?.toString() ?: "" }
    .withWrite { this@asString.set(it.toDoubleOrNull()) }

suspend infix fun <T> Writable<T>.modify(action: suspend (T) -> T) {
    set(action(await()))
}

fun CalculationContext.use(resourceUse: ResourceUse) {
    val x = resourceUse.start()
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
