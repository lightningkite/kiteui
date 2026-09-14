package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.DriverActionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Test backend that sends commands via HTTP POST to a running AI driver daemon.
 * The daemon forwards commands to the connected app's WebSocket.
 *
 * Uses tab separation between app ID and command so values containing spaces are preserved.
 * Throws [DriverActionException] on error responses for parity with [LocalUiTestBackend].
 */
class RemoteUiTestBackend(
    val appId: String,
    val host: String = "127.0.0.1",
    val port: Int = 7474,
) : UiTestBackend {
    override suspend fun command(command: String): String = withContext(Dispatchers.IO) {
        val url = URL("http://$host:$port")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 30000
            conn.outputStream.bufferedWriter().use { it.write("$appId\t$command") }
            val code = conn.responseCode
            val result = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw DriverActionException("Server error: $err")
            }
            if (result.startsWith("ERROR:")) throw DriverActionException(result.removePrefix("ERROR:").trim())
            result
        } finally {
            conn.disconnect()
        }
    }
}
