package com.lightningkite.kiteui


expect fun afterTimeout(milliseconds: Long, action: ()->Unit): ()->Unit
