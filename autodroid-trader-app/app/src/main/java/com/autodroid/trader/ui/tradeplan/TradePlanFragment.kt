package com.autodroid.trader.ui.tradeplan

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.autodroid.trader.R
import com.autodroid.trader.ui.BaseFragment

class TradePlanFragment : BaseFragment() {
    companion object {
        private const val ARG_TRADEPLAN_ID = "tradeplan_id"

        fun newInstance(tradePlanId: String): TradePlanFragment {
            val fragment = TradePlanFragment()
            val args = Bundle()
            args.putString(ARG_TRADEPLAN_ID, tradePlanId)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var tradePlanName: TextView
    private lateinit var tradePlanDescription: TextView

    override fun getLayoutId(): Int {
        return R.layout.fragment_tradeplan
    }

    override fun initViews(view: View) {
        tradePlanName = view.findViewById(R.id.tradeplan_detail_name)
        tradePlanDescription = view.findViewById(R.id.tradeplan_detail_description)

        // Get trade plan ID from arguments
        val args = arguments
        if (args != null) {
            val tradePlanId = args.getString(ARG_TRADEPLAN_ID)
            if (tradePlanId != null) {
                tradePlanName.text = "Trade Plan ID: $tradePlanId"
                tradePlanDescription.text = "This is a detailed view of trade plan #$tradePlanId."
            }
        }

        // Set up back button
        val backButton = view.findViewById<TextView>(R.id.tradeplan_detail_back_button)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun setupObservers() {
        // No observers needed for this simple implementation
    }
}