// OrderDetailFragment.kt
package com.autodroid.manager.ui.orders.detail

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.autodroid.manager.R
import com.autodroid.manager.ui.BaseDetailFragment

class OrderDetailFragment : BaseDetailFragment() {
    companion object {
        private const val TAG = "OrderDetailFragment"
        private const val ARG_ORDER_ID = "orderId"
        private const val ARG_ORDER_TITLE = "orderTitle"
        private const val ARG_ORDER_STATUS = "orderStatus"
        private const val ARG_ORDER_CREATED = "orderCreated"
    }

    private lateinit var orderNameTextView: TextView
    private lateinit var orderStatusTextView: TextView
    private lateinit var orderCreatedTextView: TextView
    private lateinit var orderDescriptionTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun getLayoutId(): Int {
        return R.layout.fragment_order_detail
    }

    override fun initViews(view: View) {
        orderNameTextView = view.findViewById(R.id.order_detail_name)
        orderStatusTextView = view.findViewById(R.id.order_detail_status)
        orderCreatedTextView = view.findViewById(R.id.order_detail_created)
        orderDescriptionTextView = view.findViewById(R.id.order_detail_description)
        startButton = view.findViewById(R.id.order_detail_start_button)
        stopButton = view.findViewById(R.id.order_detail_stop_button)
        
        // Get order data from arguments
        val orderId = arguments?.getString(ARG_ORDER_ID) ?: ""
        val orderTitle = arguments?.getString(ARG_ORDER_TITLE) ?: ""
        val orderStatus = arguments?.getString(ARG_ORDER_STATUS) ?: ""
        val orderCreated = arguments?.getString(ARG_ORDER_CREATED) ?: ""
        
        // Display order information
        orderNameTextView.text = orderTitle
        orderStatusTextView.text = orderStatus
        orderCreatedTextView.text = orderCreated
        orderDescriptionTextView.text = "Order ID: $orderId\nThis is a sample order description."
        
        // Set up button listeners
        startButton.setOnClickListener {
            Log.d(TAG, "Starting order: $orderId")
            startButton.isEnabled = false
            stopButton.isEnabled = true
            orderStatusTextView.text = "Running"
        }
        
        stopButton.setOnClickListener {
            Log.d(TAG, "Stopping order: $orderId")
            startButton.isEnabled = true
            stopButton.isEnabled = false
            orderStatusTextView.text = "Stopped"
        }
        
        // Set up back button
        val backButton = view.findViewById<View>(R.id.order_detail_back_button)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun setupObservers() {
        // Setup observers for order detail data
    }
}