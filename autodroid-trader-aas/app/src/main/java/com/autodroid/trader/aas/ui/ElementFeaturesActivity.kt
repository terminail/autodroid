package com.autodroid.trader.aas.ui

import com.autodroid.trader.aas.R

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.aas.database.UIRecorderDatabase
import com.autodroid.trader.aas.ui.adapters.ElementFeatureAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ElementFeaturesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ElementFeatureAdapter
    private lateinit var tvAppTitle: TextView
    private lateinit var database: UIRecorderDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_element_features)

        initViews()
        initDatabase()
        setupRecyclerView()

        val packageName = intent.getStringExtra("package_name") ?: ""
        if (packageName.isNotEmpty()) {
            loadElementFeatures(packageName)
            tvAppTitle.text = "Features for: $packageName"
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view_element_features)
        tvAppTitle = findViewById(R.id.tv_app_title)
    }

    private fun initDatabase() {
        database = UIRecorderDatabase.getInstance(this)
    }

    private fun setupRecyclerView() {
        adapter = ElementFeatureAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadElementFeatures(packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val features = database.elementFeatureDao().getElementsByPackage(packageName)
                launch(Dispatchers.Main) {
                    adapter.updateFeatures(features)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}