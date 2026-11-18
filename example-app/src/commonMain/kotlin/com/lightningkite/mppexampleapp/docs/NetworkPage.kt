package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.widgets.code
import com.lightningkite.reactive.core.*

@Routable("docs/network")
object NetworkPage : DocPage {
    override val covers: List<String> = listOf(
        "network", "API", "fetch", "HTTP", "requests", "REST",
        "JSON", "serialization", "authentication"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Network and API Integration") {
                text("Learn how to make HTTP requests and integrate with REST APIs in KiteUI.")

                space()

                titledSection("The fetch API") {
                    text("KiteUI provides a lightweight, multiplatform fetch API for HTTP requests:")

                    space()
                    code {
                        content = """
                            val response: RequestResponse = fetch("https://api.example.com/data")
                            val jsonText = response.text()
                        """.trimIndent()
                    }

                    space()
                    text("Key features:")
                    card.col {
                        text("• Multiplatform - works on all KiteUI targets")
                        text("• Lightweight - minimal bundle size impact")
                        text("• Suspend-based - integrates with Kotlin coroutines")
                        text("• Progress tracking - monitor upload/download progress")
                    }
                }

                space()

                titledSection("Basic GET Request") {
                    text("Fetch data from an API endpoint:")

                    space()
                    code {
                        content = """
                            val userData = rememberSuspending {
                                val response = fetch("https://api.example.com/user/123")
                                Json.decodeFromString<User>(response.text())
                            }

                            text {
                                ::content { "User: ${'$'}{userData().name}" }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("POST Request with Body") {
                    text("Send a JSON body; type sets the Content-Type:")

                    space()
                    code {
                        content = """
                            @Serializable data class LoginRequest(val email: String, val password: String)
                            @Serializable data class LoginResponse(val token: String, val userId: String)

                            button {
                                text("Login")
                                onClick {
                                    val response = fetch(
                                        url = "https://api.example.com/login",
                                        method = HttpMethod.POST,
                                        type = "application/json",
                                        body = Json.encodeToString(LoginRequest(email(), password())),
                                    )
                                    val result = Json.decodeFromString<LoginResponse>(response.text())
                                    authToken.value = result.token
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Request Headers") {
                    text("Add custom headers like authentication tokens:")

                    space()
                    code {
                        content = """
                            val response = fetch(
                                url = "https://api.example.com/protected",
                                headers = HttpHeaders("Authorization" to "Bearer ${'$'}token"),
                            )
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Error Handling") {
                    text("fetch throws a FetchException (ConnectionException or RequestBlockedException) when the request can't complete. It does not throw for an HTTP error status: check response.ok yourself. Errors thrown while loading reach the views that read the value, and are shown by the exception handlers:")

                    space()
                    code {
                        content = """
                            val data = rememberSuspending {
                                val response = fetch("https://api.example.com/data")
                                if (!response.ok) throw IllegalStateException("HTTP ${'$'}{response.status}")
                                Json.decodeFromString<DataModel>(response.text())
                            }

                            col {
                                text { ::content { data().toString() } }
                                errorText()   // show the error here instead of in a dialog
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Query Parameters") {
                    text("Encode each value with encodeURIComponent:")

                    space()
                    code {
                        content = """
                            val url = "https://api.example.com/search" +
                                "?q=${'$'}{encodeURIComponent(searchQuery)}&page=${'$'}page&limit=${'$'}limit"
                            val response = fetch(url)
                        """.trimIndent()
                    }
                }

                space()

                titledSection("File Upload") {
                    text("Ask the user for a file and send it as the request body:")

                    space()
                    code {
                        content = """
                            button {
                                text("Upload Image")
                                onClick {
                                    val file = context.requestFile(listOf("image/*")) ?: return@onClick
                                    val response = fetch(
                                        url = "https://api.example.com/upload",
                                        method = HttpMethod.POST,
                                        body = file,
                                    )
                                    if (!response.ok) throw IllegalStateException("Upload failed: HTTP ${'$'}{response.status}")
                                    context.toast("Upload complete!")
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Progress Tracking") {
                    text("Monitor upload and download progress. The total is -1 when the size isn't known:")

                    space()
                    code {
                        content = """
                            val progress = Signal(0f)

                            button {
                                text("Download")
                                onClick {
                                    val response = fetch(
                                        url = "https://example.com/large-file.zip",
                                        onDownloadProgress = { current, total ->
                                            if (total > 0) progress.value = current.toFloat() / total
                                        },
                                    )
                                    // Process downloaded file
                                }
                            }

                            text {
                                ::content { "Progress: ${'$'}{(progress() * 100).toInt()}%" }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("JSON Serialization") {
                    text("Decode responses into @Serializable classes at the boundary:")

                    space()
                    code {
                        content = """
                            @Serializable
                            data class User(
                                val id: Int,
                                val name: String,
                                val email: String,
                                val createdAt: Instant,
                            )

                            val user = Json.decodeFromString<User>(response.text())
                        """.trimIndent()
                    }
                }

                space()

                titledSection("API Client Pattern") {
                    text("Create a reusable API client:")

                    space()
                    code {
                        content = """
                            class ApiClient(
                                private val baseUrl: String,
                                private val authToken: Signal<String?>,
                            ) {
                                private fun headers() = HttpHeaders {
                                    authToken.value?.let { set("Authorization", "Bearer ${'$'}it") }
                                }

                                private suspend fun check(response: RequestResponse): RequestResponse {
                                    if (!response.ok) throw IllegalStateException("HTTP ${'$'}{response.status}")
                                    return response
                                }

                                suspend fun getUser(id: Int): User {
                                    val response = check(fetch("${'$'}baseUrl/users/${'$'}id", headers = headers()))
                                    return Json.decodeFromString(response.text())
                                }

                                suspend fun updateUser(user: User): User {
                                    val response = check(fetch(
                                        url = "${'$'}baseUrl/users/${'$'}{user.id}",
                                        method = HttpMethod.PUT,
                                        headers = headers(),
                                        type = "application/json",
                                        body = Json.encodeToString(user),
                                    ))
                                    return Json.decodeFromString(response.text())
                                }
                            }

                            // Usage
                            val api = ApiClient("https://api.example.com", authToken)
                            val user = rememberSuspending { api.getUser(123) }
                        """.trimIndent()
                    }

                    space()
                    text("With a Lightning Server backend, use its generated SDK and ModelCache instead of writing a client by hand.")
                }

                space()

                titledSection("Best Practices") {
                    card.col {
                        text("✓ Check response.ok: fetch doesn't throw for HTTP error statuses")
                        text("✓ Let errors propagate to the exception handlers instead of catching them to show a toast")
                        text("✓ Load with rememberSuspending so views show loading states automatically")
                        text("✓ Decode JSON into @Serializable classes")
                        text("✓ Use HTTPS for production APIs")
                    }
                }

                space()

                titledSection("Common Patterns Summary") {
                    card.col {
                        h5("Simple GET:")
                        code {
                            content = """
                                val data = rememberSuspending {
                                    fetch("https://api.example.com/data").text()
                                }
                            """.trimIndent()
                        }

                        space()
                        h5("POST with JSON:")
                        code {
                            content = """
                                fetch(
                                    url = url,
                                    method = HttpMethod.POST,
                                    type = "application/json",
                                    body = Json.encodeToString(data),
                                )
                            """.trimIndent()
                        }

                        space()
                        h5("Authenticated Request:")
                        code {
                            content = """
                                fetch(url, headers = HttpHeaders("Authorization" to "Bearer ${'$'}token"))
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}
