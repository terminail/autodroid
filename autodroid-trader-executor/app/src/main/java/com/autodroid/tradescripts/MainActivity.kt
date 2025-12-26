package com.autodroid.tradescripts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.tradescripts.fragment.FlowFragment
import com.autodroid.tradescripts.fragment.StepFragment
import com.autodroid.tradescripts.model.NavigationItem
import com.autodroid.tradescripts.utils.AppScanner

/**
 * Main Activity - Displays expandable folder navigation with single RecyclerView
 * 显示可展开的文件夹导航，使用单个RecyclerView
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MainAdapter
    private var apkItems: List<NavigationItem.ApkItem> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupBackPressedHandler()
        setupRecyclerView()
        loadNavigationData()
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = MainAdapter(
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
        if (apkItems.isEmpty()) {
            // 使用应用扫描器获取应用数据
            apkItems = AppScanner.scanApps(this)
        }
        
        adapter.setData(apkItems)
        
        // 显示扫描结果
        val totalApps = apkItems.size
        val totalFlows = apkItems.sumOf { it.flows?.size ?: 0 }
        val totalPages = apkItems.sumOf { app ->
            app.flows?.sumOf { it.steps?.size ?: 0 } ?: 0
        }
        
        Toast.makeText(
            this, 
            "扫描到 $totalApps 个应用，$totalFlows 个流程，$totalPages 个页面", 
            Toast.LENGTH_SHORT
        ).show()
    }
    
    // 不再需要的辅助方法已删除
    
    private fun onAppClick(apkItem: NavigationItem.ApkItem) {
        // 切换应用的展开/收起状态
        adapter.toggleAppExpansion(apkItem)
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
            android.util.Log.e("MainActivity", "Error showing flow apks fragment", e)
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
    
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    
                    recyclerView.post {
                        if (supportFragmentManager.backStackEntryCount == 0) {
                            val fragmentContainer = findViewById<android.widget.FrameLayout>(R.id.fragmentContainer)
                            fragmentContainer.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    fragmentContainer.visibility = View.GONE
                                    fragmentContainer.alpha = 1f
                                    recyclerView.visibility = View.VISIBLE
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
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    /**
     * Refresh the navigation UI
     */
    fun refreshNavigation() {
        try {
            loadNavigationData()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Could not refresh navigation", e)
        }
    }
}
