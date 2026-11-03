package com.lightningkite.kiteui.models

// These match the commonHtml shapes, because the gradle plugin generates JVM resources in that form
// (paths under `common/` on the classpath).

public actual data class Font(
    val cssFontFamilyName: String,
    val url: String? = null,
    val fallback: String = "Helvetica",
    val direct: FontDirect? = null,
)

public data class FontDirect(
    val normal: Map<Int, String>,
    val italics: Map<Int, String>,
)

public actual val systemDefaultFont: Font = Font("system-ui")
public actual val systemDefaultFixedWidthFont: Font = Font("monospace")

public actual sealed class ImageSource actual constructor() : VisualMediaSource
public actual data class ImageResource(val relativeUrl: String) : ImageSource()

public actual sealed class VideoSource actual constructor() : VisualMediaSource
public actual data class VideoResource(val relativeUrl: String) : VideoSource()

public actual sealed class AudioSource actual constructor()
public actual data class AudioResource(val relativeUrl: String) : AudioSource()
