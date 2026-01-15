package com.autodroid.guardiansdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.databinding.ActivitySettingBinding
import com.autodroid.guardiansdk.ui.settings.SettingFragment

/**
 * Guardian SDK 设置主Activity
 * 包含设置列表，点击设置项弹出对应的修改页面
 */
class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = binding.root.findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.container.id, SettingFragment.newInstance())
                .commit()
        }
    }
}