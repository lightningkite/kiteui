package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.mppexampleapp.internal.CanvasSamplePage.Point

@Routable("sample/canvas")
public object CanvasSamplePage : Page {
    public data class Point(val x: Double, val y: Double)

    public override fun ViewWriter.render() = frame {
        canvas {
            delegate = DrawDelegate()
        }
    }
//    fun Canvas.onPointerHold(action: suspend (get: suspend ()->Point)->Unit) {
//        action.createCoroutineUnintercepted(receiver = iterator, completion = )
//        onPointerDown {
//
//        }
//    }
}

public class DrawDelegate(): CanvasDelegate() {
    public val lines = ArrayList<ArrayList<Point>>()
    public var line: ArrayList<Point>? = (null)
    public val pointersDown = mutableSetOf<Int>()

    public override fun draw(context: DrawingContext2D) {
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

    public override fun onPointerCancel(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        pointersDown.remove(id)
        line = null
        invalidate()
        return true
    }

    public override fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        if (id in pointersDown) {
            line?.add(Point(x, y))
        }
        invalidate()
        return true
    }

    public override fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        pointersDown.add(id)
        val new = ArrayList<Point>()
        line = new
        lines.add(new)
        invalidate()
        return true
    }

    public override fun onPointerUp(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        pointersDown.remove(id)
        line = null
        invalidate()
        return true
    }

}
