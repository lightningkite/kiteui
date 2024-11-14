package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.delay

@Routable("testing")
object TestingGroundScreen: Screen {
    override fun ViewWriter.render() {
        scrolls - col {
            h1("Experiments tests")
//            centered - sizeConstraints(maxWidth = 10.rem) - image { source = Resources.imagesSolera }
          important-viewPager {
              children(Constant(InfiniteImagesScreen.ReturnIndexList)) { currImage ->
                  val renders = Property(0)
                  stack {
                      ::transitionId { currImage().toString() }
                      spacing = 0.25.rem
                      zoomableImage {
                          reactiveScope {
                              renders.value++
                              val index = currImage()
                              source = ImageRemote("https://picsum.photos/seed/${index}/100/100")
                              async(index) { delay(1) }
                              source = ImageRemote("https://picsum.photos/seed/${index}/1000/1000")
                          }
                          scaleType = ImageScaleType.Fit
                      }
                      centered - h2 { ::content { renders().toString() } }
                  }
              }
            }
        }
    }
}