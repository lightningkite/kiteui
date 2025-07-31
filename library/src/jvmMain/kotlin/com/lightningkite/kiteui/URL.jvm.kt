package com.lightningkite.kiteui

import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

public actual fun decodeURIComponent(content: String): String = URLDecoder.decode(content, Charsets.UTF_8)
public actual fun encodeURIComponent(content: String): String  = URLEncoder.encode(content, Charsets.UTF_8)
