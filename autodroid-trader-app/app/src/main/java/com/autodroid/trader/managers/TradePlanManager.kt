// TradePlanManager.kt
package com.autodroid.trader.managers

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.autodroid.trader.R
import com.autodroid.trader.AppViewModel
import com.autodroid.trader.model.TradePlan
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class TradePlanManager(private val context: Context?, private val appViewModel: AppViewModel) {
    private val gson: Gson
    private val inflater: LayoutInflater

    init {
        this.gson = Gson()
        this.inflater = LayoutInflater.from(context)
    }

    fun handleTradePlans(tradeplansJson: String?) {
        try {
            val tradeplansElement =
                gson.fromJson<JsonElement>(tradeplansJson, JsonElement::class.java)
            val tradeplansLists: MutableList<TradePlan> =
                ArrayList<TradePlan>()

            if (tradeplansElement.isJsonObject()) {
                tradeplansLists.add(parseTradePlanObject(tradeplansElement.getAsJsonObject()))
            } else if (tradeplansElement.isJsonArray()) {
                val tradeplansArray = tradeplansElement.getAsJsonArray()
                for (tradeplanElement in tradeplansArray) {
                    if (tradeplanElement.isJsonObject()) {
                        tradeplansLists.add(parseTradePlanObject(tradeplanElement.getAsJsonObject()))
                    }
                }
            }

            appViewModel.setAvailableTradePlans(tradeplansLists)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse tradeplans: " + e.message)
        }
    }

    private fun parseTradePlanObject(tradeplan: JsonObject): TradePlan {
        return TradePlan(
            id = if (tradeplan.has("id")) tradeplan.get("id").getAsString() else null,
            name = if (tradeplan.has("name")) tradeplan.get("name").getAsString() else null,
            title = if (tradeplan.has("title")) tradeplan.get("title").getAsString() else null,
            subtitle = if (tradeplan.has("subtitle")) tradeplan.get("subtitle").getAsString() else null,
            description = if (tradeplan.has("description")) tradeplan.get("description").getAsString() else null,
            status = if (tradeplan.has("status")) tradeplan.get("status").getAsString() else null
        )
    }

    fun updateTradePlansUI(
        tradeplans: MutableList<TradePlan>?,
        container: LinearLayout,
        titleView: TextView
    ) {
        container.removeAllViews()

        if (tradeplans == null || tradeplans.isEmpty()) {
            titleView.setText("No trade plans available")
        } else {
            titleView.setText("Available Trade Plans")

            for (tradeplan in tradeplans) {
                val tradeplanItem = inflater.inflate(R.layout.item_tradeplan, null)

                val tradeplanName = tradeplanItem.findViewById<TextView>(R.id.tradeplan_item_name)
                val tradeplanDescription =
                    tradeplanItem.findViewById<TextView>(R.id.tradeplan_item_description)

                // Use title if available, otherwise use name
                val displayName = tradeplan.title ?: tradeplan.name ?: "Unknown Trade Plan"
                tradeplanName.setText(displayName)
                
                // Use subtitle if available, otherwise use description
                val displayDescription = tradeplan.subtitle ?: tradeplan.description ?: ""
                tradeplanDescription.setText(displayDescription)

                container.addView(tradeplanItem)
            }
        }
    }

    companion object {
        private const val TAG = "TradePlanManager"
    }
}