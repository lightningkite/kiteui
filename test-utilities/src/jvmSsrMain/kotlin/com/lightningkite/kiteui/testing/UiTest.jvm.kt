package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWithChildren
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * The main thread of every [uiTest] in this JVM.  The element tree and reactive code aren't thread-safe, so as on
 * Android and in the browser, they run on one thread: work resuming from a server or a timer queues onto it.
 *
 * Unlike Android's, `Dispatchers.Main` runs in place when already on this thread, as `Dispatchers.Main.immediate`
 * does.  `AppScope` holds whichever of the two it was given when first used, which in a test JVM depends on which
 * test ran first; this way it behaves the same either way.
 */
private object UiThread : MainCoroutineDispatcher() {
    private val queue = LinkedBlockingQueue<Runnable>()

    /** What escaped a task on this thread, which on a device would crash the app.  The running test fails with it. */
    val crashes = ConcurrentLinkedQueue<Throwable>()

    private val thread = Thread({
        while (true) {
            val task = queue.take()
            try { task.run() } catch (e: Throwable) { crashes += e }
        }
    }, "uiTest main").apply { isDaemon = true; start() }

    override val immediate: MainCoroutineDispatcher get() = this
    override fun isDispatchNeeded(context: CoroutineContext): Boolean = Thread.currentThread() !== thread
    override fun dispatch(context: CoroutineContext, block: Runnable) = queue.put(block)
    override fun toString(): String = "uiTest main"

    /**
     * Makes this [Dispatchers.Main], once for the JVM rather than per test: leftover work from an earlier test reads
     * `Dispatchers.Main` from other threads, and kotlinx-coroutines-test fails when that overlaps setting it.  Again
     * only if another test has set or reset it since.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun install() {
        // Dispatchers.Main throws when nothing is set: the case of the first uiTest, or one after a resetMain.
        val current = try { Dispatchers.Main.immediate } catch (_: IllegalStateException) { null }
        if (current !== this) Dispatchers.setMain(this)
    }
}

@OptIn(InternalKiteUi::class, OverrideOnly::class)
actual fun uiTest(
    mockExternalServices: MockExternalServices?,
    content: ViewWriter.() -> Unit,
    block: suspend UiTestScope.() -> Unit,
) {
    UiThread.install()
    runBlocking(UiThread) {
        val context = ElementContext("/")
        val root = Frame(context)
        var testFailure: Throwable? = null
        try {
            context.overlayFrame = root
            if (mockExternalServices != null) {
                context.externalServices = mockExternalServices
            }
            content(root)
            root.onStartup()

            val backend = LocalUiTestBackend(
                root = { root },
                navigator = { null },
            )
            UiTestScope(backend).block()
        } catch (e: Throwable) {
            testFailure = e
            throw e
        } finally {
            // Shutting down only cancels the page's coroutines.  Its reactive code stops listening (to a session, a
            // live query) once they have finished, and one waiting on a request finishes only when the request does,
            // so wait for them, or the page keeps working into the next test.  An element's job isn't a child of its
            // container's, so gather them before shutdown forgets the tree.
            fun Element.jobs(): List<Job> =
                listOf(coroutineContext.job) + (this as? ElementWithChildren)?.children.orEmpty().flatMap { it.jobs() }
            val jobs = root.jobs()
            root.onShutdown()
            val finished = withTimeoutOrNull(10.seconds) { jobs.joinAll() }

            val stuck = if (finished == null) IllegalStateException("The page's coroutines were still running 10 s after it was shut down") else null
            val problems = listOfNotNull(stuck) + generateSequence { UiThread.crashes.poll() }
            val failure = testFailure ?: problems.firstOrNull()
            problems.filter { it !== failure }.forEach { failure?.addSuppressed(it) }
            if (testFailure == null && failure != null) throw failure
        }
    }
}
