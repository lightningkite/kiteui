//package com.lightningkite.kiteui
//
//import com.lightningkite.kiteui.*
//import com.lightningkite.kiteui.reactive.*
//import com.lightningkite.lightningdb.*
//import com.lightningkite.lightningserver.db.ClientModelRestEndpoints
//import com.lightningkite.now
//import kotlinx.datetime.Instant
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertFalse
//import kotlin.test.assertTrue
//import kotlin.time.Duration
//import kotlin.time.Duration.Companion.milliseconds
//import kotlin.time.Duration.Companion.minutes
//import kotlin.time.Duration.Companion.seconds
//
//class ConnectivityGateTest {
//    @Test fun test() = timeTravelTest { clock ->
//        val gate = ConnectivityGate {
//            clock += it.milliseconds
//            println("Clock advanced to ${clock.seconds}")
//        }
//        testContext {
//            launch {
//                var fail = 5
//                gate.run("Test") {
//                    println("Attempt at ${now()}")
//                    if(fail-- > 0) throw ConnectionException("Darn...")
//                    println("A")
//                }
//            }
//        }
//    }
//
//    @Test fun waitUntilConnect() = timeTravelTest { clock ->
//        val gate = ConnectivityGate {
//            clock += it.milliseconds
//            println("Clock advanced to ${clock.seconds}")
//        }
//        val onClose = ArrayList<(Short) -> Unit>()
//        val onOpen = ArrayList<() -> Unit>()
//        val ws = object: WebSocket {
//            override fun close(code: Short, reason: String) {
//            }
//
//            override fun onBinaryMessage(action: (Blob) -> Unit) {
//            }
//
//            override fun onClose(action: (Short) -> Unit) {
//                onClose.add(action)
//            }
//
//            override fun onMessage(action: (String) -> Unit) {
//            }
//
//            override fun onOpen(action: () -> Unit) {
//                onOpen.add(action)
//            }
//
//            override fun send(data: Blob) {
//            }
//
//            override fun send(data: String) {
//            }
//        }
//
//        testContext {
//            var connection = false
//            launch {
//                ws.waitUntilConnect(gate.delay)
//                connection = true
//            }
//            launch {
//                onOpen.invokeAllSafe()
//                clock += 1.minutes
//            }
//            assertTrue(connection)
//        }
//    }
//
//    @Test fun waitUntilConnectFail() = timeTravelTest { clock ->
//        val gate = ConnectivityGate {
//            clock += it.milliseconds
//            println("Clock advanced to ${clock.seconds}")
//        }
//        val onClose = ArrayList<(Short) -> Unit>()
//        val onOpen = ArrayList<() -> Unit>()
//        val ws = object: WebSocket {
//            override fun close(code: Short, reason: String) {
//            }
//
//            override fun onBinaryMessage(action: (Blob) -> Unit) {
//            }
//
//            override fun onClose(action: (Short) -> Unit) {
//                onClose.add(action)
//            }
//
//            override fun onMessage(action: (String) -> Unit) {
//            }
//
//            override fun onOpen(action: () -> Unit) {
//                onOpen.add(action)
//            }
//
//            override fun send(data: Blob) {
//            }
//
//            override fun send(data: String) {
//            }
//        }
//
//        testContext {
//            var connectionFail = false
//            launch {
//                try {
//                    ws.waitUntilConnect(gate.delay)
//                } catch(e: ConnectionException) {
//                    connectionFail = true
//                }
//            }
//            launch {
//                clock += 100.milliseconds
//                onClose.forEach { it(1000) }
//            }
//            assertTrue(connectionFail)
//        }
//    }
//
//    @Test fun retrySocket() = timeTravelTest { clock ->
//        val gate = ConnectivityGate {
//            clock += it.milliseconds
//            println("Clock advanced to ${clock.seconds}")
//        }
//        val socket = retryWebsocket(
//            underlyingSocket = {
//                object: WebSocket {
//                    override fun close(code: Short, reason: String) {
//                        TODO("Not yet implemented")
//                    }
//
//                    override fun onBinaryMessage(action: (Blob) -> Unit) {
//                        TODO("Not yet implemented")
//                    }
//
//                    override fun onClose(action: (Short) -> Unit) {
//                        TODO("Not yet implemented")
//                    }
//
//                    override fun onMessage(action: (String) -> Unit) {
//                        TODO("Not yet implemented")
//                    }
//
//                    override fun onOpen(action: () -> Unit) {
//                        TODO("Not yet implemented")
//                    }
//
//                    override fun send(data: Blob) {
//                        TODO("Not yet implemented")
//                    }
//
//                    override fun send(data: String) {
//                        TODO("Not yet implemented")
//                    }
//
//                }
//            },
//            pingTime = 3000,
//            gate = gate
//        )
//
//    }
//}