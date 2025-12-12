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

    override fun ViewWriter.render(): Unit = run {
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
                                val json = response.text()
                                Json.decodeFromString<User>(json)
                            }

                            text {
                                ::content { "User: ${'$'}{userData().name}" }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("POST Request with Body") {
                    text("Send data to an API:")

                    space()
                    code {
                        content = """
                            data class LoginRequest(val email: String, val password: String)
                            data class LoginResponse(val token: String, val userId: String)

                            button {
                                text("Login")
                                onClick {
                                    val request = LoginRequest(email(), password())
                                    val json = Json.encodeToString(request)

                                    val response = fetch(
                                        url = "https://api.example.com/login",
                                        method = HttpMethod.POST,
                                        headers = RequestHeaders(
                                            "Content-Type" to "application/json"
                                        ),
                                        body = HttpBody.Text(json)
                                    )

                                    val result = Json.decodeFromString<LoginResponse>(
                                        response.text()
                                    )
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
                                headers = RequestHeaders(
                                    "Authorization" to "Bearer ${'$'}{authToken}",
                                    "Content-Type" to "application/json"
                                )
                            )
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Error Handling") {
                    text("Handle network and HTTP errors:")

                    space()
                    code {
                        content = """
                            val data = rememberSuspending {
                                try {
                                    val response = fetch("https://api.example.com/data")

                                    if (response.status !in 200..299) {
                                        throw Exception("HTTP ${'$'}{response.status}: ${'$'}{response.statusText}")
                                    }

                                    Json.decodeFromString<DataModel>(response.text())
                                } catch (e: Exception) {
                                    // Handle network errors
                                    toast("Error: ${'$'}{e.message}")
                                    throw e
                                }
                            }

                            try {
                                // Display data
                                text { ::content { data().toString() } }
                            } catch (e: Exception) {
                                // Display error state
                                danger.text("Failed to load data")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Query Parameters") {
                    text("Add query parameters to requests:")

                    space()
                    code {
                        content = """
                            val searchQuery = "kotlin"
                            val page = 1
                            val limit = 20

                            val url = "https://api.example.com/search?q=${'$'}searchQuery&page=${'$'}page&limit=${'$'}limit"
                            val response = fetch(url)
                        """.trimIndent()
                    }

                    space()
                    text("Or use a helper function:")
                    code {
                        content = """
                            fun buildUrl(base: String, params: Map<String, String>): String {
                                val query = params.entries.joinToString("&") { (k, v) ->
                                    "${'$'}k=${'$'}{encodeURIComponent(v)}"
                                }
                                return if (query.isEmpty()) base else "${'$'}base?${'$'}query"
                            }

                            val url = buildUrl(
                                "https://api.example.com/search",
                                mapOf("q" to searchQuery, "page" to page.toString())
                            )
                        """.trimIndent()
                    }
                }

                space()

                titledSection("File Upload") {
                    text("Upload files with multipart/form-data:")

                    space()
                    code {
                        content = """
                            button {
                                text("Upload Image")
                                onClick {
                                    // Get file from user
                                    val file = pickFile()

                                    val response = fetch(
                                        url = "https://api.example.com/upload",
                                        method = HttpMethod.POST,
                                        body = HttpBody.FormData(
                                            mapOf(
                                                "file" to file,
                                                "description" to "My uploaded file"
                                            )
                                        )
                                    )

                                    toast("Upload complete!")
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Progress Tracking") {
                    text("Monitor upload and download progress:")

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
                                            progress.value = current.toFloat() / total.toFloat()
                                        }
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
                    text("Use KotlinX Serialization for type-safe JSON handling:")

                    space()
                    code {
                        content = """
                            @Serializable
                            data class User(
                                val id: Int,
                                val name: String,
                                val email: String,
                                val createdAt: String
                            )

                            @Serializable
                            data class ApiResponse<T>(
                                val success: Boolean,
                                val data: T?,
                                val error: String?
                            )

                            // Deserialize response
                            val json = response.text()
                            val apiResponse = Json.decodeFromString<ApiResponse<User>>(json)

                            if (apiResponse.success) {
                                val user = apiResponse.data!!
                                // Use user data
                            } else {
                                // Handle error
                                toast(apiResponse.error ?: "Unknown error")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Authentication Patterns") {
                    h5("Token-based Authentication:")
                    code {
                        content = """
                            // Store auth token
                            val authToken = Signal<String?>(null)

                            // Login
                            suspend fun login(email: String, password: String) {
                                val response = fetch(
                                    url = "https://api.example.com/login",
                                    method = HttpMethod.POST,
                                    body = HttpBody.Json(mapOf(
                                        "email" to email,
                                        "password" to password
                                    ))
                                )
                                val result = Json.decodeFromString<LoginResponse>(response.text())
                                authToken.value = result.token
                            }

                            // Make authenticated requests
                            suspend fun fetchUserData() {
                                val token = authToken() ?: throw Exception("Not authenticated")

                                val response = fetch(
                                    url = "https://api.example.com/user",
                                    headers = RequestHeaders(
                                        "Authorization" to "Bearer ${'$'}token"
                                    )
                                )
                                return Json.decodeFromString<User>(response.text())
                            }
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
                                private val authToken: Signal<String?>
                            ) {
                                private suspend fun request(
                                    path: String,
                                    method: HttpMethod = HttpMethod.GET,
                                    body: HttpBody? = null
                                ): RequestResponse {
                                    val headers = RequestHeaders(
                                        "Content-Type" to "application/json"
                                    )

                                    authToken()?.let { token ->
                                        headers["Authorization"] = "Bearer ${'$'}token"
                                    }

                                    return fetch(
                                        url = "${'$'}baseUrl${'$'}path",
                                        method = method,
                                        headers = headers,
                                        body = body
                                    )
                                }

                                suspend fun getUser(id: Int): User {
                                    val response = request("/users/${'$'}id")
                                    return Json.decodeFromString(response.text())
                                }

                                suspend fun updateUser(user: User): User {
                                    val response = request(
                                        "/users/${'$'}{user.id}",
                                        method = HttpMethod.PUT,
                                        body = HttpBody.Text(Json.encodeToString(user))
                                    )
                                    return Json.decodeFromString(response.text())
                                }
                            }

                            // Usage
                            val api = ApiClient("https://api.example.com", authToken)
                            val user = rememberSuspending { api.getUser(123) }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Best Practices") {
                    card.col {
                        text("✓ Always handle errors gracefully")
                        text("✓ Show loading states while requests are in progress")
                        text("✓ Use proper HTTP methods (GET, POST, PUT, DELETE)")
                        text("✓ Set appropriate Content-Type headers")
                        text("✓ Validate and sanitize user input before sending")
                        text("✓ Store sensitive data (tokens) securely")
                        text("✓ Implement retry logic for transient failures")
                        text("✓ Use HTTPS for production APIs")
                        text("✓ Cache responses when appropriate")
                        text("✓ Provide user feedback for long-running requests")
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
                                    body = HttpBody.Text(Json.encodeToString(data))
                                )
                            """.trimIndent()
                        }

                        space()
                        h5("Authenticated Request:")
                        code {
                            content = """
                                fetch(
                                    url = url,
                                    headers = RequestHeaders(
                                        "Authorization" to "Bearer ${'$'}token"
                                    )
                                )
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}
