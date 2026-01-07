package com.autodroid.trader.aas.ui

import com.autodroid.trader.aas.R

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.aas.database.UIRecorderDatabase
import com.autodroid.trader.aas.ui.adapters.UIEventAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecordViewActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UIEventAdapter
    private lateinit var database: UIRecorderDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_view)
        
        initViews()
        initDatabase()
        setupRecyclerView()
        
        // Load events
        loadAllEvents()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
    }
    
    private fun initDatabase() {
        database = UIRecorderDatabase.getInstance(this)
    }
    
    private fun setupRecyclerView() {
        adapter = UIEventAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun loadAllEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = database.uiEventDao().getRecentEvents(100) // Load last 100 events
                launch(Dispatchers.Main) {
                    adapter.updateEvents(events)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(this@RecordViewActivity, "Error loading events", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadAllEvents() // Refresh events when activity resumes
    }
}