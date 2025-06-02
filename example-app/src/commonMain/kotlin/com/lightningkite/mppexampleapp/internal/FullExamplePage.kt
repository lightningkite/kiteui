package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.mppexampleapp.UseFullPage
import com.lightningkite.readable.Constant

@Routable("full-screen")
class FullScreenPage: Page, UseFullPage {

    override fun ViewWriter.render(): ViewModifiable = run {
        viewPager {
            children(Constant((1..100).toList()), { it }) {
                frame {
                    image {
                        cannotBeCovered = false
                        source = Resources.imagesSolera
                        scaleType = ImageScaleType.Crop
                    }
                    col {
                        h1 { content = "Full Screen!" }
                        link {
                            text { content = "Go back to root" }
                            to = { RootPage }
                        }
                    }
                }
            }
        }
//        unpadded - frame {
//            cannotBeCovered = false
//            image {
//                cannotBeCovered = false
//                source = Resources.imagesSolera
//                scaleType = ImageScaleType.Crop
//            }
//            col {
//                h1 { content = "Full Screen!" }
//                link {
//                    text { content = "Go back to root" }
//                    to = { RootPage }
//                }
//            }
//        }
    }
}