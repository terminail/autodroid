package com.autodroid.workscripts.fragment

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.workscripts.MainActivity
import com.autodroid.workscripts.R
import com.autodroid.workscripts.model.NavigationItem

/**
 * 显示流程页面列表的Fragment
 */
class FlowFragment : Fragment() {
    companion object {
        public const  val TAG: String="FlowStepsFragment"

        private const val ARG_FLOW_ITEM = "flow_item"
        
        fun newInstance(flowItem: NavigationItem.FlowItem): FlowFragment {
            val fragment = FlowFragment()
            val args = Bundle()
            // 将flowItem的基本信息存入Bundle
            args.putString("flow_name", flowItem.name)
            args.putString("flow_description", flowItem.description)
            args.putSerializable("pages", ArrayList(flowItem.pages ?: emptyList()))
            fragment.arguments = args
            return fragment
        }
    }
    
    private lateinit var headerTitleTextView: TextView
    private lateinit var flowNameTextView: TextView
    private lateinit var flowDescriptionTextView: TextView
    private lateinit var pagesRecyclerView: RecyclerView
    private lateinit var pagesAdapter: FlowStepsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_flow, container, false)
        
        headerTitleTextView = view.findViewById(R.id.headerTitleTextView)
        flowNameTextView = view.findViewById(R.id.flowNameTextView)
        flowDescriptionTextView = view.findViewById(R.id.flowDescriptionTextView)
        pagesRecyclerView = view.findViewById(R.id.pagesRecyclerView)
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            // Set up back button
            val backButton = view.findViewById<ImageButton>(R.id.back_button)
            backButton.setOnClickListener {
                handleBackNavigation()
            }
            
            // 设置流程信息
            val flowName = arguments?.getString("flow_name") ?: ""
            val flowDescription = arguments?.getString("flow_description") ?: ""
            val pages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("pages", ArrayList::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("pages") as? ArrayList<*> ?: arrayListOf()
        }
            
            headerTitleTextView.text = flowName
            flowNameTextView.text = flowName
            flowDescriptionTextView.text = flowDescription
            
            // 设置页面列表
            pagesAdapter = FlowStepsAdapter(pages) { pageItem ->
                onPageClick(pageItem)
            }
            pagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            pagesRecyclerView.adapter = pagesAdapter
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            Toast.makeText(
                requireContext(),
                "初始化页面失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun onPageClick(pageItem: NavigationItem.StepItem) {
        try {
            // 显示步骤详情Fragment
            val stepFilePath = getStepFilePath(pageItem)
            if (stepFilePath.isNotEmpty()) {
                showStepDetailFragment(pageItem, stepFilePath)
            } else {
                // 显示页面加载提示
                Toast.makeText(
                    requireContext(),
                    "加载页面: ${pageItem.fullPath}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "加载页面失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun getStepFilePath(pageItem: NavigationItem.StepItem): String {
        // 从页面项的fullPath中提取步骤文件路径
        // 格式示例: cn.com.gjzq.yjb2/testflowa/step1.xml
        // 由于fullPath移除了.xml后缀，我们需要重新添加
        val fullPath = pageItem.fullPath
        return if (fullPath.endsWith(".xml")) {
            fullPath
        } else {
            "$fullPath.xml"
        }
    }
    
    private fun showStepDetailFragment(pageItem: NavigationItem.StepItem, stepFilePath: String) {
        try {
            val fragment = StepFragment.newInstance(pageItem, stepFilePath)
            
            // 使用FragmentTransaction替换当前内容
            val transaction = parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("step_detail_${pageItem.name}")
            
            // Commit the transaction safely
            if (isAdded && activity != null && !requireActivity().isFinishing) {
                transaction.commitAllowingStateLoss()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing step detail fragment", e)
            Toast.makeText(
                requireContext(),
                "显示页面失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
 * Handle back navigation from the flow pages fragment
 */
    private fun handleBackNavigation() {
        try {
            // Check if there are fragments in the back stack
            if (parentFragmentManager.backStackEntryCount > 1) {
                // Pop the current fragment and return to the previous one
                parentFragmentManager.popBackStack()
            } else {
                // If this is the last fragment, go back to the main navigation
                parentFragmentManager.popBackStack()
                
                // Show the navigation list and hide the fragment container with animation
                showMainNavigationWithAnimation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling back navigation", e)
            // Fallback: directly manipulate UI if back stack operation fails
            showMainNavigationWithAnimation()
        }
    }
    
    /**
     * Show the main navigation and hide the fragment container with animation
     */
    private fun showMainNavigationWithAnimation() {
        try {
            val fragmentContainer = requireActivity().findViewById<FrameLayout>(R.id.fragmentContainer)
            val recyclerView = requireActivity().findViewById<RecyclerView>(R.id.recyclerView)
            
            // Ensure UI updates happen after the back stack operation completes
            fragmentContainer.post {
                // 添加淡出动画效果
                fragmentContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        fragmentContainer.visibility = View.GONE
                        fragmentContainer.alpha = 1f // Reset alpha for next time
                        recyclerView.visibility = View.VISIBLE
                        // 添加淡入动画效果
                        recyclerView.alpha = 0f
                        recyclerView.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .withEndAction {
                                // Notify adapter to refresh if needed
                                try {
                                    val mainActivity = activity as? MainActivity
                                    mainActivity?.refreshNavigation()
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not refresh navigation", e)
                                }
                            }
                            .start()
                    }
                    .start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing main navigation", e)
            // Direct UI manipulation as final fallback
            try {
                val fragmentContainer = requireActivity().findViewById<FrameLayout>(R.id.fragmentContainer)
                val recyclerView = requireActivity().findViewById<RecyclerView>(R.id.recyclerView)
                fragmentContainer.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                
                // Notify adapter to refresh if needed
                try {
                    val mainActivity = activity as? MainActivity
                    mainActivity?.refreshNavigation()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not refresh navigation", e)
                }
            } catch (uiException: Exception) {
                Log.e(TAG, "Error updating UI directly", uiException)
            }
        }
    }
    
    /**
     * 流程页面适配器
     */
    private inner class FlowStepsAdapter(
        private val pages: java.util.ArrayList<out Any>,
        private val onPageClick: (NavigationItem.StepItem) -> Unit
    ) : RecyclerView.Adapter<FlowStepsAdapter.StepViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_step, parent, false)
            return StepViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
            holder.bind(pages[position] as NavigationItem.StepItem)
        }
        
        override fun getItemCount(): Int = pages.size
        
        inner class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val pageNameTextView: TextView = itemView.findViewById(R.id.pageNameTextView)
            private val pageDescriptionTextView: TextView = itemView.findViewById(R.id.pageDescriptionTextView)
            
            fun bind(pageItem: NavigationItem.StepItem) {
                try {
                    pageNameTextView.text = pageItem.name
                    
                    // 设置页面描述
                    if (pageItem.description.isNotEmpty()) {
                        pageDescriptionTextView.text = pageItem.description
                        pageDescriptionTextView.visibility = View.VISIBLE
                    } else {
                        pageDescriptionTextView.visibility = View.GONE
                    }
                    
                    itemView.setOnClickListener {
                        try {
                            onPageClick(pageItem)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling page click", e)
                            Toast.makeText(
                                itemView.context,
                                "页面点击失败: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error binding page item", e)
                }
            }
        }
    }
}