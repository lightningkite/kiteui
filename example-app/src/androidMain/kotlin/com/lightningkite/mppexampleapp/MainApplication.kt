package com.lightningkite.kiteuiexample

import android.app.Application
import java.io.File

public class MainApplication: Application() {
    public override fun onCreate() {
        super.onCreate()
        val dexOutputDir: File = codeCacheDir
        dexOutputDir.setReadOnly()
    }
}