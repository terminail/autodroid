// WorkScriptDetailFragment.kt
package com.autodroid.manager.ui.workscripts.detail

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.autodroid.manager.R
import com.autodroid.manager.ui.BaseFragment

class WorkScriptDetailFragment : BaseFragment() {
    companion object {
        private const val ARG_WORKSCRIPT_ID = "workscript_id"
        
        fun newInstance(workScriptId: String): WorkScriptDetailFragment {
            val fragment = WorkScriptDetailFragment()
            val args = Bundle()
            args.putString(ARG_WORKSCRIPT_ID, workScriptId)
            fragment.arguments = args
            return fragment
        }
    }
    
    private lateinit var workScriptName: TextView
    private lateinit var workScriptDescription: TextView

    override fun getLayoutId(): Int {
        return R.layout.fragment_workscript_detail
    }

    override fun initViews(view: View) {
        workScriptName = view.findViewById(R.id.workscript_detail_name)
        workScriptDescription = view.findViewById(R.id.workscript_detail_description)
        
        // Get workscript ID from arguments
        val args = arguments
        if (args != null) {
            val workScriptId = args.getString(ARG_WORKSCRIPT_ID)
            if (workScriptId != null) {
                workScriptName.text = "WorkScript ID: $workScriptId"
                workScriptDescription.text = "This is a detailed view of workscript #$workScriptId."
            }
        }
        
        // Set up back button
        val backButton = view.findViewById<TextView>(R.id.workscript_detail_back_button)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun setupObservers() {
        // No observers needed for this simple implementation
    }
}