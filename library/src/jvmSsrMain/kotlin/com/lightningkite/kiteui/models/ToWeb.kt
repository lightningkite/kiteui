package com.lightningkite.kiteui.models


fun ImageVector.toWeb(): String {
    return buildString {
        append("data:image/svg+xml;utf8,<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${width.value}\" height=\"${height.value}\" viewBox=\"$viewBoxMinX $viewBoxMinY $viewBoxWidth $viewBoxHeight\">")
        paths.forEach { path ->
            append(
                "<path d=\"${path.path}\" stroke=\"${path.strokeColor?.toWeb() ?: Color.transparent.toWeb()}\" stroke-width=\"${path.strokeWidth ?: 0}\" fill=\"${
                    (path.fillColor ?: Color.transparent).closestColor().toWeb()
                }\"/>"
            )
        }
        append("</svg>")
    }
}
