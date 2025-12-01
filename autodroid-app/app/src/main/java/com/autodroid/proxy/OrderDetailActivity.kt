// OrderDetailActivity.kt
package com.autodroid.proxy

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.autodroid.proxy.viewmodel.AppViewModel
import com.google.gson.Gson
import java.util.*

class OrderDetailActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "OrderDetailActivity"
    }

    private lateinit var orderNameTextView: TextView
    private lateinit var orderStatusTextView: TextView
    private lateinit var orderCreatedTextView: TextView
    private lateinit var orderDescriptionTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private lateinit var viewModel: AppViewModel
    private lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        // Initialize Gson
        gson = Gson()

        // Initialize UI components
        orderNameTextView = findViewById(R.id.order_detail_name)
        orderStatusTextView = findViewById(R.id.order_detail_status)
        orderCreatedTextView = findViewById(R.id.order_detail_created)
        orderDescriptionTextView = findViewById(R.id.order_detail_description)
        startButton = findViewById(R.id.order_detail_start_button)
        stopButton = findViewById(R.id.order_detail_stop_button)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        // Get order data from intent
        val orderId = intent.getStringExtra("order_id")
        val orderTitle = intent.getStringExtra("order_title")
        val orderStatus = intent.getStringExtra("order_status")
        val orderCreated = intent.getStringExtra("order_created")

        // Populate order details
        populateOrderDetails(orderId, orderTitle, orderStatus, orderCreated)

        // Set up click listeners
        setupClickListeners()
    }

    private fun populateOrderDetails(orderId: String?, orderTitle: String?, orderStatus: String?, orderCreated: String?) {
        orderNameTextView.text = orderTitle ?: "Unknown Order"
        orderStatusTextView.text = "Status: ${orderStatus ?: "Unknown"}"
        orderCreatedTextView.text = "Created: ${orderCreated ?: "Unknown"}"
        orderDescriptionTextView.text = "This is a detailed view of test order #$orderId. You can start or stop the test execution from here."
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            // Start order execution
            startOrderExecution()
        }

        stopButton.setOnClickListener {
            // Stop order execution
            stopOrderExecution()
        }

        // Back button
        val backButton: Button = findViewById(R.id.order_detail_back_button)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun startOrderExecution() {
        // Update UI to show running state
        orderStatusTextView.text = "Status: Running"
        orderStatusTextView.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
        startButton.isEnabled = false
        stopButton.isEnabled = true

        // Log order start
        Log.d(TAG, "Starting order execution")

        // TODO: Implement actual order execution logic
        // This would typically involve communicating with the server
    }

    private fun stopOrderExecution() {
        // Update UI to show stopped state
        orderStatusTextView.text = "Status: Stopped"
        orderStatusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        startButton.isEnabled = true
        stopButton.isEnabled = false

        // Log order stop
        Log.d(TAG, "Stopping order execution")

        // TODO: Implement actual order stop logic
        // This would typically involve communicating with the server
    }
}