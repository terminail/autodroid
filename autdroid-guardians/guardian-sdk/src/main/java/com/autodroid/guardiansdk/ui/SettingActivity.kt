package com.autodroid.guardiansdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.autodroid.guardiansdk.databinding.ActivitySettingBinding
import com.autodroid.guardiansdk.ui.settings.SettingFragment

/**
 * 隐秘设置主Activity
 * 包含设置列表，点击设置项弹出对应的修改页面
 */
class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 加载设置列表Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.container.id, SettingFragment.newInstance())
                .commit()
        }
    }
}