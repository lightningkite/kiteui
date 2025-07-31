package com.lightningkite.kiteui

import platform.Foundation.*
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters

private val component = NSCharacterSet.characterSetWithCharactersInString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
public actual fun decodeURIComponent(content: String): String = (content as NSString).stringByRemovingPercentEncoding()!!
public actual fun encodeURIComponent(content: String): String = (content as NSString).stringByAddingPercentEncodingWithAllowedCharacters(component)!!
