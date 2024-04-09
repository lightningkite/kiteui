package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.clockMillis
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

@Routable("sample/pong")
object PongSampleScreen : Screen {

    override fun ViewWriter.render() = stack {
        canvas {
            val dg = PongDelegate()
            val c = currentTheme
            reactiveScope {
                dg.font = c().title
            }
            delegate = dg
            var last = clockMillis()
            reactiveScope {
                rerunOn(AnimationFrame)
                val now = clockMillis()
                val diff = now - last
                last = now
                dg.frame(diff / 1000.0)
                dg.invalidate()
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


class PongDelegate : CanvasDelegate() {
    var ballX: Double = 0.0
    var ballY: Double = 0.0
    var ballRadius: Double = 5.0
    var ballVX: Double = 100.0
    var ballVY: Double = 100.0
    var stageHalfLength: Double = 100.0
    var stageHalfWidth: Double = 50.0
    var paddleOffset: Double = 20.0
    var paddleHalfThickness: Double = 5.0
    var paddleHalfWidth: Double = 10.0
    var paddleLeftY: Double = 0.0
    var paddleRightY: Double = 0.0
    var font: FontAndStyle = FontAndStyle()

    var scoreLeft: Int = 0
    var scoreRight: Int = 0

    var eventText: String = "Event text here"

    override fun onKeyDown(key: KeyCode): Boolean {
        eventText = "onKeyDown $key"
        return true
    }
    override fun onKeyUp(key: KeyCode): Boolean {
        eventText = "onKeyUp $key"
        return true
    }
    override fun onWheel(x: Double, y: Double, z: Double): Boolean {
        eventText = "onWheel $x $y $z"
        return true
    }

    fun frame(time: Double) {
        ballX += ballVX * time
        ballY += ballVY * time
        if (ballY > stageHalfWidth - ballRadius) {
            ballVY = -abs(ballVY)
            ballY = stageHalfWidth - ballRadius
        }
        if (ballY < -stageHalfWidth + ballRadius) {
            ballVY = abs(ballVY)
            ballY = -stageHalfWidth + ballRadius
        }
        if (abs(ballX - -(stageHalfLength - paddleOffset)) < paddleHalfThickness + ballRadius) {
            if (abs(ballY - paddleLeftY) < paddleHalfWidth + ballRadius) {
                ballVX = abs(ballVX)
            }
        }
        if (abs(ballX - (stageHalfLength - paddleOffset)) < paddleHalfThickness + ballRadius) {
            if (abs(ballY - paddleRightY) < paddleHalfWidth + ballRadius) {
                ballVX = -abs(ballVX)
            }
        }
        if (ballX < -stageHalfLength) {
            ballX = 0.0
            ballY = 0.0
            ballVX = -ballVX
            scoreRight++
        } else if (ballX > stageHalfLength) {
            ballX = 0.0
            ballY = 0.0
            ballVX = -ballVX
            scoreLeft++
        }
    }

    override fun draw(context: DrawingContext2D) {
        this.width = context.width
        this.height = context.height
        val canvas = context
        canvas.fillPaint = Color.black
        canvas.fillRect(
            transformX(-stageHalfLength),
            transformY(-stageHalfWidth),
            transformWidth(stageHalfLength * 2),
            transformHeight(stageHalfWidth * 2),
        )
        canvas.fillPaint = Color.white
        canvas.fillRect(
            transformX(-stageHalfLength + paddleOffset - paddleHalfThickness),
            transformY(paddleLeftY - paddleHalfWidth),
            transformWidth(paddleHalfThickness * 2),
            transformHeight(paddleHalfWidth * 2),
        )
        canvas.fillRect(
            transformX(stageHalfLength - paddleOffset - paddleHalfThickness),
            transformY(paddleRightY - paddleHalfWidth),
            transformWidth(paddleHalfThickness * 2),
            transformHeight(paddleHalfWidth * 2),
        )
        canvas.fillPaint = Color.red
        canvas.fillRect(
            transformX(ballX - ballRadius),
            transformY(ballY - ballRadius),
            transformWidth(ballRadius * 2),
            transformHeight(ballRadius * 2),
        )
        canvas.fillPaint = Color.white
        canvas.beginPath()
        canvas.appendArc(
            transformX(ballX),
            transformY(ballY),
            transformWidth(ballRadius),
            Angle.zero,
            Angle.circle,
            true
        )
        canvas.closePath()
        canvas.fill()
        canvas.textAlign(TextAlign.center)
        canvas.font(height / 6.0, font)
        canvas.drawText(
            "$scoreLeft - $scoreRight",
            transformX(0.0),
            transformY(-stageHalfWidth / 2.0)
        )
        canvas.font(height / 20.0, font)
        canvas.drawText(
            eventText,
            transformX(0.0),
            transformY(stageHalfWidth / 2.0)
        )

        canvas.beginPath()
        canvas.appendArc(
            transformX(0.0),
            transformY(stageHalfWidth / 2.0),
            10.0,
            Angle.zero,
            Angle.circle,
            true
        )
        canvas.closePath()
        canvas.fill()
    }

    override fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        return onPointerUp(id, x, y, width, height)
    }

    override fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        return onPointerUp(id, x, y, width, height)
    }

    override fun onPointerUp(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
        if (reverseX(x) < 0) {
            paddleLeftY = reverseY(y)
        } else {
            paddleRightY = reverseY(y)
        }
        return true
    }

    var width: Double = 1.0
    var height: Double = 1.0
    fun transformX(x: Double): Double {
        return (x + stageHalfLength) / (stageHalfLength * 2) * width
    }

    fun transformY(y: Double): Double {
        return (y + stageHalfWidth) / (stageHalfWidth * 2) * height
    }

    fun transformWidth(x: Double): Double {
        return (x) / (stageHalfLength * 2) * width
    }

    fun transformHeight(y: Double): Double {
        return (y) / (stageHalfWidth * 2) * height
    }

    fun reverseX(x: Double): Double {
        return (x - width / 2) / width * stageHalfLength * 2
    }

    fun reverseY(y: Double): Double {
        return (y - height / 2) / height * stageHalfWidth * 2
    }

    override fun sizeThatFitsWidth(width: Double, height: Double): Double {
        val scale = min(
            (width)/(stageHalfLength),
            (height)/(stageHalfWidth)
        )
        return stageHalfLength * scale
    }

    override fun sizeThatFitsHeight(width: Double, height: Double): Double {
        val scale = min(
            (width)/(stageHalfLength),
            (height)/(stageHalfWidth)
        )
        return stageHalfWidth * scale
    }
}