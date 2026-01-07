package com.autodroid.aas.ui.adapters

import com.autodroid.aas.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.aas.database.ElementFeature
import java.text.SimpleDateFormat
import java.util.*

class ElementFeatureAdapter(
    private var features: List<ElementFeature> = emptyList()
) : RecyclerView.Adapter<ElementFeatureAdapter.ViewHolder>() {

    fun updateFeatures(newFeatures: List<ElementFeature>) {
        features = newFeatures
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val elementId: TextView = view.findViewById(R.id.tv_element_id)
        val elementType: TextView = view.findViewById(R.id.tv_element_type)
        val elementText: TextView = view.findViewById(R.id.tv_element_text)
        val elementHint: TextView = view.findViewById(R.id.tv_element_hint)
        val usageCount: TextView = view.findViewById(R.id.tv_usage_count)
        val lastUsed: TextView = view.findViewById(R.id.tv_last_used)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_element_feature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feature = features[position]

        holder.elementId.text = feature.elementId ?: feature.elementSignature.take(20)
        holder.elementType.text = "Type: ${feature.elementType}"
        holder.elementText.text = "Text: ${feature.elementContentDesc ?: feature.elementText ?: "N/A"}"
        holder.elementHint.text = "Hint: ${feature.elementHint ?: "N/A"}"
        holder.usageCount.text = "Usage: ${feature.usageCount}"
        
        val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        holder.lastUsed.text = "Last used: ${formatter.format(Date(feature.lastUsedTime))}"
    }

    override fun getItemCount(): Int = features.size
}