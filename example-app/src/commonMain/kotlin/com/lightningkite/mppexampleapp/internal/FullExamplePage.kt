package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.mppexampleapp.UseFullPage
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("full-screen")
class FullScreenPage: Page, UseFullPage {

    override fun ViewWriter.render(): ViewModifiable = run {
//        programmatic {
//            delegate = ProgrammaticLayoutDelegate.AllFull
//            frame {
//                image {
//                    cannotBeCovered = false
//                    source = Resources.imagesSnowyBackground
//                    scaleType = ImageScaleType.Crop
//                }
//                col {
//                    viewDebugTarget = this
//                    h1 { content = "Full Screen!" }
//                    link {
//                        text { content = "Go back to root" }
//                        to = { RootPage }
//                    }
//                }
//            }
//        }

        viewPager {
            children(Constant((1..100).toList()), { it }) {
                frame {
                    image {
                        cannotBeCovered = false
                        source = Resources.imagesSnowyBackground
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
//                source = Resources.imagesSnowyBackground
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