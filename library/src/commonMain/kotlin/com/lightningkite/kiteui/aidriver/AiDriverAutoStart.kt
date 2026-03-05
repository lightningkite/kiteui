// by Claude - attempt to auto-start the AI driver daemon if not running
package com.lightningkite.kiteui.aidriver

/**
 * Try to start the AI driver daemon. No-op on platforms that can't launch processes
 * (browser JS, Android, iOS). On JVM, looks for `~/.kiteui/bin/kiteui-drive` and
 * invokes `kiteui-drive start`.
 */
internal expect fun tryAutoStartDaemon(port: Int)
