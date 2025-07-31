package com.lightningkite.kiteui

public expect suspend fun ByteArray.sha1(): ByteArray
public expect suspend fun ByteArray.sha256(): ByteArray
public expect suspend fun ByteArray.sha512(): ByteArray
