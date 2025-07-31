package com.lightningkite.kiteui

import android.os.Build
import java.net.URLDecoder
import java.net.URLEncoder

public actual fun decodeURIComponent(content: String): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || Build.VERSION.SDK_INT == 0) {
    URLDecoder.decode(content, Charsets.UTF_8)
} else {
    android.net.Uri.decode(content)
}

public actual fun encodeURIComponent(content: String): String  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || Build.VERSION.SDK_INT == 0) {
    URLEncoder.encode(content, Charsets.UTF_8)
} else {
    android.net.Uri.encode(content)
}
