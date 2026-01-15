package com.autodroid.note

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.autodroid.guardiansdk.GuardianSdk

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 Guardian SDK
        GuardianSdk.initialize(this)
        
        // 设置布局
        setContentView(R.layout.activity_main)
        
        // 设置按钮点击事件 - 打开设置界面
        findViewById<android.widget.Button>(R.id.btnOpenSettings).setOnClickListener {
            GuardianSdk.getInstance().startSettingActivity()
        }
    }
}
