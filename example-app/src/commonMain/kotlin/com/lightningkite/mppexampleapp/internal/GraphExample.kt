package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.math.*

@Routable("graph-example")
object GraphExamplePage : Page {
    override fun ViewWriter.render(): ViewModifiable {
        return scrolling - col {
            h1 { content = "Graph Example" }

            // Basic line graph example
            card - col {
                h2 { content = "Basic Line Graph" }
                text { content = "A simple line graph with some data points." }

                sizeConstraints(height = 300.dp) - frame {
                    lineGraph(
                        listOf(
                            Point(0.0, 0.0),
                            Point(1.0, 2.0),
                            Point(2.0, 1.0),
                            Point(3.0, 4.0),
                            Point(4.0, 3.0),
                            Point(5.0, 5.0)
                        )
                    ) {
                        xAxisLabel = "X Axis"
                        yAxisLabel = "Y Axis"
                    }
                }
            }

            // Sine wave example
            card - col {
                h2 { content = "Sine Wave" }
                text { content = "A graph showing a sine wave." }

                sizeConstraints(height = 300.dp) - frame {
                    lineGraph(
                        (0..100).map { 
                            val x = it * 0.1
                            Point(x, sin(x))
                        }
                    ) {
                        xAxisLabel = "X"
                        yAxisLabel = "sin(x)"
                        lineColor = Color.blue
                    }
                }
            }

            // Customized graph example
            card - col {
                h2 { content = "Customized Graph" }
                text { content = "A graph with customized appearance." }

                sizeConstraints(height = 300.dp) - frame {
                    graph {
                        data = (0..50).map { 
                            val x = it * 0.2
                            Point(x, cos(x))
                        }
                        xAxisLabel = "X"
                        yAxisLabel = "cos(x)"
                        lineColor = Color.green
                        pointColor = Color.purple
                        gridColor = Color(0.9f, 0.9f, 0.9f, 1.0f)
                        axisColor = Color.black
                        lineWidth = 3.0.dp
                        pointSize = 6.0.dp
                        padding = 50.0.dp
                    }
                }
            }

            // Graph with Y values only
            card - col {
                h2 { content = "Graph with Y Values Only" }
                text { content = "A graph created from a list of Y values." }

                sizeConstraints(height = 300.dp) - frame {
                    lineGraph(
                        listOf(5.0, 8.0, 13.0, 7.0, 10.0, 15.0, 12.0, 9.0)
                    ) {
                        xAxisLabel = "Index"
                        yAxisLabel = "Value"
                        lineColor = Color.red
                        showPoints = true
                        pointSize = 8.0.dp
                    }
                }
            }

            // Graph with pairs
            card - col {
                h2 { content = "Graph with Pairs" }
                text { content = "A graph created from a list of X-Y pairs." }

                sizeConstraints(height = 300.dp) - frame {
                    lineGraph(
                        listOf(
                            0.0 to 0.0,
                            1.0 to 1.0,
                            2.0 to 4.0,
                            3.0 to 9.0,
                            4.0 to 16.0,
                            5.0 to 25.0
                        )
                    ) {
                        xAxisLabel = "X"
                        yAxisLabel = "X²"
                        lineColor = Color.teal
                        showGrid = true
                    }
                }
            }

            // Graph with custom text sizes
            card - col {
                h2 { content = "Custom Text Sizes" }
                text { content = "A graph with customized text sizes for labels." }

                sizeConstraints(height = 300.dp) - frame {
                    lineGraph(
                        (0..20).map { 
                            val x = it * 0.5
                            Point(x, x * x / 4)
                        }
                    ) {
                        xAxisLabel = "X Axis"
                        yAxisLabel = "Y Axis"
                        lineColor = Color.purple

                        // Custom text sizes
                        axisLabelFontSize = 18.0.dp  // Larger axis labels
                        tickLabelFontSize = 14.0.dp  // Larger tick labels
                        noDataMessageFontSize = 20.0.dp  // Larger "no data" message (not visible with data)
                    }
                }
            }

            // Graph with no data to show custom message font size
            card - col {
                h2 { content = "No Data Message" }
                text { content = "A graph with no data to demonstrate the custom 'no data' message font size." }

                sizeConstraints(height = 300.dp) - frame {
                    graph {
                        data = emptyList()  // No data

                        // Custom text sizes
                        noDataMessageFontSize = 24.0.dp  // Very large "no data" message
                        axisLabelFontSize = 16.0.dp
                    }
                }
            }
        }
    }
}
