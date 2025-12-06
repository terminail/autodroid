// ApkDetailFragment.kt
package com.autodroid.manager.ui.apk.detail

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.autodroid.manager.R
import com.autodroid.manager.apk.ApkScannerManager
import com.autodroid.manager.ui.BaseFragment

class ApkDetailFragment : BaseFragment() {
    companion object {
        private const val TAG = "ApkDetailFragment"
        private const val ARG_APK_INFO = "apkInfo"
        
        // Helper method to create fragment with arguments
        fun newInstance(apkInfo: com.autodroid.manager.model.Apk): ApkDetailFragment {
            val fragment = ApkDetailFragment()
            val args = Bundle()
            args.putString("appName", apkInfo.appName)
            args.putString("packageName", apkInfo.packageName)
            args.putString("version", apkInfo.version)
            args.putInt("versionCode", apkInfo.versionCode)
            args.putString("installTime", apkInfo.installedTime.toString())
            args.putString("updateTime", "Unknown") // ApkScannerManager.ApkInfo doesn't have updateTime
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var appNameTextView: TextView
    private lateinit var packageNameTextView: TextView
    private lateinit var versionTextView: TextView
    private lateinit var versionCodeTextView: TextView
    private lateinit var installTimeTextView: TextView
    private lateinit var updateTimeTextView: TextView
    private lateinit var analyzeButton: Button
    private lateinit var testButton: Button

    override fun getLayoutId(): Int {
        return R.layout.fragment_apk_detail
    }

    override fun initViews(view: View) {
        // Initialize views
        appNameTextView = view.findViewById(R.id.apk_detail_app_name)
        packageNameTextView = view.findViewById(R.id.apk_detail_package_name)
        versionTextView = view.findViewById(R.id.apk_detail_version)
        versionCodeTextView = view.findViewById(R.id.apk_detail_version_code)
        installTimeTextView = view.findViewById(R.id.apk_detail_install_time)
        updateTimeTextView = view.findViewById(R.id.apk_detail_update_time)
        analyzeButton = view.findViewById(R.id.apk_detail_analyze_button)
        testButton = view.findViewById(R.id.apk_detail_test_button)
        
        // Get APK info from arguments
        val appName = arguments?.getString("appName") ?: "Unknown App"
        val packageName = arguments?.getString("packageName") ?: "Unknown"
        val version = arguments?.getString("version") ?: "Unknown"
        val versionCode = arguments?.getInt("versionCode", 0)
        val installTime = arguments?.getString("installTime") ?: "Unknown"
        val updateTime = arguments?.getString("updateTime") ?: "Unknown"
        
        // Display APK information
        appNameTextView.text = appName
        packageNameTextView.text = packageName
        versionTextView.text = version
        versionCodeTextView.text = versionCode?.toString() ?: "Unknown"
        installTimeTextView.text = installTime
        updateTimeTextView.text = updateTime
        
        // Set up button listeners
        analyzeButton.setOnClickListener {
            Log.d(TAG, "Analyzing APK: $packageName")
            // TODO: Implement APK analysis functionality
            showToast("APK analysis feature coming soon")
        }
        
        testButton.setOnClickListener {
            Log.d(TAG, "Creating test plan for APK: $packageName")
            // TODO: Implement test plan creation functionality
            showToast("Test plan creation feature coming soon")
        }
        
        // Set up back button
        val backButton = view.findViewById<Button>(R.id.apk_detail_back_button)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun setupObservers() {
        // Setup observers for APK detail data if needed
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}