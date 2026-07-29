package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.UrlCacheStrategy
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.addAndRunStateListener

/**
 * Exercises [UrlCacheStrategy] against the signed-URL problem it exists for: image URLs whose query
 * string is a signature that rotates and eventually expires, while the picture behind it never
 * changes.
 *
 * The first section checks that rotating a signature only reloads the strategies that treat the
 * query string as part of the image's identity.  The second checks that a view showing a failed
 * load still recovers when a fresh signature arrives, even though [UrlCacheStrategy.PathOnly]
 * considers the two URLs to be the same image.
 */
@Routable("image-reload-test")
object ImageReloadTestPage : Page {

    private const val IMAGE_PATH = "https://picsum.photos/seed/reload-test"

    private fun signedUrl(picture: Int, signature: Int) = "$IMAGE_PATH-$picture/300/300?sig=$signature"

    // picsum ignores query parameters it doesn't recognise but answers 400 to a blur outside 1..10,
    // which gives us one path that succeeds or fails purely on its query string - the same shape as
    // a signed URL whose signature has expired.
    private fun expiredUrl(picture: Int) = "$IMAGE_PATH-$picture/300/300?blur=99"

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            rotationSection()
            space()
            expirySection()
        }
    }

    private fun ElementWriter.rotationSection() = col {
        val signature = Signal(1)
        val picture = Signal(1)

        h2("Signature rotation")
        text(
            "All three images show the same picture. \"Rotate signature\" changes only the query " +
                    "string; \"New picture\" changes the path. The load counter goes up each time " +
                    "the view actually re-downloads and re-renders."
        )
        row {
            expanding.button {
                text("Rotate signature")
                onClick { signature.value++ }
            }
            expanding.button {
                text("New picture")
                onClick { picture.value++ }
            }
        }
        subtext { ::content { signedUrl(picture(), signature()) } }

        for (strategy in UrlCacheStrategy.entries) card.col {
            h3(strategy.name)
            subtext(
                when (strategy) {
                    UrlCacheStrategy.None -> "The URL never identifies the image, so every press reloads."
                    UrlCacheStrategy.Full -> "The whole URL identifies the image, so every press reloads."
                    UrlCacheStrategy.PathOnly -> "The query string is only a signature, so only \"New picture\" reloads."
                }
            )
            row {
                val loads = Signal(0)
                sizeConstraints(width = 8.rem, height = 8.rem).image {
                    scaleType = ImageScaleType.Crop
                    ::source { ImageRemote(signedUrl(picture(), signature()), strategy) }
                    countLoads(loads)
                }
                centered.text { ::content { "Loads: ${loads()}" } }
            }
        }
    }

    private fun ElementWriter.expirySection() = col {
        // The signature the app is holding, and the one the server will still accept.  They start
        // out matching; expiring advances the server's without telling the app.
        val held = Signal(1)
        val accepted = Signal(1)
        val mount = Signal(1)
        val loads = Signal(0)

        h2("Expired signature recovery")
        text(
            "A signature only fails when a view actually requests it, so expiring one changes " +
                    "nothing while the picture is already on screen. Remount to get a view that " +
                    "requests the expired URL and fails - then refreshing the signature has to " +
                    "recover it, even though PathOnly considers both URLs the same image."
        )
        text("Press the buttons left to right: expire, remount (image fails), refresh (image returns).")
        row {
            expanding.button {
                text("Expire signature")
                onClick { accepted.value++ }
            }
            expanding.button {
                text("Remount view")
                onClick { mount.value++ }
            }
            expanding.button {
                text("Refresh signature")
                onClick { held.value = accepted.value }
            }
        }
        subtext {
            ::content {
                if (held() == accepted()) "signature ${held()} is valid: ${signedUrl(0, held())}"
                else "signature ${held()} has expired, server now wants ${accepted()}: ${expiredUrl(0)}"
            }
        }

        card.row {
            sizeConstraints(width = 8.rem, height = 8.rem).swapping(
                current = { mount() },
                views = {
                    image {
                        scaleType = ImageScaleType.Crop
                        ::source {
                            val url = if (held() == accepted()) signedUrl(0, held()) else expiredUrl(0)
                            ImageRemote(url, UrlCacheStrategy.PathOnly)
                        }
                        countLoads(loads)
                    }
                }
            )
            centered.text { ::content { "Loads: ${loads()}" } }
        }
    }

    /**
     * A re-render drops the shown info back to not-ready until the new image finishes loading, so
     * counting those transitions counts the reloads.
     */
    private fun ImageView.countLoads(into: MutableReactiveValue<Int>) {
        shownInfo.addAndRunStateListener { if (!it.ready) into.value++ }.also(::onRemove)
    }
}
