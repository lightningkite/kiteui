package com.lightningkite.mppexampleapp

import com.lightningkite.mppexample.*

data class Category(
    val name: String,
    val key: String,
    val image: String,
    val subcategories: List<Category> = listOf(),
    val products: List<Product> = listOf()
)

open class CategoryScreen(
    private val category: Category, private val showBackButton: Boolean = true
) : AuthenticatedScreen() {
    override fun ViewContext.renderAuthenticated() {
        box {
            appBar(title = category.name, showBackButton = showBackButton)

            forEach(data = { category.subcategories }, render = { it ->
                listTile(
                    image = ImageRemote(it.image),
                    imageConstraints = SizeConstraints(maxHeight = 64.px),
                    onClick = {
                        navigator.navigate(CategoryScreen(it))
                    }
                ) {
                    text { content = it.name }
                }
            })

            forEach(data = { category.products }, render = { it -> productCard(it) })
        } in fullHeight() in scrolls()
    }

    override fun createPath(): String = "/categories/${category.key}"

    companion object {
        const val PATH = "/categories/{categoryKey}"
    }
}