package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.utils.commaString
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

infix fun <T> Writable<T>.equalToDynamic(value: suspend () -> T): Writable<Boolean> = shared {
    await() == value()
}.withWrite {
    if (it) set(value())
}

@JsName("invokeAllSafeMutable")
@JvmName("invokeAllSafeMutable")
fun MutableList<() -> Unit>.invokeAllSafe() = toList().invokeAllSafe()
fun List<() -> Unit>.invokeAllSafe() = forEach {
    try {
        it()
    } catch (e: Exception) {
        e.printStackTrace2()
    }
}

infix fun <T> Writable<T>.bind(master: Writable<T>) {
    with(CalculationContextStack.current()) {
        launch {
            this@bind.set(master.await())
        }
        var setting = false
        master.addListener {
            if (setting) return@addListener
            setting = true
            launch {
                this@bind.set(master.await())
            }
            setting = false
        }.also { onRemove(it) }
        this@bind.addListener {
            if (setting) return@addListener
            setting = true
            launch {
                master.set(this@bind.await())
            }
            setting = false
        }.also { onRemove(it) }
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