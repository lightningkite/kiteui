package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.SecurePlatformStorage
import kotlin.time.Clock

private const val launchValueKey = "example.secureStorageCheck"

// In plain storage, beside it: what a launch wrote, to compare with what secure storage gives back on the next one.
private const val launchValueWrittenKey = "example.secureStorageCheck.written"

// Read on the first run in this process, before that run writes: what an earlier launch left.
private val storedBeforeThisLaunch: String? = SecurePlatformStorage.get(launchValueKey)
private val writtenBeforeThisLaunch: String? = PlatformStorage.get(launchValueWrittenKey)

actual fun secureStorageChecks(): List<Check> {
    val stored = storedBeforeThisLaunch
    val written = writtenBeforeThisLaunch
    val persistence = when {
        written == null -> Check(
            "Value kept from an earlier launch", Outcome.NotYet,
            "No earlier launch has written one. Quit the app completely and run the checks again."
        )
        stored == written -> Check("Value kept from an earlier launch", Outcome.Pass, "Found \"$stored\" on launch.")
        else -> Check(
            "Value kept from an earlier launch", Outcome.Fail,
            "An earlier launch wrote \"$written\", but this launch found ${if (stored == null) "nothing" else "\"$stored\""}."
        )
    }

    val roundTrip = "round trip at ${Clock.System.now()}"
    SecurePlatformStorage.set("example.roundTrip", roundTrip)
    val readBack = SecurePlatformStorage.get("example.roundTrip")
    SecurePlatformStorage.remove("example.roundTrip")
    val afterRemove = SecurePlatformStorage.get("example.roundTrip")

    val forNextLaunch = "written at ${Clock.System.now()}"
    SecurePlatformStorage.set(launchValueKey, forNextLaunch, ensureOnDisk = true)
    PlatformStorage.set(launchValueWrittenKey, forNextLaunch, ensureOnDisk = true)

    return listOf(
        Check(
            "Set, get, remove",
            if (readBack == roundTrip && afterRemove == null) Outcome.Pass else Outcome.Fail,
            "Read back \"$readBack\"; after remove, ${afterRemove ?: "nothing"}."
        ),
        persistence,
    )
}
