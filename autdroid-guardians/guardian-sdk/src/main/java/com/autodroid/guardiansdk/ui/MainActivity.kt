package com.autodroid.guardiansdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.autodroid.guardiansdk.databinding.ActivityMainBinding
import com.autodroid.guardiansdk.ui.fragments.GuardiansFragment
import com.autodroid.guardiansdk.ui.settings.SettingFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(GuardiansFragment.newInstance())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                com.autodroid.guardiansdk.R.id.nav_guardians -> {
                    loadFragment(GuardiansFragment.newInstance())
                    true
                }
                com.autodroid.guardiansdk.R.id.nav_settings -> {
                    loadFragment(SettingFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}
