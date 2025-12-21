// TradePlansFragment.kt
package com.autodroid.trader.ui.tradeplan

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
import com.autodroid.trader.model.TradePlan
import com.google.android.material.appbar.AppBarLayout

class TradePlansFragment : BaseFragment() {
    private var tradePlansTitleTextView: TextView? = null
    private var tradePlansRecyclerView: RecyclerView? = null
    private var adapter: TradePlanAdapter? = null
    private var tradePlanItems: MutableList<TradePlan>? = null
    
    // Pull-down detection for fragment header
    private var appBarLayout: AppBarLayout? = null
    private var touchStartY = 0f
    private var isPullingDown = false
    private var touchSlop = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tradePlanItems = ArrayList<TradePlan>()
        setupMockData()
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_trade_plans
    }

    override fun initViews(view: View) {
        tradePlansTitleTextView = view?.findViewById<TextView?>(R.id.tradeplans_title)
        tradePlansRecyclerView = view?.findViewById<RecyclerView>(R.id.tradeplans_recycler_view)
        
        // Find AppBarLayout
        appBarLayout = view.findViewById(R.id.app_bar_layout)
        
        // Initialize touch slop for pull-down detection
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        // Set up RecyclerView
        tradePlansRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        
        // Set up touch listener for pull-down detection
        tradePlansRecyclerView!!.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
        
        adapter = TradePlanAdapter(
            tradePlanItems,
            object : TradePlanAdapter.OnTradePlanClickListener {
                override fun onTradePlanClick(tradePlan: TradePlan?) {
                    // Handle item click - open trade plan detail
                    openTradePlanDetail(tradePlan)
                }
            }
        )
        tradePlansRecyclerView!!.setAdapter(adapter)
    }

    override fun setupObservers() {
        // Setup observers for tradePlans data
    }

    private fun setupMockData() {
        // Add mock trade plan items
        val tradePlan1 = TradePlan(
            id = "1",
            title = "Login Trade Plan",
            subtitle = "com.example.app",
            status = "Active"
        )
        tradePlanItems!!.add(tradePlan1)

        val tradePlan2 = TradePlan(
            id = "2",
            title = "Purchase Trade Plan",
            subtitle = "com.shopping.app",
            status = "Inactive"
        )
        tradePlanItems!!.add(tradePlan2)

        val tradePlan3 = TradePlan(
            id = "3",
            title = "Settings Trade Plan",
            subtitle = "com.settings.app",
            status = "Active"
        )
        tradePlanItems!!.add(tradePlan3)
    }

    private fun openTradePlanDetail(tradePlan: TradePlan?) {
        // Get trade plan ID
        val tradePlanId = tradePlan?.id
        
        // Navigate to TradePlanFragment using Navigation Component
        if (tradePlanId != null) {
            val action = TradePlansFragmentDirections.actionNavTradeplansToTradeplanDetailFragment(tradePlanId)
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
                    val layoutManager = tradePlansRecyclerView?.layoutManager as? LinearLayoutManager
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