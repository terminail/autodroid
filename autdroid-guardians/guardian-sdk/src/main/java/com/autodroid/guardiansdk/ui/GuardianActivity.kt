package com.autodroid.guardiansdk.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.contacts.ContactFragment
import com.autodroid.guardiansdk.ui.settings.SettingFragment
import com.autodroid.guardiansdk.ui.why.WhyFragment
import com.autodroid.guardiansdk.util.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.repository.SettingRepository

class GuardianActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var settingRepository: SettingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.guardian_activity_guardian)

        val database = GuardianDatabase.getDatabase(this)
        settingRepository = SettingRepository(database.settingDao())
        
        setupViews()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(ContactFragment.newInstance())
        }

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrEmpty()) {
                processSharedData(sharedText)
            }
        }
    }

    private fun processSharedData(sharedText: String) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    settingRepository.putString("ward_shared_data_temp", sharedText, "临时被监护人分享数据")
                }

                Toast.makeText(
                    this@GuardianActivity,
                    "已接收到被监护人信息，请在WHY页面确认导入",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GuardianActivity,
                    "导入失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_guardians -> {
                    loadFragment(ContactFragment.newInstance())
                    true
                }
                R.id.nav_why -> {
                    loadFragment(WhyFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

