package com.autodroid.trader.app.ui.tradeplan

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.trader.app.R
import com.autodroid.trader.app.ui.BaseFragment
import kotlinx.coroutines.launch

class TradePlanFragment : BaseFragment() {
    companion object {
        private const val ARG_TRADEPLAN_ID = "tradeplan_id"
        private const val ARG_TRADEPLAN_TITLE = "tradeplan_title"

        fun newInstance(tradePlanId: String, tradePlanTitle: String? = null): TradePlanFragment {
            val fragment = TradePlanFragment()
            val args = Bundle()
            args.putString(ARG_TRADEPLAN_ID, tradePlanId)
            args.putString(ARG_TRADEPLAN_TITLE, tradePlanTitle)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var tradePlanTitle: TextView
    private lateinit var stockDetailAdapter: StockDetailAdapter
    private lateinit var newsAdapter: NewsAdapter

    private val stockDetailViewModel: StockDetailViewModel by activityViewModels()

    override fun getLayoutId(): Int {
        return R.layout.fragment_trade_plan
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): android.view.View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        
        val toolbar = (activity as? AppCompatActivity)?.findViewById<Toolbar>(R.id.toolbar)
        toolbar?.visibility = View.GONE
        
        return view
    }

    override fun initViews(view: View) {
        tradePlanTitle = view.findViewById(R.id.tradeplan_detail_title)

        val args = arguments
        if (args != null) {
            val title = args.getString(ARG_TRADEPLAN_TITLE)
            if (title != null) {
                tradePlanTitle.text = title
            }
        }

        val backButton = view.findViewById<TextView>(R.id.tradeplan_detail_back_button)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        val rotateScreenButton = view.findViewById<TextView>(R.id.rotate_screen_button)
        rotateScreenButton.setOnClickListener {
            val currentOrientation = requireActivity().requestedOrientation
            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            } else {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            }
        }

        val stockDetailRecyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.stock_detail_recycler_view)
        stockDetailAdapter = StockDetailAdapter(
            items = emptyList(),
            onTimeframeChanged = { timeframe ->
                stockDetailViewModel.changeTimeframe(timeframe)
            }
        )
        stockDetailRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        stockDetailRecyclerView.adapter = stockDetailAdapter

        val newsRecyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.news_recycler_view)
        if (newsRecyclerView != null) {
            newsAdapter = NewsAdapter(newsList = emptyList())
            newsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            newsRecyclerView.adapter = newsAdapter
        }
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            stockDetailViewModel.stockDetailItems.collect { items ->
                stockDetailAdapter.updateItems(items)
                
                if (::newsAdapter.isInitialized) {
                    val newsItem = items.find { it is StockDetailItem.News }
                    if (newsItem is StockDetailItem.News) {
                        newsAdapter.updateItems(newsItem.newsList)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        val toolbar = (activity as? AppCompatActivity)?.findViewById<Toolbar>(R.id.toolbar)
        toolbar?.visibility = View.VISIBLE
    }
}