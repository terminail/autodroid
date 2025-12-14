package com.autodroid.workscripts.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.workscripts.utils.LayoutHelper

class PageFragment : Fragment() {
    
    companion object {
        private const val ARG_LAYOUT_RESOURCE_NAME = "layout_resource_name"
        private const val ARG_PAGE_NAME = "page_name"
        
        fun newInstance(layoutResourceName: String, pageName: String): PageFragment {
            return PageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LAYOUT_RESOURCE_NAME, layoutResourceName)
                    putString(ARG_PAGE_NAME, pageName)
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutResourceName = arguments?.getString(ARG_LAYOUT_RESOURCE_NAME)
        val pageName = arguments?.getString(ARG_PAGE_NAME)
        
        if (layoutResourceName != null) {
            // 使用 LayoutHelper 从子文件夹中加载布局
            val layoutView = LayoutHelper.inflateLayoutFromSubfolder(
                requireContext(),
                layoutResourceName
            )
            
            if (layoutView != null) {
                return layoutView
            } else {
                // 如果 LayoutHelper 失败，尝试使用系统方式加载
                try {
                    val resourceId = resources.getIdentifier(
                        layoutResourceName.replace(".xml", ""),
                        "layout",
                        requireContext().packageName
                    )
                    if (resourceId != 0) {
                        return inflater.inflate(resourceId, container, false)
                    }
                } catch (e: Exception) {
                    // Handle exception silently
                }
            }
        }
        
        // 如果都失败了，创建一个简单的错误视图
        return createErrorView(inflater, container, pageName, layoutResourceName)
    }
    
    private fun createErrorView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        pageName: String?,
        layoutResourceName: String?
    ): View {
        val errorView = android.widget.TextView(requireContext()).apply {
            text = "无法加载页面: ${pageName ?: "未知页面"}\n布局资源: ${layoutResourceName ?: "未知"}"
            setPadding(32, 32, 32, 32)
            textSize = 16f
            setTextColor(android.graphics.Color.RED)
        }
        
        val scrollView = android.widget.ScrollView(requireContext()).apply {
            addView(errorView)
        }
        
        return scrollView
    }
}