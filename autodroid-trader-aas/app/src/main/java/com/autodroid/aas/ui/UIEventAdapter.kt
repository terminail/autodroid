package com.autodroid.aas.ui.adapters

import com.autodroid.aas.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.aas.database.UIEvent
import java.text.SimpleDateFormat
import java.util.*

class UIEventAdapter(
    private var events: List<UIEvent> = emptyList()
) : RecyclerView.Adapter<UIEventAdapter.ViewHolder>() {
    
    fun updateEvents(newEvents: List<UIEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val packageName: TextView = view.findViewById(R.id.package_name)
        val eventType: TextView = view.findViewById(R.id.event_type)
        val elementText: TextView = view.findViewById(R.id.element_text)
        val elementId: TextView = view.findViewById(R.id.element_id)
        val elementClass: TextView = view.findViewById(R.id.element_class)
        val eventTime: TextView = view.findViewById(R.id.event_time)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ui_event, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        
        holder.packageName.text = event.packageName
        holder.eventType.text = event.eventType
        holder.elementText.text = event.elementText ?: "N/A"
        holder.elementId.text = event.elementId ?: "N/A"
        holder.elementClass.text = event.elementClass ?: "N/A"
        
        val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        holder.eventTime.text = formatter.format(Date(event.eventTime))
    }
    
    override fun getItemCount(): Int = events.size
}