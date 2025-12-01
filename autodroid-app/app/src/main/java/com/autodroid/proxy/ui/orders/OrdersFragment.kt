// OrdersFragment.kt
package com.autodroid.proxy.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.proxy.R
import com.autodroid.proxy.ui.BaseFragment
import com.autodroid.proxy.ui.adapters.BaseItemAdapter
import com.autodroid.proxy.viewmodel.AppViewModel

class OrdersFragment : BaseFragment() {
    private var ordersTitleTextView: TextView? = null
    private var ordersRecyclerView: RecyclerView? = null
    private var adapter: BaseItemAdapter? = null
    private var orderItems: MutableList<MutableMap<String?, Any?>?>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(requireActivity()).get<AppViewModel>(AppViewModel::class.java)
        orderItems = ArrayList<MutableMap<String?, Any?>?>()
        setupMockData()
    }

    override val layoutId: Int
        get() = R.layout.fragment_orders

    override fun initViews(view: View?) {
        ordersTitleTextView = view?.findViewById<TextView?>(R.id.orders_title)
        ordersRecyclerView = view?.findViewById<RecyclerView>(R.id.orders_recycler_view)


        // Set up RecyclerView
        ordersRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = BaseItemAdapter(
            orderItems,
            BaseItemAdapter.OnItemClickListener { item: MutableMap<String?, Any?>? ->
                // Handle item click - open order detail
                openOrderDetail(item!!)
            },
            R.layout.item_generic
        )
        ordersRecyclerView!!.setAdapter(adapter)
    }

    override fun setupObservers() {
        // Setup observers for orders data
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
        // Navigate to OrderDetailFragment using Navigation Component
        val orderId = order.get("id") as String?
        val orderTitle = order.get("title") as String?
        val orderStatus = order.get("status") as String?
        val orderCreated = order.get("subtitle") as String?
        
        if (orderId != null && orderTitle != null && orderStatus != null && orderCreated != null) {
            val action = OrdersFragmentDirections.actionNavOrdersToOrderDetailFragment(
                orderId, orderTitle, orderStatus, orderCreated
            )
            findNavController().navigate(action)
        }
    }
}