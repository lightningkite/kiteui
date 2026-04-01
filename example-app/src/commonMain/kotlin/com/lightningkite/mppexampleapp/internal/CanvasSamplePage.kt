package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.internal.CanvasSamplePage.Point

@Routable("sample/canvas")
object CanvasSamplePage : Page {
    data class Point(val x: Double, val y: Double)

    override fun ElementWriter.CanAddTheme.render() {
        frame {
            canvas {
                delegate = DrawDelegate()
            }
        }
    }
//    fun Canvas.onPointerHold(action: suspend (get: suspend ()->Point)->Unit) {
//        action.createCoroutineUnintercepted(receiver = iterator, completion = )
//        onPointerDown {
//
//        }
//    }
}

class DrawDelegate(): CanvasDelegate() {
    val lines = ArrayList<ArrayList<Point>>()
    var line: ArrayList<Point>? = (null)
    val pointersDown = mutableSetOf<Int>()

    override fun draw(context: DrawingContext2D) {
        with(context) {
            clear()
            fillPaint = Color.red
            strokePaint = Color.black
            lineWidth = 5.0

            for (line in lines) {
                beginPath()
                moveTo(line.firstOrNull()?.x ?: 0.0, line.firstOrNull()?.y ?: 0.0)
                for (point in line) {
                    lineTo(point.x, point.y)
                }
                stroke()
            }
        }
    }

    override fun onPointerCancel(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        pointersDown.remove(id)
        line = null
        invalidate()
        return true
    }

    override fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        if (id in pointersDown) {
            line?.add(Point(x, y))
        }
        invalidate()
        return true
    }

    override fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        pointersDown.add(id)
        val new = ArrayList<Point>()
        line = new
        lines.add(new)
        invalidate()
        return true
    }

    override fun onPointerUp(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        pointersDown.remove(id)
        line = null
        invalidate()
        return true
    }

}
