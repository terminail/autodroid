package com.autodroid.workscripts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.workscripts.adapter.ExpandableNavigationAdapter
import com.autodroid.workscripts.fragment.PageFragment
import com.autodroid.workscripts.model.NavigationItem
import com.autodroid.workscripts.ui.FlowPagesFragment
import com.autodroid.workscripts.ui.StepDetailFragment
import com.autodroid.workscripts.utils.AppScanner

/**
 * Main Activity - Displays expandable folder navigation with single RecyclerView
 * 显示可展开的文件夹导航，使用单个RecyclerView
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpandableNavigationAdapter
    private var appItems: List<NavigationItem.AppItem> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupRecyclerView()
        loadNavigationData()
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = ExpandableNavigationAdapter(
            onAppClick = { appItem ->
                onAppClick(appItem)
            },
            onFlowClick = { flowItem ->
                onFlowClick(flowItem)
            },
            onPageClick = { pageItem ->
                onPageClick(pageItem)
            }
        )
        
        recyclerView.adapter = adapter
    }
    
    private fun loadNavigationData() {
        // 只在首次加载时扫描应用数据
        if (appItems.isEmpty()) {
            // 使用应用扫描器获取应用数据
            appItems = AppScanner.scanApps(this)
        }
        
        adapter.setData(appItems)
        
        // 显示扫描结果
        val totalApps = appItems.size
        val totalFlows = appItems.sumOf { it.flows?.size ?: 0 }
        val totalPages = appItems.sumOf { app -> 
            app.flows?.sumOf { it.pages?.size ?: 0 } ?: 0 
        }
        
        Toast.makeText(
            this, 
            "扫描到 $totalApps 个应用，$totalFlows 个流程，$totalPages 个页面", 
            Toast.LENGTH_SHORT
        ).show()
    }
    
    // 不再需要的辅助方法已删除
    
    private fun onAppClick(appItem: NavigationItem.AppItem) {
        // 切换应用的展开/收起状态
        adapter.toggleAppExpansion(appItem)
    }
    
    private fun onFlowClick(flowItem: NavigationItem.FlowItem) {
        // 显示流程页面Fragment
        showFlowPagesFragment(flowItem)
    }
    
    private fun onPageClick(pageItem: NavigationItem.PageItem) {
        // 显示页面加载提示
        Toast.makeText(
            this, 
            "加载页面: ${pageItem.fullPath}", 
            Toast.LENGTH_SHORT
        ).show()
        
        // 显示页面Fragment
        showPageFragment(pageItem)
    }
    
    private fun showFlowPagesFragment(flowItem: NavigationItem.FlowItem) {
        // 创建并显示流程页面Fragment
        val fragment = FlowPagesFragment.newInstance(flowItem)
        
        // 使用FragmentTransaction替换当前内容
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("flow_pages")
            .commit()
            
        // 显示fragment容器，隐藏导航列表
        findViewById<android.widget.FrameLayout>(R.id.fragmentContainer).visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
    
    private fun showPageFragment(pageItem: NavigationItem.PageItem) {
        try {
            // 创建页面 Fragment - 使用 StepDetailFragment 来处理 XML 转换
            val fragment = StepDetailFragment.newInstance(
                pageItem,
                pageItem.layoutResourceName
            )
            
            // 替换整个Activity内容为Fragment
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null) // 添加到返回栈，支持返回键
                .commit()
                
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "显示页面失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onBackPressed() {
        // 如果有返回栈，弹出栈顶fragment
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            // 隐藏fragment容器，显示导航列表
            findViewById<android.widget.FrameLayout>(R.id.fragmentContainer).visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }
}