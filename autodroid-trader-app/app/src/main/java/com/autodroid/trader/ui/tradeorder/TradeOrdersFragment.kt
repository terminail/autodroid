// TradeOrdersFragment.kt
package com.autodroid.trader.ui.tradeorder

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.trader.R
import com.autodroid.trader.ui.BaseFragment
import com.autodroid.trader.ui.BaseItemAdapter
import com.google.android.material.appbar.AppBarLayout

class TradeOrdersFragment : BaseFragment() {
    private var ordersTitleTextView: TextView? = null
    private var ordersRecyclerView: RecyclerView? = null
    private var adapter: BaseItemAdapter? = null
    private var orderItems: MutableList<MutableMap<String?, Any?>?>? = null
    
    // Pull-down detection for fragment header
    private var appBarLayout: AppBarLayout? = null
    private var touchStartY = 0f
    private var isPullingDown = false
    private var touchSlop = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderItems = ArrayList<MutableMap<String?, Any?>?>()
        setupMockData()
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_trade_orders
    }

    override fun initViews(view: View) {
        ordersTitleTextView = view?.findViewById<TextView?>(R.id.orders_title)
        ordersRecyclerView = view?.findViewById<RecyclerView>(R.id.orders_recycler_view)
        
        // Find AppBarLayout
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        
        // Initialize touch slop for pull-down detection
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        // Set up RecyclerView
        ordersRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        
        // Set up touch listener for pull-down detection
        ordersRecyclerView!!.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
        
        adapter = BaseItemAdapter(
            orderItems,
            object : BaseItemAdapter.OnItemClickListener {
                override fun onItemClick(item: MutableMap<String?, Any?>?) {
                    // Handle item click - open order detail
                    if (item != null) {
                        openOrderDetail(item)
                    }
                }
            },
            R.layout.item_generic
        )
        ordersRecyclerView!!.setAdapter(adapter)
    }

    override fun setupObservers() {
        // Setup observers for tradeorder data
    }

    private fun setupMockData() {
        // Add mock order items
        val order1: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        order1.put("title", "Test Order #12345")
        order1.put("subtitle", "Created - 2024-01-01 09:15")
        order1.put("status", "Pending")
        order1.put("id", "1")
        orderItems!!.add(order1)

        val order2: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        order2.put("title", "Test Order #12346")
        order2.put("subtitle", "In Progress - 2024-01-01 10:30")
        order2.put("status", "Running")
        order2.put("id", "2")
        orderItems!!.add(order2)

        val order3: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        order3.put("title", "Test Order #12347")
        order3.put("subtitle", "Completed - 2024-01-01 11:45")
        order3.put("status", "Completed")
        order3.put("id", "3")
        orderItems!!.add(order3)
    }

    private fun openOrderDetail(order: MutableMap<String?, Any?>) {
        // Navigate to TradeOrderFragment using Navigation Component
        val orderId = order.get("id") as String?
        val orderTitle = order.get("title") as String?
        val orderStatus = order.get("status") as String?
        val orderCreated = order.get("subtitle") as String?
        
        if (orderId != null && orderTitle != null && orderStatus != null && orderCreated != null) {
            val action = TradeOrdersFragmentDirections.actionNavOrdersToOrderDetailFragment(
                orderId, orderTitle, orderStatus, orderCreated
            )
            findNavController().navigate(action)
        }
    }
    
    /**
     * Handle touch events for pull-down detection
     */
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = event.y
                isPullingDown = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - touchStartY
                
                // Check if pulling down at the top of the list
                if (deltaY > touchSlop && !isPullingDown) {
                    val layoutManager = ordersRecyclerView?.layoutManager as? LinearLayoutManager
                    if (layoutManager?.findFirstVisibleItemPosition() == 0) {
                        // At the top of the list and pulling down
                        isPullingDown = true
                        appBarLayout?.visibility = VISIBLE
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPullingDown = false
            }
        }
        return false
    }
}