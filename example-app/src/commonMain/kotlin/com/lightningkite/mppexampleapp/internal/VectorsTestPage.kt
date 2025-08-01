package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("test/vectors")
public object VectorsTestPage : Page {
    public override fun ViewWriter.render(): ViewModifiable = run {
        col {
            row {
                image {
                    source = ImageVector(
                        4.rem, 4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                strokeColor = Color.black,
                                strokeWidth = 4.0,
                                path = "M 20 50 A 60 60 0 0 0 80 50"
                            ), ImageVector.Path(
                                strokeColor = Color.blue,
                                strokeWidth = 2.0,
                                path = "M 50 0 V 100"
                            )
                        )
                    )
                }
                image {
                    source = ImageVector(
                        4.rem, 4.rem,
                        viewBoxWidth = 320,
                        viewBoxHeight = 320,
                        paths = listOf(
                            ImageVector.Path(
                                strokeColor = Color.black,
                                strokeWidth = 10.0,
                                path = "M 10 315\n" +
                                        "           L 110 215\n" +
                                        "           A 30 50 0 0 1 162.55 162.45\n" +
                                        "           L 172.55 152.45\n" +
                                        "           A 30 50 -45 0 1 215.1 109.9\n" +
                                        "           L 315 10"
                            )
                        )
                    )
                }
            }
            row {
                image {
                    source = ImageVector(
                        4.rem,
                        4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                fillColor = Color.green,
                                path = "M 0,0 L 0,100 L 100,100 L 100,0 Z"
                            ),
                            ImageVector.Path(
                                strokeColor = Color.blue,
                                strokeWidth = 5.0,
                                path = "M 25, 50 l 50,0 l -50,0 z"
                            ),
                            ImageVector.Path(
                                fillColor = Color.red,
                                strokeWidth = 2.0,
                                path = "M 25, 50 a 25,25 0 1,1 50,0 a 25,25 0 1,1 -50,0z"
                            )
                        )
                    )
                }
                image {
                    source = ImageVector(
                        4.rem,
                        4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                fillColor = Color.green,
                                path = "M 0,0 L 0,100 L 100,100 L 100,0 Z"
                            ),
                            ImageVector.Path(
                                strokeColor = Color.blue,
                                strokeWidth = 5.0,
                                path = "M 0, 0 Q 100, 0, 100, 100 L 0, 100 z"
                            ),
                        )
                    )
                }
                image {
                    source = ImageVector(
                        4.rem,
                        4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                fillColor = Color.green,
                                path = "M 0,0 L 0,100 L 100,100 L 100,0 Z"
                            ),
                            ImageVector.Path(
                                strokeColor = Color.blue,
                                strokeWidth = 5.0,
                                path = "M 50, 0 Q 100, 0, 100, 50 T 50 100 T 0 50 z"
                            ),
                        )
                    )
                }
                image {
                    source = ImageVector(
                        4.rem,
                        4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                fillColor = Color.green,
                                path = "M 0,0 L 0,100 L 100,100 L 100,0 Z"
                            ),
                            ImageVector.Path(
                                strokeColor = Color.blue,
                                strokeWidth = 5.0,
                                path = "M 0, 0 C 100, 0, 0, 100, 100, 100 L 0, 100 z"
                            ),
                        )
                    )
                }
                image {
                    source = ImageVector(
                        4.rem,
                        4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                fillColor = Color.green,
                                path = "M 0,0 L 0,100 L 100,100 L 100,0 Z"
                            ),
                            ImageVector.Path(
                                strokeColor = Color.blue,
                                strokeWidth = 5.0,
                                path = "M 50, 0 C 75, 0, 100, 25, 100, 50 S 100 100 50 100 S 0 100 0 50 z"
                            ),
                        )
                    )
                }
            }
            row {
                image {
                    source = ImageVector(
                        4.rem,
                        4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                fillColor = LinearGradient(
                                    listOf(
                                        GradientStop(0f, Color.blue),
                                        GradientStop(1f, Color.red),
                                    ), Angle.zero, false
                                ),
                                path = "M 0,0 L 0,100 L 100,100 L 100,0 Z"
                            ),
                            ImageVector.Path(
                                strokeColor = Color.black,
                                strokeWidth = 5.0,
                                path = "M0,0L100,100"
                            ),
                        )
                    )
                }
                image {
                    source = ImageVector(
                        4.rem,
                        4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                fillColor = LinearGradient(
                                    listOf(
                                        GradientStop(0f, Color.blue),
                                        GradientStop(1f, Color.red),
                                    ), Angle.eighthTurn, false
                                ),
                                path = "M 0,0 L 0,100 L 100,100 L 100,0 Z"
                            ),
                            ImageVector.Path(
                                strokeColor = Color.black,
                                strokeWidth = 5.0,
                                path = "M0,0L100,100"
                            ),
                        )
                    )
                }
                image {
                    source = ImageVector(
                        4.rem,
                        4.rem,
                        viewBoxWidth = 100,
                        viewBoxHeight = 100,
                        paths = listOf(
                            ImageVector.Path(
                                fillColor = LinearGradient(
                                    listOf(
                                        GradientStop(0f, Color.blue),
                                        GradientStop(1f, Color.red),
                                    ), Angle.eighthTurn + Angle.halfTurn, false
                                ),
                                path = "M 0,0 L 0,100 L 100,100 L 100,0 Z"
                            ),
                            ImageVector.Path(
                                strokeColor = Color.black,
                                strokeWidth = 5.0,
                                path = "M0,0L100,100"
                            ),
                        )
                    )
                }
            }
            row {
                sizeConstraints(width = 4.rem, height = 4.rem) - themeFromLast {
                    it.copy(
                        background = LinearGradient(
                            listOf(
                                GradientStop(0f, Color.blue),
                                GradientStop(1f, Color.red),
                            ), Angle.zero, false
                        )
                    )
                } - frame { space() }
                sizeConstraints(width = 4.rem, height = 4.rem) - themeFromLast {
                    it.copy(
                        background = LinearGradient(
                            listOf(
                                GradientStop(0f, Color.blue),
                                GradientStop(1f, Color.red),
                            ), Angle.eighthTurn, false
                        )
                    )
                } - frame { space() }
                sizeConstraints(width = 4.rem, height = 4.rem) - themeFromLast {
                    it.copy(
                        background = LinearGradient(
                            listOf(
                                GradientStop(0f, Color.blue),
                                GradientStop(1f, Color.red),
                            ), Angle.eighthTurn + Angle.halfTurn, false
                        )
                    )
                } - frame { space() }
            }

            listOf(1, 2, 4, 8).forEachIndexed { idx, size ->
                sizeConstraints(width = size.rem, height = size.rem) - frame {
                    themeChoice += ThemeDerivation {
                        it.copy(
                            background = Color.red,
                            cornerRadii = CornerRadii.ForceConstant(2.rem)
                        ).withBack
                    }
                    image {
                        themeChoice += ThemeDerivation {
                            it.copy(
                                background = RadialGradient(
                                    stops = listOfNotNull(
                                        GradientStop(0f, Color.blue),
                                        if (idx.mod(2) == 0) GradientStop(0.5f, Color.green) else null,
                                        GradientStop(1f, Color.red),
                                    ),
                                ),
                                cornerRadii = CornerRadii.ForceConstant(2.rem)
                            ).withBack
                        }
                    }
                }
            }
        }
    }
}

private fun vec(vararg pathData: String) = ImageVector(
    width = 10.rem,
    height = 10.rem,
    viewBoxWidth = 150,
    viewBoxHeight = 150,
    paths = pathData.map {
        ImageVector.Path(
            strokeColor = Color.black,
            strokeWidth = 1.0,
            path = it
        )
    }
)