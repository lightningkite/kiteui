package com.lightningkite.kiteui

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.test.Test

class DTest {
    interface Tester {
        var value: Int
    }

    class Delegate<T> : ReadWriteProperty<T, Int> {
        var mine = 0

        override fun getValue(thisRef: T, property: KProperty<*>): Int = mine
        override fun setValue(thisRef: T, property: KProperty<*>, value: Int) {
            mine = value
        }

        operator fun plusAssign(value: Int) {
            println("Called plusAssign")
            mine = value
        }
    }

    @Test
    fun testDelegatePlusAssign() {
        val a = object : Tester {
            override var value: Int by Delegate()
        }

        a.value += 1
    }
}