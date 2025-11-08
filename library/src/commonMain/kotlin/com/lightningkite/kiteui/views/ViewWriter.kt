@file:Suppress("NOTHING_TO_INLINE")

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

abstract class ViewWriter: CoroutineScopeHelpers() {
    abstract val context: RContext
    abstract fun willAddChild(view: RView)
    abstract fun addChild(view: RView)
    abstract val representsView: RView?

    fun split(): ViewWriter {
        val r = object : ViewWriter(), CoroutineScope by this {
            override val representsView: RView? = this@ViewWriter.representsView
            override val context: RContext = this@ViewWriter.context.split()
            override fun addChild(view: RView) = this@ViewWriter.addChild(view)
            override fun willAddChild(view: RView) = this@ViewWriter.willAddChild(view)
        }
        return r
    }

    inline fun produceOne(action: ViewWriter.()->Unit): RView {
        var output: RView? = null
        object : ViewWriter() {
            override val representsView: RView? = this@ViewWriter.representsView
            override val coroutineContext: CoroutineContext get() = this@ViewWriter.coroutineContext
            override val context: RContext get() = this@ViewWriter.context
            override fun willAddChild(view: RView) = this@ViewWriter.willAddChild(view)
            override fun addChild(view: RView) {
                if(output != null) throw IllegalStateException("Produced more than one view at this layer, but only one was expected.")
                output = view
                this@ViewWriter.addChild(view)
            }
        }.let(action)
        return output ?: throw IllegalStateException("Produced no views at this layer, but expected one.")
    }
    inline fun produceOneMaybe(action: ViewWriter.()->Unit): RView? {
        var output: RView? = null
        object : ViewWriter() {
            override val representsView: RView? = this@ViewWriter.representsView
            override val coroutineContext: CoroutineContext get() = this@ViewWriter.coroutineContext
            override val context: RContext get() = this@ViewWriter.context
            override fun willAddChild(view: RView) = this@ViewWriter.willAddChild(view)
            override fun addChild(view: RView) {
                if(output != null) throw IllegalStateException("Produced more than one view at this layer, but only one was expected.")
                output = view
                this@ViewWriter.addChild(view)
            }
        }.let(action)
        return output
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T : RView> write(view: T, setup: T.() -> Unit): T {
        contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
        willAddChild(view)
        setup(view)
        view.postSetup()
        addChild(view)
        return view
    }

    class BeforeSetup(
        val base: ViewWriter,
        val action: RView.()->Unit
    ): ViewWriter() {
        override val representsView: RView? = base.representsView
        override val context: RContext get() = base.context
        override val coroutineContext: CoroutineContext get() = base.coroutineContext
        override fun willAddChild(view: RView) {
            base.willAddChild(view)
            view.action()
        }
        override fun addChild(view: RView) {
            base.addChild(view)
        }
    }

    fun beforeNextElementSetup(action: RView.()->Unit): ViewWriter = BeforeSetup(this, action)

    @ViewModifierDsl3
    val Theme.onNext: ViewWrapper get() {
        return beforeNextElementSetup {
            themeChoice = ThemeDerivation { this@onNext.withBack }
        }
    }

    @ViewModifierDsl3
    val Theme.setAsBaseOnNext: ViewWrapper get() {
        return beforeNextElementSetup {
            themeChoice = ThemeDerivation { this@setAsBaseOnNext.withoutBack }
        }
    }

    @ViewModifierDsl3
    val ThemeDerivation.onNext: ViewWrapper get() {
        return beforeNextElementSetup {
            val old = themeChoice
            themeChoice = old + this@onNext
        }
    }

    fun apply(themeDerivation: ThemeDerivation) = beforeNextElementSetup {
        val old = themeChoice
        themeChoice = old + themeDerivation
    }
}
