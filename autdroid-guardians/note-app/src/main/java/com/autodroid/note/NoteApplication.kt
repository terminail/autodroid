package com.autodroid.note

import android.app.Application
import com.autodroid.guardiansdk.GuardianSdk

class NoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Guardian SDK - SDK会自动启动所有必要服务
        GuardianSdk.initialize(this)
    }
}