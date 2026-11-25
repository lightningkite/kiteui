package com.lightningkite.kiteui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Runs [block] on [Dispatchers.IO] and returns on the main thread, whatever the caller's dispatcher.
 *
 * `rememberSuspending` and `remember` run `Unconfined`, so they carry on on whichever thread resumes them: without
 * the hop back they would continue on an engine thread and update reactive state and views from it.
 */
internal suspend fun <T> ioThenMain(block: suspend () -> T): T =
    withContext(Dispatchers.Main.immediate) { withContext(Dispatchers.IO) { block() } }
