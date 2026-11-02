package com.lightningkite.kiteui

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.errorText
import kotlin.test.Test
import kotlin.test.assertEquals

/** Errors walk from the element that raised them up through enclosing containers, so a handler only sees its own subtree. */
class ExceptionScopeTest {
    private val caught = ArrayList<String>()

    private fun ElementContext.catchAs(name: String, priority: Float = 1f) {
        exceptionHandlers += ExceptionHandler(priority) { e ->
            caught += "$name:${e.message}"
            { caught -= "$name:${e.message}" }
        }
    }

    private fun ElementTreeHandle.raise(elementName: String, message: String) {
        val element = findByName(elementName)!!
        element.context.handleException(
            Exception(message),
            ExceptionHandler.Metadata(source = element, process = null, foregroundProcess = null),
        )
    }

    @Test
    fun siblingContainersDoNotShareHandlers() {
        val tree = elementTree {
            col { debugName = "left"; context.catchAs("left"); text { debugName = "leftChild" } }
            col { debugName = "right"; context.catchAs("right"); text { debugName = "rightChild" } }
        }
        try {
            tree.raise("rightChild", "a")
            tree.raise("leftChild", "b")
            assertEquals(listOf("right:a", "left:b"), caught)
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun errorFallsThroughToEnclosingContainerThenRoot() {
        val tree = elementTree {
            col {
                debugName = "outer"
                context.catchAs("outer")
                col { debugName = "inner"; text { debugName = "deep" } }
            }
            text { debugName = "bare" }
        }
        tree.context.catchAs("root", priority = 0f)
        try {
            tree.raise("deep", "a")
            tree.raise("bare", "b")
            assertEquals(listOf("outer:a", "root:b"), caught)
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun errorTextOnlySeesErrorsFromItsOwnContainer() {
        val tree = elementTree {
            col { debugName = "form"; errorText(); text { debugName = "formChild" } }
            text { debugName = "outside" }
        }
        tree.context.catchAs("root", priority = 0f)
        try {
            tree.raise("outside", "a")
            assertEquals(listOf("root:a"), caught)
            tree.raise("formChild", "b")
            assertEquals(listOf("root:a"), caught, "errorText should have caught b")
        } finally {
            tree.shutdown()
        }
    }
}
