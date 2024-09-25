package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.lens

data class Validated<T>(
    val value: T,
    val issueNode: IssueNode? = null
) {
    val valid: T get() {
        if(issueNode == null) return value
        fun IssueNode.notWarning(): Boolean {
            if(exception !is WarningException) return true
            return children.any { it.value.notWarning() }
        }
        if(issueNode.notWarning()) throw PathedExceptions(issueNode.toMap())
        else return value
    }
    fun <B> get(key: String, getter: (T)->B): Validated<B> = Validated(
        value = getter(value),
        issueNode = issueNode.get(key)
    )
}

class WarningException(message: String): Exception(message)

class PathedExceptions(
    val map: Map<List<String>, Exception>
): Exception(map.entries.joinToString("\n") { it.key.joinToString(".") + ": " + (it.value.message ?: it.value.toString()) })

data class IssueNode(
    val exception: Exception?,
//    val onlyWarning: Boolean = false,
    val children: Map<String, IssueNode> = mapOf()
) {
    fun toException() = PathedExceptions(toMap())
    fun toMap(destination: MutableMap<List<String>, Exception> = HashMap(), path: List<String> = listOf()): Map<List<String>, Exception> {
        exception?.let { destination[path] = it }
        children.forEach { it.value.toMap(destination, path + it.key) }
        return destination
    }
    companion object {
        val EMPTY = IssueNode(null, mapOf())
    }

}

operator fun IssueNode?.get(key: String): IssueNode? {
    if(this == null) return null
    return children[key]
}

operator fun IssueNode?.plus(other: IssueNode?): IssueNode? {
    if(this == null) return other
    if(other == null) return this
    return IssueNode(
        exception = this.exception ?: other.exception,
        children = run {
            val map = this.children.toMutableMap()
            for((key, value) in other.children) {
                map[key] = map[key]?.plus(value) ?: value
            }
            map
        }
    )
}
operator fun IssueNode?.plus(pair: Pair<List<String>, Exception>): IssueNode {
    if(pair.first.isEmpty()) return IssueNode(pair.second, this?.children ?: mapOf())
    return IssueNode(this?.exception, run {
        if(this == null) mapOf(pair.first[0] to (null as IssueNode?).plus(pair.first.drop(1) to pair.second))
        else {
            this.children + (pair.first[0] to children[pair.first[0]].plus(pair.first.drop(1) to pair.second))
        }
    })
}


////

val <T> Writable<Validated<T>>.direct: Writable<T> get() = object: Writable<T> {
    override val state: ReadableState<T> = this@direct.state.map { it.value }
    override fun addListener(listener: () -> Unit): () -> Unit = this@direct.addListener(listener)
    override suspend fun set(value: T) {
        TODO("Not yet implemented")
    }
    override suspend fun reportSetException(exception: Exception) {
        val v = this@direct()
        this@direct.set(Validated(v.value, v.issueNode + IssueNode(exception)))
    }
}
