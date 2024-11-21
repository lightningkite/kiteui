package com.lightningkite.kiteui

expect suspend fun ByteArray.sha1(): ByteArray
expect suspend fun ByteArray.sha256(): ByteArray
expect suspend fun ByteArray.sha512(): ByteArray
