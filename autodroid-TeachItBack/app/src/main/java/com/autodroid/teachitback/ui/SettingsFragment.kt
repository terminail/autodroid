package com.autodroid.teachitback.ui

import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.autodroid.teachitback.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        setupApiKeyPreference()
        setupUserNamePreference()
        setupAboutPreference()
    }

    private fun setupApiKeyPreference() {
        val apiKey = findPreference<EditTextPreference>("ai_api_key")

        apiKey?.setOnPreferenceChangeListener { _, newValue ->
            val newKey = newValue as String
            if (newKey.trim().isNotEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "API密钥已保存",
                    Toast.LENGTH_SHORT
                ).show()
                true
            } else {
                Toast.makeText(
                    requireContext(),
                    "API密钥不能为空",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
        }
    }

    private fun setupUserNamePreference() {
        val userName = findPreference<EditTextPreference>("user_name")

        userName?.setOnPreferenceChangeListener { _, newValue ->
            Toast.makeText(
                requireContext(),
                "用户名已更新",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    private fun setupAboutPreference() {
        val about = findPreference<EditTextPreference>("about")
        about?.summary = "Teach It Back v1.0"
    }
}
