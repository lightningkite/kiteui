package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.widgets.code
                            text("• Incorrect module dependency")
                            text("• Need to regenerate auto-routes")
                        }
                        text("Try: ./gradlew clean build")
                    }

                    space()
                    h5("Q: Auto-routes generation fails")
                    card.col {
                        text("Run the generation task explicitly:")
                        code {
                            content = "./gradlew generateAutoRoutes"
                        }
                        text("Check that all Routable pages are valid and have correct syntax.")
                    }

                    space()
                    h5("Q: Large bundle size on web")
                    card.col {
                        text("KiteUI is designed for small bundles, but check:")
                        col {
                            text("• Are you importing large unnecessary dependencies?")
                            text("• Use production builds for deployment")
                            text("• Consider code splitting for large apps")
                        }
                    }
                }

                space()

                titledSection("Data Loading Issues") {

                    h5("Q: Loading state shows forever")
                    card.col {
                        text("Check that your suspend function completes:")
                        code {
                            content = """
                                val data = rememberSuspending {
                                    try {
                                        fetchData()
                                    } catch (e: Exception) {
                                        // Must handle or re-throw
                                        throw e
                                    }
                                }
                            """.trimIndent()
                        }
                    }

                    space()
                    h5("Q: Data doesn't refresh when it should")
                    card.col {
                        text("Make sure your rememberSuspending block reads the dependencies:")
                        code {
                            content = """
                                val userId = Signal(1)

                                val user = rememberSuspending {
                                    // Must read userId() to track dependency
                                    fetchUser(userId())
                                }

                                // Now changing userId will refetch
                                button {
                                    text("Next User")
                                    onClick { userId.value++ }
                                }
                            """.trimIndent()
                        }
                    }
                }

                space()

                titledSection("Form and Input Issues") {

                    h5("Q: Two-way binding doesn't work")
                    card.col {
                        text("Use 'bind' keyword with Signal:")
                        code {
                            content = """
                                val name = Signal("")
                                textInput {
                                    content bind name  // Note: bind keyword
                                }
                            """.trimIndent()
                        }
                    }

                    space()
                    h5("Q: Form validation doesn't trigger")
                    card.col {
                        text("Validation should use reactive expressions:")
                        code {
                            content = """
                                val email = Signal("")
                                val emailError = remember {
                                    // This re-evaluates when email changes
                                    if (!email().contains("@")) "Invalid" else null
                                }
                            """.trimIndent()
                        }
                    }
                }

                space()

                titledSection("Performance Issues") {

                    h5("Q: App feels slow or laggy")
                    card.col {
                        text("Common causes and solutions:")
                        col {
                            text("• Use recyclerView for long lists, not forEach")
                            text("• Avoid expensive computations in reactive blocks")
                            text("• Cache computed values with remember")
                            text("• Check for unnecessary re-renders")
                        }
                    }

                    space()
                    h5("Q: Too many network requests")
                    card.col {
                        text("Implement caching:")
                        code {
                            content = """
                                // Cache result to avoid refetching
                                val cachedData = remember {
                                    // Only fetches once
                                    rememberSuspending { fetchData() }
                                }
                            """.trimIndent()
                        }
                    }
                }

                space()

                titledSection("Platform-Specific Issues") {

                    h5("Q: Feature works on web but not mobile")
                    card.col {
                        text("Some features are platform-specific. Check:")
                        col {
                            text("• Web-specific APIs (like certain HTML features)")
                            text("• Platform availability in documentation")
                            text("• Use expect/actual for platform-specific code")
                        }
                    }

                    space()
                    h5("Q: How do I write platform-specific code?")
                    card.col {
                        code {
                            content = """
                                // In commonMain
                                expect fun getPlatformName(): String

                                // In jsMain
                                actual fun getPlatformName() = "JavaScript"

                                // In androidMain
                                actual fun getPlatformName() = "Android"
                            """.trimIndent()
                        }
                    }
                }

                space()

                titledSection("Getting Help") {
                    card.col {
                        text("If you're still stuck:")
                        col {
                            text("• Check the KiteUI documentation")
                            text("• Look at example-app source code")
                            text("• Search GitHub issues")
                            text("• Ask in the community channels")
                        }
                        space()
                        text("When reporting issues, include:")
                        col {
                            text("• KiteUI version")
                            text("• Platform (Android/iOS/JS/JVM)")
                            text("• Minimal reproduction code")
                            text("• Error messages and stack traces")
                        }
                    }
                }

                space()

                titledSection("Quick Reference") {
                    card.col {
                        h5("Common Commands:")
                        code {
                            content = """
                                # Build project
                                ./gradlew build

                                # Run web version
                                ./gradlew :example-app:jsRun

                                # Generate routes
                                ./gradlew generateAutoRoutes

                                # Clean build
                                ./gradlew clean build

                                # Publish to maven local
                                ./gradlew publishToMavenLocal
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}
