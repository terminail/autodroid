package com.autodroid.sms.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.autodroid.sms.R

/**
 * 设置界面
 */
class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}