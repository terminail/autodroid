// WorkScriptAdapter.kt
package com.autodroid.manager.ui.workscripts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.manager.R
import com.autodroid.manager.model.WorkScript
import com.autodroid.manager.ui.workscripts.WorkScriptAdapter.WorkScriptViewHolder

class WorkScriptAdapter(
    private var workscripts: MutableList<WorkScript>?,
    private val listener: OnWorkScriptClickListener?
) : RecyclerView.Adapter<WorkScriptViewHolder?>() {
    interface OnWorkScriptClickListener {
        fun onWorkScriptClick(workScript: WorkScript?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkScriptViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.workscript_item, parent, false)
        return WorkScriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkScriptViewHolder, position: Int) {
        val workScript = workscripts!!.get(position)
        holder.bind(workScript)
    }

    override fun getItemCount(): Int {
        return if (workscripts != null) workscripts!!.size else 0
    }

    fun updateWorkScripts(newWorkScripts: MutableList<WorkScript>?) {
        this.workscripts = newWorkScripts
        notifyDataSetChanged()
    }

    inner class WorkScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView
        private val descriptionTextView: TextView
        private val packageTextView: TextView

        init {
            nameTextView = itemView.findViewById<TextView>(R.id.workscript_item_name)
            descriptionTextView = itemView.findViewById<TextView>(R.id.workscript_item_description)
            packageTextView = itemView.findViewById<TextView>(R.id.workscript_item_package)
        }

        fun bind(workScript: WorkScript) {
            // Use title if available, otherwise use name
            val displayName = workScript.title ?: workScript.name ?: "Unknown WorkScript"
            nameTextView.setText(displayName)

            // Use subtitle if available, otherwise use description
            val displayDescription = workScript.subtitle ?: workScript.description ?: ""
            if (displayDescription.isEmpty()) {
                descriptionTextView.setVisibility(View.GONE)
            } else {
                descriptionTextView.setVisibility(View.VISIBLE)
                descriptionTextView.setText(displayDescription)
            }

            // Show status if available
            if (!workScript.status.isNullOrEmpty()) {
                packageTextView.setText(workScript.status)
                packageTextView.setVisibility(View.VISIBLE)
            } else {
                packageTextView.setVisibility(View.GONE)
            }

            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    listener.onWorkScriptClick(workScript)
                }
            })
        }
    }
}