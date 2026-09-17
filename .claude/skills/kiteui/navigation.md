# KiteUI Navigation & Pages

## Creating Pages

### Basic Page

```kotlin
@Routable("my-page")
object MyPage : Page {
    override fun ViewWriter.render() {
        col {
            h1("My Page Title")
            text("Content goes here")
        }
    }
}
```

**Note:** Older KiteUI 7 versions used `render(): ViewModifiable = run { }`, but current versions use `render() { }`.

### Page with Parameters

```kotlin
@Routable("user/{userId}")
class UserProfilePage(val userId: String) : Page {
    override fun ViewWriter.render() {
        col {
            h1("User Profile")
            text("User ID: $userId")
        }
    }
}
```

### Page with State

```kotlin
@Routable("counter")
object CounterPage : Page {
    override fun ViewWriter.render() {
        val count = Signal(0)

        col {
            h1("Counter")
            text { ::content { "Count: ${count()}" } }
            button {
                text("Increment")
                action = Action("Increment") {
                    count.value++
                }
            }
        }
    }
}
```

**Note:** Action constructor takes a name string, not an Icon. Icons are set separately if needed.

## Navigation Actions

### Navigate to Page

```kotlin
button {
    text("Go to Settings")
    onClick {
        pageNavigator.navigate(SettingsPage)
    }
}
```

### Navigate with Parameters

```kotlin
onClick {
    pageNavigator.navigate(UserProfilePage("user123"))
}
```

### Navigate Back

```kotlin
onClick {
    pageNavigator.goBack()
}
```

### Replace Current Page

```kotlin
onClick {
    pageNavigator.replace(LoginPage)
}
```

## Using Links

```kotlin
// Internal navigation link
link {
    text("Go to Settings")
    to = { SettingsPage }
}

// External link
externalLink {
    text("Visit Website")
    to = "https://example.com"
}
```

## URL Query Parameters

Use `@QueryParameter` for URL-persisted state:

```kotlin
@Routable("members")
object MembersScreen : Page {
    override val title: Reactive<String> = Constant("Members")

    // URL-persisted search state (creates ?searchQuery= parameter)
    @QueryParameter
    val searchQuery = Signal("")

    override fun ViewWriter.render() {
        col {
            field("Search") {
                textInput {
                    content bind searchQuery
                }
            }
            // ... rest of page
        }
    }
}
```

## Common Patterns

### Login Screen

```kotlin
@Routable("login")
object LoginPage : Page {
    override fun ViewWriter.render() {
        val email = Signal("")
        val password = Signal("")

        frame {
            // Background image
            image {
                source = Resources.loginBackground
                scaleType = ImageScaleType.Crop
                opacity = 0.5
            }

            // Login form
            padded.scrolling.col {
                expanding.space()
                centered.sizeConstraints(maxWidth = 30.rem).card.col {
                    h1("Welcome")

                    field("Email") {
                        fieldTheme.textInput {
                            hint = "your@email.com"
                            keyboardHints = KeyboardHints.email
                            content bind email
                        }
                    }

                    field("Password") {
                        fieldTheme.textInput {
                            hint = "Password"
                            keyboardHints = KeyboardHints.password
                            content bind password
                            action = Action("Sign In", Icon.login) {
                                signIn(email(), password())
                            }
                        }
                    }

                    important.button {
                        text("Sign In")
                        onClick {
                            signIn(email(), password())
                        }
                    }
                }
                expanding.space()
            }
        }
    }

    private suspend fun ViewWriter.signIn(email: String, password: String) {
        // Login logic
        pageNavigator.navigate(HomePage)
    }
}
```

### List Page with Search

```kotlin
@Routable("users")
object UsersPage : Page {
    override fun ViewWriter.render() {
        val searchQuery = Signal("")
        val users = Signal(listOf<User>())
        val filteredUsers = remember {
            val query = searchQuery().lowercase()
            users().filter { it.name.lowercase().contains(query) }
        }

        launch {
            users.value = api.fetchUsers()
        }

        col {
            card.row {
                expanding.textInput {
                    hint = "Search users"
                    content bind searchQuery
                }
            }

            scrolling.col {
                forEach(filteredUsers) { user ->
                    userCard(user)
                }
            }
        }
    }
}
```

### Detail Page with Loading

```kotlin
@Routable("product/{id}")
class ProductPage(val id: String) : Page {
    override fun ViewWriter.render() {
        val product = LateInitProperty<Product>()

        launch {
            product.value = api.fetchProduct(id)
        }

        scrolling.col {
            // Automatically shows loading while product is unset
            image {
                ::source { product().imageUrl }
                scaleType = ImageScaleType.Crop
            }

            padded.col {
                h1 { ::content { product().name } }
                text { ::content { product().description } }
                text { ::content { "${product().price}" } }

                important.button {
                    text("Add to Cart")
                    onClick {
                        addToCart(product())
                    }
                }
            }
        }
    }
}
```

## Common Issue: Route Conflicts

**Symptoms:**
- Navigating to a route shows the wrong screen
- Old placeholder screens show instead of new implementations
- Routes work inconsistently

**Root Cause:** Multiple files have `@Routable` annotations with the same path. KiteUI's route generator picks one arbitrarily, often the wrong one.

**Solution:** Delete the duplicate routes and update references.

**Example diagnosis:**
```bash
# Find duplicate routes
grep -r "@Routable(\"admin/organizations\")" apps/src

# Output shows duplicates:
apps/src/.../AdminOrgListScreen.kt:@Routable("admin/organizations")
apps/src/.../OrganizationManagementScreen.kt:@Routable("admin/organizations")
```

**Fix:**
1. Delete the old/placeholder screen file
2. Update all references to the deleted screen
3. Rebuild frontend (routes are generated at compile time)
4. Verify the correct screen loads

**Prevention:** Before creating a new screen, search for existing routes:
```bash
grep -r "@Routable(\"your/route/here\")" apps/src
```

## Best Practices

1. Use `@Routable` annotations for all pages
2. Make deep linking easy with URL parameters
3. Use typed navigation (`navigate(MyPage)`) over string URLs
4. Use `@QueryParameter` for search/filter state that should be shareable via URL
