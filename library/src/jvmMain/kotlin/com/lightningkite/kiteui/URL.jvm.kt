package com.lightningkite.kiteui

import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

@InternalKiteUi
public actual fun decodeURIComponent(content: String): String = URLDecoder.decode(content, Charsets.UTF_8)
@InternalKiteUi
public actual fun encodeURIComponent(content: String): String  = URLEncoder.encode(content, Charsets.UTF_8)
