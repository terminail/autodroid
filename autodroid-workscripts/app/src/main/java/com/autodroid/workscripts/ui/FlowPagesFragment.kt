package com.autodroid.workscripts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.workscripts.R
import com.autodroid.workscripts.model.NavigationItem

/**
 * 显示流程页面列表的Fragment
 */
class FlowPagesFragment : Fragment() {
    
    companion object {
        private const val ARG_FLOW_ITEM = "flow_item"
        
        fun newInstance(flowItem: NavigationItem.FlowItem): FlowPagesFragment {
            val fragment = FlowPagesFragment()
            val args = Bundle()
            // 将flowItem的基本信息存入Bundle
            args.putString("flow_name", flowItem.name)
            args.putString("flow_description", flowItem.description)
            args.putSerializable("pages", ArrayList(flowItem.pages ?: emptyList()))
            fragment.arguments = args
            return fragment
        }
    }
    
    private lateinit var flowNameTextView: TextView
    private lateinit var flowDescriptionTextView: TextView
    private lateinit var pagesRecyclerView: RecyclerView
    private lateinit var pagesAdapter: FlowPagesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_flow_pages, container, false)
        
        flowNameTextView = view.findViewById(R.id.flowNameTextView)
        flowDescriptionTextView = view.findViewById(R.id.flowDescriptionTextView)
        pagesRecyclerView = view.findViewById(R.id.pagesRecyclerView)
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 设置流程信息
        val flowName = arguments?.getString("flow_name") ?: ""
        val flowDescription = arguments?.getString("flow_description") ?: ""
        val pages = arguments?.getSerializable("pages") as? ArrayList<NavigationItem.PageItem> ?: arrayListOf()
        
        flowNameTextView.text = flowName
        flowDescriptionTextView.text = flowDescription
        
        // 设置页面列表
        pagesAdapter = FlowPagesAdapter(pages) { pageItem ->
            onPageClick(pageItem)
        }
        pagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        pagesRecyclerView.adapter = pagesAdapter
    }
    
    private fun onPageClick(pageItem: NavigationItem.PageItem) {
        // 显示页面加载提示
        android.widget.Toast.makeText(
            requireContext(),
            "加载页面: ${pageItem.fullPath}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        // 这里可以添加页面加载逻辑
        // showPageFragment(pageItem)
    }
    
    /**
     * 流程页面适配器
     */
    private inner class FlowPagesAdapter(
        private val pages: List<NavigationItem.PageItem>,
        private val onPageClick: (NavigationItem.PageItem) -> Unit
    ) : RecyclerView.Adapter<FlowPagesAdapter.PageViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_flow_page, parent, false)
            return PageViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bind(pages[position])
        }
        
        override fun getItemCount(): Int = pages.size
        
        inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val pageNameTextView: TextView = itemView.findViewById(R.id.pageNameTextView)
            private val pageDescriptionTextView: TextView = itemView.findViewById(R.id.pageDescriptionTextView)
            
            fun bind(pageItem: NavigationItem.PageItem) {
                pageNameTextView.text = pageItem.name
                
                // 设置页面描述
                if (pageItem.description.isNotEmpty()) {
                    pageDescriptionTextView.text = pageItem.description
                    pageDescriptionTextView.visibility = View.VISIBLE
                } else {
                    pageDescriptionTextView.visibility = View.GONE
                }
                
                itemView.setOnClickListener {
                    onPageClick(pageItem)
                }
            }
        }
    }
}