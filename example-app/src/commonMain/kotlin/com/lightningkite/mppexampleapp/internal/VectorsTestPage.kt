package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources

@Routable("test/vectors")
object VectorsTestPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        scrolling - col {
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
                        id = "grad0",
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
                        id = "grad1",
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
                        id = "grad2",
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
                            id = "red$size",
                            background = Color.red,
                            cornerRadii = CornerRadii.ForceConstant(2.rem)
                        ).withBack
                    }
                    image {
                        themeChoice += ThemeDerivation {
                            it.copy(
                                id = "radial$size",
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

            sizeConstraints(width = 4.rem, height = 4.rem) - image {
                source = Resources.vectorsBox
            }
            sizeConstraints(width = 4.rem, height = 4.rem) - image {
                source = Resources.vectorsHiking
            }
            sizeConstraints(width = 4.rem, height = 4.rem) - image {
                source = ImageVector(
                    width = 3.rem,
                    height = 3.rem,
                    viewBoxMinX = 50,
                    viewBoxMinY = 50,
                    viewBoxWidth = 364,
                    viewBoxHeight = 364,
                    paths = listOf(
                        ImageVector.Path(
                            LinearGradient(
                                stops = listOf(
                                    GradientStop(0f, Color.blue),
                                    GradientStop(1f, Color.red),
                                ),
                                angle = Angle.eighthTurn,
                                screenStatic = false
                            ),
                            path = "M183.039 68C183.039 65.7909 181.248 64 179.039 64H127.568C92.4602 64 64 92.4602 64 127.568V172.5C64 174.709 65.7909 176.5 68 176.5H84C86.2091 176.5 88 174.709 88 172.5V127.568C88 105.715 105.715 88 127.568 88H179.039C181.248 88 183.039 86.2091 183.039 84V68ZM293.276 88C291.067 88 289.276 86.2091 289.276 84V68C289.276 65.7909 291.067 64 293.276 64H316H336.432C371.54 64 400 92.4602 400 127.568V148V172.5C400 174.709 398.209 176.5 396 176.5H380C377.791 176.5 376 174.709 376 172.5V148V127.568C376 105.715 358.285 88 336.432 88H316H293.276ZM289.276 380C289.276 377.791 291.067 376 293.276 376H336.432C358.285 376 376 358.285 376 336.432V286.737C376 284.528 377.791 282.737 380 282.737H396C398.209 282.737 400 284.528 400 286.737V336.432C400 371.54 371.54 400 336.432 400H293.276C291.067 400 289.276 398.209 289.276 396V380ZM84 282.737C86.2091 282.737 88 284.528 88 286.737V336.432C88 358.285 105.715 376 127.568 376H179.039C181.248 376 183.039 377.791 183.039 380V396C183.039 398.209 181.248 400 179.039 400H127.568C92.4602 400 64 371.54 64 336.432V286.737C64 284.528 65.7909 282.737 68 282.737H84Z"
                        ),
                    )
                )
            }
        }
    }
}
