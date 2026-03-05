// by Claude - iOS devices can't launch the daemon
package com.lightningkite.kiteui.aidriver

internal actual fun tryAutoStartDaemon(port: Int) { /* no-op on iOS */ }
