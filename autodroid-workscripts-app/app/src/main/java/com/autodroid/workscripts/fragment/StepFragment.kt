package com.autodroid.workscripts.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.autodroid.workscripts.R
import com.autodroid.workscripts.model.NavigationItem
import com.autodroid.workscripts.utils.AppiumXmlParser
import com.autodroid.workscripts.utils.ControlGridAdapter
import com.autodroid.workscripts.utils.ControlInfo
import java.io.File
import java.io.StringReader
import org.xmlpull.v1.XmlPullParserFactory
open class StepFragment : Fragment() {
    private lateinit var gridView: GridView
    private lateinit var headerTitleTextView: TextView
    private lateinit var controlList: List<ControlInfo>
    private lateinit var adapter: ControlGridAdapter
    
    companion object {
        private const val ARG_PAGE_ITEM = "page_item"
        private const val ARG_STEP_FILE_PATH = "step_file_path"
        
        fun newInstance(pageItem: NavigationItem.StepItem, stepFilePath: String): StepFragment {
            val fragment = StepFragment()
            val args = Bundle()
            args.putSerializable(ARG_PAGE_ITEM, pageItem)
            args.putString(ARG_STEP_FILE_PATH, stepFilePath)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_step, container, false)
        this.gridView = view.findViewById(R.id.grid_view)
        this.headerTitleTextView = view.findViewById(R.id.headerTitleTextView)
        
        // Set background image if available
        val pageItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_PAGE_ITEM, NavigationItem.StepItem::class.java) as? NavigationItem.StepItem
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_PAGE_ITEM) as? NavigationItem.StepItem
        }
        
        if (pageItem != null && pageItem.screenshots.isNotEmpty()) {
            val firstScreenshot = pageItem.screenshots.firstOrNull()
            if (firstScreenshot != null) {
                loadAndSetBackground(view, firstScreenshot, pageItem.fullPath)
            }
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up back button
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            handleBackNavigation()
        }
        
        // Set the header title to the step name
        val pageItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_PAGE_ITEM, NavigationItem.StepItem::class.java) as? NavigationItem.StepItem
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_PAGE_ITEM) as? NavigationItem.StepItem
        }
        if (pageItem != null) {
            headerTitleTextView.text = pageItem.name
        }
        
        loadAndDisplayControls()
    }
    
    private fun loadAndSetBackground(view: View, screenshotName: String, fullPath: String) {
        try {
            // Extract the app package name from the full path
            // Expected format: pages/com.tdx.androidCCZQ/netgrid-trading/config.yaml
            val pathParts = fullPath.split("/")
            if (pathParts.size >= 3) {
                val appPackageName = pathParts[1] // e.g., "com.tdx.androidCCZQ"
                val flowName = pathParts[2]       // e.g., "netgrid-trading"
                
                // Construct the full asset path for the screenshot
                val screenshotPath = "pages/$appPackageName/$flowName/$screenshotName"
                Log.d("StepPageFragment", "Loading screenshot from: $screenshotPath")
                
                // Load the bitmap from assets
                val inputStream = requireContext().assets.open(screenshotPath)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                // Scale bitmap to fit the screen
                val scaledBitmap = scaleBitmapToScreen(bitmap)
                
                // Set as background
                val drawable = BitmapDrawable(resources, scaledBitmap)
                view.background = drawable
            }
        } catch (e: Exception) {
            Log.e("StepPageFragment", "Error loading background image: ${e.message}", e)
            // Silently fail - don't show error to user
        }
    }
    
    private fun scaleBitmapToScreen(bitmap: Bitmap): Bitmap {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Calculate scale factors
        val scaleX = screenWidth.toFloat() / bitmap.width
        val scaleY = screenHeight.toFloat() / bitmap.height
        
        // Use the larger scale factor to ensure the image covers the entire screen
        val scale = maxOf(scaleX, scaleY)
        
        // Calculate new dimensions
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        // Scale the bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    
    
    private fun loadAndDisplayControls() {
        try {
            // Get the step file path from arguments or use default
            val stepFilePath = arguments?.getString(ARG_STEP_FILE_PATH) ?: "pages/cn.com.gjzq.yjb2/testflowa/step1.xml"
            
            Log.d("AppiumXml", "Loading XML file: $stepFilePath")
            
            // Ensure the path starts with "pages/"
            val fullPath = if (stepFilePath.startsWith("pages/")) {
                stepFilePath
            } else {
                "pages/$stepFilePath"
            }
            
            Log.d("AppiumXml", "Full path: $fullPath")
            
            // Load the step XML file from the correct path
            val inputStream = requireContext().assets.open(fullPath)
            
            // Parse XML and extract control information
            val parser = AppiumXmlParser()
            controlList = parser.parseXml(inputStream)
            
            Log.d("AppiumXml", "Found ${controlList.size} controls")
            
            // Set up GridView adapter
            adapter = ControlGridAdapter(requireContext(), controlList)
            gridView.adapter = adapter
            
            // Set click listener for grid items
            gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                showControlDetails(controlList[position])
            }
            
            // Show success message
            Toast.makeText(requireContext(), "找到 ${controlList.size} 个控件", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e("AppiumXml", "Error loading XML: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(requireContext(), "解析XML失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle back navigation from the step detail fragment
     */
    private fun handleBackNavigation() {
        try {
            // Navigate back to previous screen
            parentFragmentManager.popBackStack()
        } catch (e: Exception) {
            Log.e("StepDetailFragment", "Error handling back navigation", e)
            // Show error message to user
            Toast.makeText(requireContext(), "返回上一页失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show control details dialog
     */
    private fun showControlDetails(control: ControlInfo) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("控件详情")
        
        val details = StringBuilder()
        details.append("类型: ").append(control.type).append("\n\n")
        details.append("文本: ").append(control.text ?: "无").append("\n\n")
        details.append("ClassName: ").append(control.className).append("\n\n")
        details.append("ResourceId: ").append(control.resourceId ?: "无").append("\n\n")
        details.append("XPath: ").append(control.xpath).append("\n\n")
        
        val bounds = control.bounds
        details.append("位置: [").append(bounds.left).append(",").append(bounds.top)
                .append("][").append(bounds.right).append(",").append(bounds.bottom).append("]\n\n")
        
        details.append("宽度: ").append(bounds.width()).append("px\n")
        details.append("高度: ").append(bounds.height()).append("px\n")
        details.append("可点击: ").append(if (control.clickable) "是" else "否").append("\n")
        details.append("可见: ").append(if (control.visible) "是" else "否")
        
        builder.setMessage(details.toString())
        builder.setPositiveButton("确定") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    
    
    
}