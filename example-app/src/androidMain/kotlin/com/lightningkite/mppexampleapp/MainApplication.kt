package com.lightningkite.kiteuiexample

import android.app.Application
import java.io.File

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        val dexOutputDir: File = codeCacheDir
        dexOutputDir.setReadOnly()
    }
}