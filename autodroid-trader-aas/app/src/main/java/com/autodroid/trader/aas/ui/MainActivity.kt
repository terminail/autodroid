package com.autodroid.trader.aas.ui

import com.autodroid.trader.aas.R

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.aas.database.UIRecorderDatabase


import com.autodroid.trader.aas.ui.adapters.UIEventAdapter

import com.autodroid.trader.aas.ui.viewmodels.UIEventViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UIEventAdapter
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var btnViewRecords: Button
    private lateinit var btnViewFeatures: Button
    private lateinit var database: UIRecorderDatabase
    private lateinit var viewModel: UIEventViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initDatabase()
        setupRecyclerView()
        initViewModel()
        setupButtons()
        
        // Check accessibility permission
        checkAccessibilityPermission()
    }
    
    private fun initViewModel() {
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(UIEventViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return UIEventViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        })[UIEventViewModel::class.java]
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        btnStartService = findViewById(R.id.btn_start_service)
        btnStopService = findViewById(R.id.btn_stop_service)
        btnViewRecords = findViewById(R.id.btn_view_records)
        btnViewFeatures = findViewById(R.id.btn_view_features)
    }
    
    private fun initDatabase() {
        database = UIRecorderDatabase.getInstance(this)
    }
    
    private fun setupRecyclerView() {
        adapter = UIEventAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // Load initial events
        loadRecentEvents()
    }
    
    private fun setupButtons() {
        btnStartService.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                showAccessibilityPermissionDialog()
            } else {
                Toast.makeText(this, "Service already enabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnStopService.setOnClickListener {
            Toast.makeText(this, "To stop service, go to Settings > Accessibility", Toast.LENGTH_LONG).show()
        }
        
        btnViewRecords.setOnClickListener {
            val intent = Intent(this, RecordViewActivity::class.java)
            startActivity(intent)
        }
        
        btnViewFeatures.setOnClickListener {
            // For now, we'll show a simple dialog to select a package
            showPackageSelectionDialog()
        }
    }
    
    private fun loadRecentEvents() {
        // ViewModel handles data loading and updates automatically
        // via the observer pattern
    }
    
    private fun checkAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityPermissionDialog()
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${com.autodroid.trader.aas.service.UIRecorderAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
    
    private fun showAccessibilityPermissionDialog() {
        Toast.makeText(this, "Please enable Trader AAS in Accessibility Settings", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    private fun showPackageSelectionDialog() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val packages = database.uiEventDao().getRecordedPackages()
                launch(Dispatchers.Main) {
                    if (packages.isNotEmpty()) {
                        showPackageSelectionList(packages)
                    } else {
                        Toast.makeText(this@MainActivity, "No recorded packages found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error loading packages", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showPackageSelectionList(packages: List<String>) {
        val packageArray = packages.toTypedArray()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select App Package")
            .setItems(packageArray) { _, which ->
                val selectedPackage = packageArray[which]
                val intent = Intent(this, ElementFeaturesActivity::class.java)
                intent.putExtra("package_name", selectedPackage)
                startActivity(intent)
            }
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // ViewModel automatically updates the UI when data changes
    }
    
    override fun onStart() {
        super.onStart()
        // Start observing events from ViewModel
        lifecycleScope.launch {
            viewModel.recentEvents.collect { events ->
                adapter.updateEvents(events)
            }
        }
    }
}