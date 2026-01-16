package com.autodroid.guardiansdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.contacts.ContactFragment
import com.autodroid.guardiansdk.ui.settings.SettingFragment

class GuardianActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.guardian_activity_guardian)

        setupViews()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(ContactFragment.newInstance())
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

