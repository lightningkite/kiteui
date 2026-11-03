package com.lightningkite.kiteui.views.direct

// Chrome makes a notch 100px on Windows and Linux.  On macOS, AWT reports a tenth of the system's pixel deltas (Compose
// scrolls 10dp a unit there too); Chrome makes a mouse notch 40px there, so a mouse wheel is slower here than in Chrome.
internal actual val wheelPixelsPerUnit: Float = if (System.getProperty("os.name").startsWith("Mac")) 10f else 100f
