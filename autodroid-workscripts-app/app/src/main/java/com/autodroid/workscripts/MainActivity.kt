package com.autodroid.workscripts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.workscripts.adapter.NavigationAdapter
import com.autodroid.workscripts.model.NavigationItem
import com.autodroid.workscripts.fragment.FlowFragment
import com.autodroid.workscripts.fragment.StepFragment
import com.autodroid.workscripts.utils.AppScanner

/**
 * Main Activity - Displays expandable folder navigation with single RecyclerView
 * 显示可展开的文件夹导航，使用单个RecyclerView
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NavigationAdapter
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
        
        adapter = NavigationAdapter(
            onAppClick = { appItem ->
                onAppClick(appItem)
            },
            onFlowClick = { flowItem ->
                onFlowClick(flowItem)
            },
            onPageClick = { pageItem ->
                onStepClick(pageItem)
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
        showFlowStepsFragment(flowItem)
    }
    
    private fun onStepClick(pageItem: NavigationItem.StepItem) {
        // 显示页面加载提示
        Toast.makeText(
            this, 
            "加载页面: ${pageItem.fullPath}", 
            Toast.LENGTH_SHORT
        ).show()
        
        // 显示页面Fragment
        showStepPageFragment(pageItem)
    }
    
    private fun showFlowStepsFragment(flowItem: NavigationItem.FlowItem) {
        try {
            // 创建并显示流程页面Fragment
            val fragment = FlowFragment.newInstance(flowItem)
            
            // 使用FragmentTransaction替换当前内容
            val transaction = supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("flow_pages_${flowItem.name}")
            
            // Commit the transaction safely
            if (!isFinishing && !isDestroyed) {
                transaction.commitAllowingStateLoss()
            }
            
            // 显示fragment容器，隐藏导航列表 with animation
            val fragmentContainer = findViewById<android.widget.FrameLayout>(R.id.fragmentContainer)
            fragmentContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error showing flow pages fragment", e)
            Toast.makeText(
                this,
                "显示流程页面失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun showStepPageFragment(pageItem: NavigationItem.StepItem) {
        try {
            // 创建页面 Fragment - 使用 StepDetailFragment 来处理 XML 转换
            val fragment = StepFragment.newInstance(
                pageItem,
                pageItem.layoutResourceName
            )
            
            // 使用FragmentTransaction替换fragment容器而不是整个Activity内容
            val transaction = supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("step_detail_${pageItem.name}")
            
            // Commit the transaction safely
            if (!isFinishing && !isDestroyed) {
                transaction.commitAllowingStateLoss()
            }
                
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error showing page fragment", e)
            Toast.makeText(
                this,
                "显示页面失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onBackPressed() {
        // 检查Fragment返回栈
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            
            // 延迟检查返回栈状态，确保UI更新完成
            recyclerView.post {
                // 如果返回栈为空，显示导航列表
                if (supportFragmentManager.backStackEntryCount == 0) {
                    val fragmentContainer = findViewById<android.widget.FrameLayout>(R.id.fragmentContainer)
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
                                .start()
                        }
                        .start()
                }
            }
        } else {
            super.onBackPressed()
        }
    }
    
    /**
     * Refresh the navigation UI
     */
    fun refreshNavigation() {
        try {
            // Reload navigation data to ensure it's up to date
            loadNavigationData()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Could not refresh navigation", e)
        }
    }
}