package com.autodroid.workscripts.ui

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageButton
import androidx.drawerlayout.widget.DrawerLayout
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import android.util.DisplayMetrics
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter
import java.io.InputStream
import java.util.Stack
import com.autodroid.workscripts.R
import com.autodroid.workscripts.model.NavigationItem
import kotlin.math.roundToInt
import android.content.Context
import java.io.File
class StepDetailFragment : Fragment() {
    private lateinit var container: ViewGroup
    private lateinit var displayMetrics: DisplayMetrics // 屏幕参数
    
    companion object {
        private const val ARG_PAGE_ITEM = "page_item"
        private const val ARG_STEP_FILE_PATH = "step_file_path"
        
        fun newInstance(pageItem: NavigationItem.PageItem, stepFilePath: String): StepDetailFragment {
            val fragment = StepDetailFragment()
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
        val view = inflater.inflate(R.layout.fragment_step_detail, container, false)
        this.container = view.findViewById(R.id.ui_container)
        displayMetrics = resources.displayMetrics // 初始化屏幕参数（宽度、高度）
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Ensure the container has proper layout parameters
        // Since the container is a FrameLayout, we need to use FrameLayout.LayoutParams
        container.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        
        // Set up back button
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            // Navigate back to previous screen
            parentFragmentManager.popBackStack()
        }
        
        loadAndRenderXml()
    }

    private fun createDynamicViewsFromAppiumXml(inputStream: InputStream) {
        try {
            // Clear the container first
            container.removeAllViews()
            
            // Parse the Appium XML
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            
            // Get screen dimensions for scaling
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            Log.d("AppiumXml", "Screen dimensions: ${screenWidth}x${screenHeight}")
            
            var eventType = parser.eventType
            var elementCount = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        
                        // Skip the hierarchy element as it's just a container
                        if (tagName == "hierarchy") {
                            eventType = parser.next()
                            continue
                        }
                        
                        // Extract attributes
                        val className = parser.getAttributeValue(null, "class")
                        val bounds = parser.getAttributeValue(null, "bounds")
                        val text = parser.getAttributeValue(null, "text")
                        val resourceId = parser.getAttributeValue(null, "resource-id")
                        
                        Log.d("AppiumXml", "Processing element: class=$className, bounds=$bounds, text=$text")
                        
                        // Only create views for elements with text content
                        if (!text.isNullOrEmpty() && text.isNotBlank()) {
                            elementCount++
                            
                            // Parse bounds to get position and size
                            val (left, top, right, bottom) = parseBounds(bounds)
                            var width = right - left
                            var height = bottom - top
                            
                            // Scale the coordinates to fit the screen
                            var scaledLeft = (left * screenWidth / 1080f).roundToInt()
                            var scaledTop = (top * screenHeight / 1920f).roundToInt()
                            var scaledWidth = (width * screenWidth / 1080f).roundToInt()
                            var scaledHeight = (height * screenHeight / 1920f).roundToInt()
                            
                            // Ensure elements are within screen boundaries
                            // Adjust position if element goes off-screen
                            if (scaledLeft < 0) scaledLeft = 0
                            if (scaledTop < 0) scaledTop = 0
                            
                            // Adjust size if element is too large
                            if (scaledWidth > screenWidth) scaledWidth = screenWidth
                            if (scaledHeight > screenHeight) scaledHeight = screenHeight
                            
                            // Adjust position if element would go off the right or bottom edge
                            if (scaledLeft + scaledWidth > screenWidth) {
                                scaledLeft = screenWidth - scaledWidth
                                if (scaledLeft < 0) scaledLeft = 0
                            }
                            if (scaledTop + scaledHeight > screenHeight) {
                                scaledTop = screenHeight - scaledHeight
                                if (scaledTop < 0) scaledTop = 0
                            }
                            
                            // Ensure minimum size for visibility
                            if (scaledWidth < 50) scaledWidth = 50
                            if (scaledHeight < 30) scaledHeight = 30
                            
                            Log.d("AppiumXml", "Creating view for text: '$text' at ($scaledLeft,$scaledTop) size ${scaledWidth}x${scaledHeight}")
                            
                            // Create a TextView for the element
                            val textView = TextView(requireContext())
                            textView.text = text
                            textView.setTextColor(Color.BLACK)
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                            textView.setBackgroundColor(Color.parseColor("#E0E0E0")) // Light gray background
                            textView.setPadding(8, 4, 8, 4)
                            
                            // Set layout parameters
                            val layoutParams = FrameLayout.LayoutParams(scaledWidth, scaledHeight)
                            layoutParams.leftMargin = scaledLeft
                            layoutParams.topMargin = scaledTop
                            textView.layoutParams = layoutParams
                            
                            // Add to container
                            container.addView(textView)
                            
                            Log.d("AppiumXml", "Added view for text: '$text'")
                        }
                    }
                }
                eventType = parser.next()
            }
            
            Log.d("AppiumXml", "Created $elementCount views with text content")
            
            // Force a layout pass
            container.requestLayout()
            
            // Print view hierarchy after rendering
            container.post {
                Log.d("AppiumXml", "UI rendering completed using dynamic view creation")
                Log.d("AppiumXml", "=== VIEW HIERARCHY ===")
                printViewHierarchy(container, 0)
                Log.d("AppiumXml", "=== END VIEW HIERARCHY ===")
            }
            
        } catch (e: Exception) {
            Log.e("AppiumXml", "Error creating dynamic views", e)
            e.printStackTrace()
            Toast.makeText(requireContext(), "创建动态视图失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadAndRenderXml() {
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
            
            // Parse the Appium XML and create views dynamically for elements with text
            createDynamicViewsFromAppiumXml(inputStream)
            
        } catch (e: Exception) {
            Log.e("AppiumXml", "Error loading XML: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(requireContext(), "解析XML失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun inflateLayout(parser: XmlPullParser, context: Context): View {
        var eventType = parser.eventType
        var rootView: View? = null
        val viewStack = Stack<View>()
        val parentStack = Stack<ViewGroup>()
        
        // Get screen density for pixel conversion
        val density = context.resources.displayMetrics.density
        Log.d("AppiumXml", "Screen density: $density")
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    Log.d("AppiumXml", "Inflating tag: $tagName")
                    
                    val view = when (tagName) {
                        "FrameLayout" -> FrameLayout(context)
                        "LinearLayout" -> LinearLayout(context)
                        "RelativeLayout" -> RelativeLayout(context)
                        "TextView" -> TextView(context)
                        "Button" -> Button(context)
                        "EditText" -> EditText(context)
                        "ImageView" -> ImageView(context)
                        "ImageButton" -> ImageButton(context)
                        "View" -> View(context)
                        "WebView" -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                WebView(context)
                            } else {
                                Log.w("AppiumXml", "Using View fallback for WebView on Android ${Build.VERSION.SDK_INT}")
                                View(context)
                            }
                        }
                        "androidx.drawerlayout.widget.DrawerLayout" -> DrawerLayout(context)
                        else -> View(context)
                    }
                    
                    // Store the root view
                    if (rootView == null) {
                        rootView = view
                    }
                    
                    // Convert dp values to pixels for layout parameters
                    val widthStr = parser.getAttributeValue(null, "android:layout_width") ?: "wrap_content"
                    val heightStr = parser.getAttributeValue(null, "android:layout_height") ?: "wrap_content"
                    
                    val width = when {
                        widthStr.endsWith("dp") -> {
                            val widthDp = widthStr.replace("dp", "").toFloatOrNull() ?: 0f
                            if (widthDp > 0) (widthDp * density).toInt() else ViewGroup.LayoutParams.WRAP_CONTENT
                        }
                        widthStr == "match_parent" -> ViewGroup.LayoutParams.MATCH_PARENT
                        widthStr == "wrap_content" -> ViewGroup.LayoutParams.WRAP_CONTENT
                        else -> widthStr.toIntOrNull() ?: ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    
                    val height = when {
                        heightStr.endsWith("dp") -> {
                            val heightDp = heightStr.replace("dp", "").toFloatOrNull() ?: 0f
                            if (heightDp > 0) (heightDp * density).toInt() else ViewGroup.LayoutParams.WRAP_CONTENT
                        }
                        heightStr == "match_parent" -> ViewGroup.LayoutParams.MATCH_PARENT
                        heightStr == "wrap_content" -> ViewGroup.LayoutParams.WRAP_CONTENT
                        else -> heightStr.toIntOrNull() ?: ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    
                    Log.d("AppiumXml", "Size: ${widthStr} -> ${width}px, ${heightStr} -> ${height}px")
                    
                    val layoutParams = when (view) {
                        is FrameLayout -> FrameLayout.LayoutParams(width, height)
                        is LinearLayout -> LinearLayout.LayoutParams(width, height)
                        is RelativeLayout -> RelativeLayout.LayoutParams(width, height)
                        is DrawerLayout -> DrawerLayout.LayoutParams(width, height)
                        else -> ViewGroup.LayoutParams(width, height)
                    }
                    
                    // Set margins (convert dp to pixels)
                    val marginLeftStr = parser.getAttributeValue(null, "android:layout_marginStart") ?: 
                                       parser.getAttributeValue(null, "android:layout_marginLeft") ?: "0dp"
                    val marginTopStr = parser.getAttributeValue(null, "android:layout_marginTop") ?: "0dp"
                    
                    val marginLeft = when {
                        marginLeftStr.endsWith("dp") -> {
                            val marginLeftDp = marginLeftStr.replace("dp", "").toFloatOrNull() ?: 0f
                            if (marginLeftDp > 0) (marginLeftDp * density).toInt() else 0
                        }
                        else -> marginLeftStr.toIntOrNull() ?: 0
                    }
                    
                    val marginTop = when {
                        marginTopStr.endsWith("dp") -> {
                            val marginTopDp = marginTopStr.replace("dp", "").toFloatOrNull() ?: 0f
                            if (marginTopDp > 0) (marginTopDp * density).toInt() else 0
                        }
                        else -> marginTopStr.toIntOrNull() ?: 0
                    }
                    
                    Log.d("AppiumXml", "Margins: ${marginLeftStr} -> ${marginLeft}px, ${marginTopStr} -> ${marginTop}px")
                    
                    if (layoutParams is ViewGroup.MarginLayoutParams) {
                        layoutParams.leftMargin = marginLeft
                        layoutParams.topMargin = marginTop
                    }
                    
                    // Handle RelativeLayout specific positioning attributes
                    if (layoutParams is RelativeLayout.LayoutParams) {
                        val alignParentStart = parser.getAttributeValue(null, "android:layout_alignParentStart")
                        if (alignParentStart == "true") {
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                        }
                        
                        val alignParentLeft = parser.getAttributeValue(null, "android:layout_alignParentLeft")
                        if (alignParentLeft == "true") {
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                        }
                        
                        val alignParentTop = parser.getAttributeValue(null, "android:layout_alignParentTop")
                        if (alignParentTop == "true") {
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        }
                        
                        val alignParentEnd = parser.getAttributeValue(null, "android:layout_alignParentEnd")
                        if (alignParentEnd == "true") {
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END)
                        }
                        
                        val alignParentRight = parser.getAttributeValue(null, "android:layout_alignParentRight")
                        if (alignParentRight == "true") {
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        }
                        
                        val alignParentBottom = parser.getAttributeValue(null, "android:layout_alignParentBottom")
                        if (alignParentBottom == "true") {
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        }
                        
                        val centerInParent = parser.getAttributeValue(null, "android:layout_centerInParent")
                        if (centerInParent == "true") {
                            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
                        }
                        
                        val centerHorizontal = parser.getAttributeValue(null, "android:layout_centerHorizontal")
                        if (centerHorizontal == "true") {
                            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                        }
                        
                        val centerVertical = parser.getAttributeValue(null, "android:layout_centerVertical")
                        if (centerVertical == "true") {
                            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                        }
                    }
                    
                    view.layoutParams = layoutParams
                    
                    // Set other attributes
                    val text = parser.getAttributeValue(null, "android:text")
                    if (view is TextView && !text.isNullOrEmpty()) {
                        view.text = text
                        view.setTextColor(Color.BLACK)
                        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    }
                    
                    val backgroundColor = parser.getAttributeValue(null, "android:background")
                    if (!backgroundColor.isNullOrEmpty()) {
                        try {
                            view.setBackgroundColor(Color.parseColor(backgroundColor))
                        } catch (e: Exception) {
                            Log.w("AppiumXml", "Failed to parse background color: $backgroundColor")
                            view.setBackgroundColor(Color.WHITE)
                        }
                    } else {
                        view.setBackgroundColor(Color.WHITE)
                    }
                    
                    val clickable = parser.getAttributeValue(null, "android:clickable")
                    if (clickable == "true") {
                        view.setOnClickListener { onViewClick(it) }
                    }
                    
                    // Add to parent if there is one
                    if (parentStack.isNotEmpty()) {
                        parentStack.peek().addView(view)
                    }
                    
                    // Push to stacks if this is a ViewGroup
                    if (view is ViewGroup) {
                        parentStack.push(view)
                    }
                    viewStack.push(view)
                }
                XmlPullParser.END_TAG -> {
                    val tagName = parser.name
                    Log.d("AppiumXml", "Ending tag: $tagName")
                    
                    // Pop from stacks
                    if (viewStack.isNotEmpty()) {
                        val poppedView = viewStack.pop()
                        if (poppedView is ViewGroup && parentStack.isNotEmpty()) {
                            parentStack.pop()
                        }
                    }
                }
                else -> {
                    // Do nothing
                }
            }
            
            eventType = parser.next()
        }
        
        return rootView ?: View(context)
    }
    
    private fun convertAppiumXmlToAndroidLayout(inputStream: InputStream): String {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        parser.nextTag()
        
        val serializer = Xml.newSerializer()
        val writer = StringWriter()
        serializer.setOutput(writer)
        
        // Start document with proper XML declaration
        serializer.startDocument("UTF-8", null)
        serializer.startTag(null, "FrameLayout")
        serializer.attribute(null, "xmlns:android", "http://schemas.android.com/apk/res/android")
        serializer.attribute(null, "android:layout_width", "match_parent")
        serializer.attribute(null, "android:layout_height", "match_parent")
        
        // Parse the hierarchy and convert to Android layout
        parseAndConvertElement(parser, serializer, 0)
        
        // End the root FrameLayout
        serializer.endTag(null, "FrameLayout")
        serializer.endDocument()
        
        return writer.toString()
    }
    
    private fun parseAndConvertElement(parser: XmlPullParser, serializer: XmlSerializer, depth: Int) {
        val elementStack = mutableListOf<UiElement>()
        
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    val className = parser.getAttributeValue(null, "class")
                    val bounds = parser.getAttributeValue(null, "bounds")
                    val text = parser.getAttributeValue(null, "text")
                    val resourceId = parser.getAttributeValue(null, "resource-id")
                    
                    Log.d("AppiumXml", "Processing element: $tagName, class: $className, bounds: $bounds")
                    
                    // Skip the hierarchy element as it's just a container
                    if (tagName == "hierarchy") {
                        eventType = parser.next()
                        continue
                    }
                    
                    // Convert bounds to Android layout parameters
                    val (left, top, right, bottom) = parseBounds(bounds)
                    val width = right - left
                    val height = bottom - top
                    
                    Log.d("AppiumXml", "Parsed bounds: [$left,$top][$right,$bottom] -> width: $width, height: $height")
                    
                    // Create UI element to track hierarchy
                    val boundsRect = android.graphics.Rect(left, top, right, bottom)
                    val element = UiElement(className ?: "View", text, boundsRect)
                    
                    // Add to parent if there is one
                    if (elementStack.isNotEmpty()) {
                        elementStack.last().children.add(element)
                    }
                    
                    // Convert to Android view class name
                    val androidViewName = convertToAndroidViewClass(className)
                    
                    if (androidViewName != null) {
                        // Check if this is a ViewGroup type
                        val isViewGroup = androidViewName in listOf(
                            "LinearLayout", "RelativeLayout", "FrameLayout", 
                            "androidx.drawerlayout.widget.DrawerLayout"
                        )
                        
                        // Determine if element has meaningful content worth displaying
                        val clickable = parser.getAttributeValue(null, "clickable")
                        val checkable = parser.getAttributeValue(null, "checkable")
                        val enabled = parser.getAttributeValue(null, "enabled")
                        val focusable = parser.getAttributeValue(null, "focusable")
                        val scrollable = parser.getAttributeValue(null, "scrollable")
                        
                        // Less aggressive filtering to include more elements from original XML
                        val shouldCreateView = isViewGroup || 
                                              (!text.isNullOrEmpty() && androidViewName in listOf("TextView", "Button", "EditText")) ||
                                              androidViewName in listOf("ImageButton", "ImageView", "Image") ||
                                              (!resourceId.isNullOrEmpty()) ||
                                              clickable == "true" ||
                                              checkable == "true" ||
                                              enabled == "true" ||
                                              focusable == "true" ||
                                              scrollable == "true" ||
                                              // Include View elements that have meaningful bounds
                                              (androidViewName == "View" && (width > 0 && height > 0)) ||
                                              // Include elements with specific package names that might be important
                                              (className.contains("android.view") || className.contains("android.widget"))
                        
                        if (shouldCreateView) {
                        
                        // Only start a tag if it's a ViewGroup or has no children
                            if (isViewGroup || element.children.isEmpty()) {
                            serializer.startTag(null, androidViewName)
                            
                            // Set layout parameters - convert px to dp for better compatibility
                            val density = resources.displayMetrics.density
                            val widthDp = (width / density).toInt()
                            val heightDp = (height / density).toInt()
                            val leftDp = (left / density).toInt()
                            val topDp = (top / density).toInt()
                            
                            // Ensure width and height are at least 1dp to prevent 0dp values
                            val adjustedWidthDp = if (widthDp <= 0) 1 else widthDp
                            val adjustedHeightDp = if (heightDp <= 0) 1 else heightDp
                            
                            Log.d("AppiumXml", "Calculated dimensions for element: ${width}px -> ${adjustedWidthDp}dp, ${height}px -> ${adjustedHeightDp}dp (density: ${density})")
                            
                            serializer.attribute(null, "android:layout_width", "${adjustedWidthDp}dp")
                            serializer.attribute(null, "android:layout_height", "${adjustedHeightDp}dp")
                            
                            // Use RelativeLayout positioning attributes for proper positioning
                            // Get screen dimensions to ensure elements are within bounds
                            val displayMetrics = resources.displayMetrics
                            val screenHeightDp = (displayMetrics.heightPixels / displayMetrics.density).toInt()
                            val screenWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
                            
                            Log.d("AppiumXml", "Screen dimensions: ${screenWidthDp}dp x ${screenHeightDp}dp")
                            
                            // Adjust left margin to ensure it's within screen bounds
                            val adjustedLeftDp = if (leftDp > screenWidthDp) screenWidthDp / 2 else leftDp
                            
                            // Adjust top margin to ensure it's within screen bounds
                            // Reserve some space for the element itself (at least 50dp)
                            val adjustedTopDp = if (topDp > screenHeightDp - 50) {
                                Log.w("AppiumXml", "Element at top=$topDp dp is outside screen bounds (height=$screenHeightDp dp), adjusting")
                                screenHeightDp / 2 // Center vertically if outside bounds
                            } else {
                                topDp
                            }
                            
                            // For RelativeLayout, we need to use margins for positioning
                            // Don't use alignParent attributes unless the element should be at the edge
                            if (adjustedLeftDp > 0) {
                                serializer.attribute(null, "android:layout_marginStart", "${adjustedLeftDp}dp")
                            }
                            
                            if (adjustedTopDp > 0) {
                                serializer.attribute(null, "android:layout_marginTop", "${adjustedTopDp}dp")
                            }
                            
                            // Log adjustment for the "首页" TextView specifically
                            if (text == "首页") {
                                Log.i("AppiumXml", "TextView '首页' positioned at left=${adjustedLeftDp}dp, top=${adjustedTopDp}dp (original: left=${leftDp}dp, top=${topDp}dp)")
                            }
                            
                            // Set ID if available
                            if (!resourceId.isNullOrEmpty()) {
                                val androidId = if (resourceId.contains("/")) {
                                    resourceId.split("/")[1]
                                } else {
                                    resourceId
                                }
                                // Fix invalid resource IDs that start with numbers
                                val validId = if (androidId.matches(Regex("^[0-9].*"))) {
                                    "id_$androidId"
                                } else {
                                    androidId
                                }
                                serializer.attribute(null, "android:id", "@+id/$validId")
                            }
                            
                            // Set text if available
                            if (!text.isNullOrEmpty() && androidViewName in listOf("TextView", "Button", "EditText")) {
                                serializer.attribute(null, "android:text", text)
                                serializer.attribute(null, "android:textColor", "#000000")
                                serializer.attribute(null, "android:textSize", "14sp")
                            }
                            
                            // Add contentDescription for ImageButton
                            if (androidViewName == "ImageButton") {
                                serializer.attribute(null, "android:contentDescription", text ?: "Image button")
                            }
                            
                            // Set background for better visibility
                            serializer.attribute(null, "android:background", "#FFFFFF")
                            
                            // Set padding
                            serializer.attribute(null, "android:padding", "4dp")
                            
                            // Set visibility
                            val displayed = parser.getAttributeValue(null, "displayed")
                            if (displayed == "false") {
                                serializer.attribute(null, "android:visibility", "gone")
                            }
                            
                            // Set clickable
                            val clickable = parser.getAttributeValue(null, "clickable")
                            if (clickable == "true") {
                                serializer.attribute(null, "android:clickable", "true")
                                serializer.attribute(null, "android:onClick", "onViewClick")
                            }
                            
                            Log.d("AppiumXml", "Created Android view: $androidViewName at ($left, $top) with size ${width}x${height}")
                            
                            // Only add to stack if it's a ViewGroup
                            if (isViewGroup) {
                                elementStack.add(element)
                            } else {
                                // Close the tag immediately for non-ViewGroup elements
                                serializer.endTag(null, androidViewName)
                            }
                        } else {
                            // For non-ViewGroup elements with children, create a FrameLayout wrapper
                            serializer.startTag(null, "FrameLayout")
                            
                            // Set layout parameters - convert px to dp for better compatibility
                            val density = resources.displayMetrics.density
                            val widthDp = (width / density).toInt()
                            val heightDp = (height / density).toInt()
                            val leftDp = (left / density).toInt()
                            val topDp = (top / density).toInt()
                            
                            // Ensure width and height are at least 1dp to prevent 0dp values
                            val adjustedWidthDp = if (widthDp <= 0) 1 else widthDp
                            val adjustedHeightDp = if (heightDp <= 0) 1 else heightDp
                            
                            Log.d("AppiumXml", "Calculated dimensions for FrameLayout wrapper: ${width}px -> ${adjustedWidthDp}dp, ${height}px -> ${adjustedHeightDp}dp (density: ${density})")
                            
                            // Get screen dimensions to ensure elements are within bounds
                            val displayMetrics = resources.displayMetrics
                            val screenHeightDp = (displayMetrics.heightPixels / displayMetrics.density).toInt()
                            val screenWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
                            
                            // Adjust margins to ensure they're within screen bounds
                            val adjustedLeftDp = if (leftDp > screenWidthDp) screenWidthDp / 2 else leftDp
                            val adjustedTopDp = if (topDp > screenHeightDp - 50) {
                                screenHeightDp / 2 // Center vertically if outside bounds
                            } else {
                                topDp
                            }
                            
                            serializer.attribute(null, "android:layout_width", "${adjustedWidthDp}dp")
                            serializer.attribute(null, "android:layout_height", "${adjustedHeightDp}dp")
                            
                            // For RelativeLayout, use margins for positioning
                            if (adjustedLeftDp > 0) {
                                serializer.attribute(null, "android:layout_marginStart", "${adjustedLeftDp}dp")
                            }
                            
                            if (adjustedTopDp > 0) {
                                serializer.attribute(null, "android:layout_marginTop", "${adjustedTopDp}dp")
                            }
                            serializer.attribute(null, "android:background", "#FFFFFF")
                            
                            // Add the actual view as a child
                            serializer.startTag(null, androidViewName)
                            serializer.attribute(null, "android:layout_width", "match_parent")
                            serializer.attribute(null, "android:layout_height", "match_parent")
                            
                            // Set text if available
                            if (!text.isNullOrEmpty() && androidViewName in listOf("TextView", "Button", "EditText")) {
                                serializer.attribute(null, "android:text", text)
                                serializer.attribute(null, "android:textColor", "#000000")
                                serializer.attribute(null, "android:textSize", "14sp")
                            }
                            
                            // Set ID if available
                            if (!resourceId.isNullOrEmpty()) {
                                val androidId = if (resourceId.contains("/")) {
                                    resourceId.split("/")[1]
                                } else {
                                    resourceId
                                }
                                // Fix invalid resource IDs that start with numbers
                                val validId = if (androidId.matches(Regex("^[0-9].*"))) {
                                    "id_$androidId"
                                } else {
                                    androidId
                                }
                                serializer.attribute(null, "android:id", "@+id/$validId")
                            }
                            
                            // Set clickable
                            val clickable = parser.getAttributeValue(null, "clickable")
                            if (clickable == "true") {
                                serializer.attribute(null, "android:clickable", "true")
                                serializer.attribute(null, "android:onClick", "onViewClick")
                            }
                            
                            serializer.endTag(null, androidViewName)
                            
                            // Add the wrapper to the stack
                            elementStack.add(element)
                            
                            Log.d("AppiumXml", "Created Android view with FrameLayout wrapper: $androidViewName at ($left, $top) with size ${width}x${height}")
                        }
                        } else {
                            // Skip this element as it doesn't have meaningful content
                            Log.d("AppiumXml", "Skipping element without meaningful content: $className")
                        }
                    }
                    
                    eventType = parser.next()
                }
                XmlPullParser.END_TAG -> {
                    val tagName = parser.name
                    
                    // Skip the hierarchy element
                    if (tagName == "hierarchy") {
                        eventType = parser.next()
                        continue
                    }
                    
                    // Close the tag if we have an open element
                    if (elementStack.isNotEmpty()) {
                        val element = elementStack.removeAt(elementStack.size - 1)
                        val androidViewName = convertToAndroidViewClass(element.className)
                        
                        if (androidViewName != null) {
                            val isViewGroup = androidViewName in listOf(
                                "LinearLayout", "RelativeLayout", "FrameLayout", 
                                "androidx.drawerlayout.widget.DrawerLayout"
                            )
                            
                            // Only close if it's a ViewGroup or we wrapped it in a FrameLayout
                            if (isViewGroup || !isViewGroup) {
                                serializer.endTag(null, if (isViewGroup) androidViewName else "FrameLayout")
                            }
                            
                            Log.d("AppiumXml", "Ended Android view: $androidViewName")
                        }
                    }
                    
                    eventType = parser.next()
                }
                else -> {
                    eventType = parser.next()
                }
            }
            
            // If we've reached the end of the current element, break
            if (eventType == XmlPullParser.END_TAG && parser.name == "hierarchy") {
                break
            }
        }
        
        // Close any remaining tags
        while (elementStack.isNotEmpty()) {
            val element = elementStack.removeAt(elementStack.size - 1)
            val androidViewName = convertToAndroidViewClass(element.className)
            
            if (androidViewName != null) {
                val isViewGroup = androidViewName in listOf(
                    "LinearLayout", "RelativeLayout", "FrameLayout", 
                    "androidx.drawerlayout.widget.DrawerLayout"
                )
                
                // Only close if it's a ViewGroup or we wrapped it in a FrameLayout
                if (isViewGroup || !isViewGroup) {
                    serializer.endTag(null, if (isViewGroup) androidViewName else "FrameLayout")
                }
                
                Log.d("AppiumXml", "Closed remaining Android view: $androidViewName")
            }
        }
    }
    
    private fun parseBounds(bounds: String?): IntArray {
        if (bounds.isNullOrEmpty()) {
            return intArrayOf(0, 0, 100, 100)
        }
        
        // Bounds format: [left,top][right,bottom]
        val regex = Regex("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]")
        val matchResult = regex.find(bounds)
        
        if (matchResult != null) {
            val (left, top, right, bottom) = matchResult.destructured
            return intArrayOf(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        }
        
        return intArrayOf(0, 0, 100, 100)
    }
    
    private fun convertToAndroidViewClass(className: String?): String? {
        return when {
            className.isNullOrEmpty() -> "View"
            className == "android.view.View" -> "View"
            className == "android.widget.TextView" -> "TextView"
            className == "android.widget.Button" -> "Button"
            className == "android.widget.EditText" -> "EditText"
            className == "android.widget.ImageView" -> "ImageView"
            className == "android.widget.Image" -> "ImageView"  // Handle android.widget.Image as ImageView
            className == "android.widget.ImageButton" -> "ImageButton"
            className == "android.widget.LinearLayout" -> "LinearLayout"
            className == "android.widget.RelativeLayout" -> "RelativeLayout"
            className == "android.widget.FrameLayout" -> "FrameLayout"
            className == "androidx.drawerlayout.widget.DrawerLayout" -> "androidx.drawerlayout.widget.DrawerLayout"
            className == "android.webkit.WebView" -> "WebView"
            className.contains("View") -> "View"
            className.contains("Layout") -> "LinearLayout"
            else -> "View"
        }
    }
    
    // Handle view clicks
    fun onViewClick(view: View) {
        val info = buildString {
            append("控件类型：${view.javaClass.simpleName}\n")
            append("控件ID：${view.id}\n")
            append("位置：左${view.left}px / 上${view.top}px\n")
            append("大小：宽${view.width}px / 高${view.height}px")
        }
        AlertDialog.Builder(requireContext())
            .setTitle("控件详情")
            .setMessage(info)
            .setPositiveButton("确定", null)
            .show()
    }
    
    // Print DOM tree structure
    private fun printDomTree(element: UiElement, depth: Int) {
        val indent = "  ".repeat(depth)
        Log.d("AppiumXml", "${indent}Element: ${element.className}")
        Log.d("AppiumXml", "${indent}Bounds: ${element.bounds}")
        Log.d("AppiumXml", "${indent}Text: ${element.text ?: "none"}")
        Log.d("AppiumXml", "${indent}Children: ${element.children.size}")
        
        element.children.forEach { child ->
            printDomTree(child, depth + 1)
        }
    }
    
    // Print view hierarchy after rendering
    private fun printViewHierarchy(view: View, depth: Int) {
        val indent = "  ".repeat(depth)
        Log.d("AppiumXml", "${indent}View: ${view.javaClass.simpleName}")
        Log.d("AppiumXml", "${indent}Tag: ${view.tag}")
        Log.d("AppiumXml", "${indent}Position: ${view.left}, ${view.top}, ${view.right}, ${view.bottom}")
        Log.d("AppiumXml", "${indent}Size: ${view.width} x ${view.height}")
        Log.d("AppiumXml", "${indent}Visibility: ${view.visibility}")
        
        if (view is ViewGroup) {
            Log.d("AppiumXml", "${indent}Child count: ${view.childCount}")
            for (i in 0 until view.childCount) {
                printViewHierarchy(view.getChildAt(i), depth + 1)
            }
        }
    }

    // 递归渲染（新增：屏幕适配 + 点击事件）
    private fun renderUiElement(element: UiElement, parent: ViewGroup) {
        // Skip the hierarchy element as it's just a container
        if (element.className == "hierarchy") {
            Log.d("AppiumXml", "Found hierarchy element with ${element.children.size} children")
            element.children.forEach { child ->
                renderUiElement(child, parent)
            }
            return
        }
        
        Log.d("AppiumXml", "Rendering element: ${element.className}, bounds: ${element.bounds}, text: ${element.text ?: "no text"}")
        
        val androidView = createViewByClassName(element.className)
        if (androidView == null) {
            Log.w("AppiumXml", "Failed to create view for class: ${element.className}")
            return
        }
        
        Log.d("AppiumXml", "Successfully created view: ${androidView.javaClass.simpleName}")
        
        // 1. 屏幕适配：将 Appium 绝对坐标（px）转换为相对坐标（适配不同分辨率）
        val params = calculateRelativeParams(element.bounds)
        
        // Create appropriate LayoutParams based on parent type to avoid ClassCastException
        // All LayoutParams subclasses support margins through inheritance
        Log.d("AppiumXml", "=== LAYOUT PARAMS DEBUG ===")
        Log.d("AppiumXml", "Parent type: ${parent.javaClass.simpleName}")
        Log.d("AppiumXml", "Relative params: left=${params.relativeLeft}, top=${params.relativeTop}, width=${params.relativeWidth}, height=${params.relativeHeight}")
        Log.d("AppiumXml", "Display metrics: width=${displayMetrics.widthPixels}, height=${displayMetrics.heightPixels}")
        
        val calculatedWidth = (displayMetrics.widthPixels * params.relativeWidth).toInt()
        val calculatedHeight = (displayMetrics.heightPixels * params.relativeHeight).toInt()
        val calculatedLeftMargin = (displayMetrics.widthPixels * params.relativeLeft).toInt()
        val calculatedTopMargin = (displayMetrics.heightPixels * params.relativeTop).toInt()
        
        Log.d("AppiumXml", "Calculated dimensions: width=$calculatedWidth, height=$calculatedHeight")
        Log.d("AppiumXml", "Calculated margins: left=$calculatedLeftMargin, top=$calculatedTopMargin")
        
        val layoutParams = when (parent) {
            is FrameLayout -> {
                Log.d("AppiumXml", "Creating FrameLayout.LayoutParams")
                FrameLayout.LayoutParams(calculatedWidth, calculatedHeight).apply {
                    leftMargin = calculatedLeftMargin
                    topMargin = calculatedTopMargin
                }
            }
            is RelativeLayout -> {
                Log.d("AppiumXml", "Creating RelativeLayout.LayoutParams")
                RelativeLayout.LayoutParams(calculatedWidth, calculatedHeight).apply {
                    leftMargin = calculatedLeftMargin
                    topMargin = calculatedTopMargin
                }
            }
            is LinearLayout -> {
                Log.d("AppiumXml", "Creating LinearLayout.LayoutParams")
                LinearLayout.LayoutParams(calculatedWidth, calculatedHeight).apply {
                    leftMargin = calculatedLeftMargin
                    topMargin = calculatedTopMargin
                }
            }
            else -> {
                Log.d("AppiumXml", "Creating ViewGroup.MarginLayoutParams")
                ViewGroup.MarginLayoutParams(calculatedWidth, calculatedHeight).apply {
                    leftMargin = calculatedLeftMargin
                    topMargin = calculatedTopMargin
                }
            }
        }
        
        Log.d("AppiumXml", "Final LayoutParams: width=${layoutParams.width}, height=${layoutParams.height}, leftMargin=${(layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin}, topMargin=${(layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin}")
        Log.d("AppiumXml", "=== END LAYOUT PARAMS DEBUG ===")

        // 2. 控件基础属性设置
        androidView.apply {
            this.layoutParams = layoutParams
            tag = "Appium_${element.className}_${element.text ?: "no_text"}"
            
            // 文本控件设置
            if (this is TextView) {
                text = element.text ?: ""
                textSize = 14f
                setTextColor(Color.BLACK)
            }

            // 3. 点击事件模拟（弹窗显示控件信息）
            setOnClickListener {
                val info = buildString {
                    append("控件类型：${element.className}\n")
                    append("文本：${element.text ?: "无"}\n")
                    append("原始坐标：${element.bounds}\n")
                    append("适配后位置：左${layoutParams.leftMargin}px / 上${layoutParams.topMargin}px\n")
                    append("适配后大小：宽${width}px / 高${height}px")
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("控件详情")
                    .setMessage(info)
                    .setPositiveButton("确定", null)
                    .show()
            }

            // 4. 控件高亮（添加边框，便于区分）
            val borderDrawable = ShapeDrawable().apply {
                shape = RectShape()
                paint.color = Color.parseColor("#FF4081") // 粉色边框
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
            }
            background = borderDrawable
        }

        parent.addView(androidView)
        Log.d("AppiumXml", "Added view to parent. Parent now has ${parent.childCount} children")
        
        // Request layout and measure to ensure proper dimensions
        androidView.requestLayout()
        
        // Different measurement approach for different view types
        if (androidView is DrawerLayout) {
            // DrawerLayout requires EXACTLY measurement
            androidView.measure(
                View.MeasureSpec.makeMeasureSpec(layoutParams.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(layoutParams.height, View.MeasureSpec.EXACTLY)
            )
        } else {
            // Use UNSPECIFIED for other view types
            androidView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        
        androidView.layout(
            (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0,
            (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0,
            (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0 + layoutParams.width,
            (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0 + layoutParams.height
        )
        
        Log.i("AppiumXml", "=== VIEW POSITION DEBUG ===")
        Log.i("AppiumXml", "View position: left=${layoutParams.leftMargin}, top=${layoutParams.topMargin}, width=${layoutParams.width}, height=${layoutParams.height}")
        Log.i("AppiumXml", "Screen dimensions: width=${displayMetrics.widthPixels}, height=${displayMetrics.heightPixels}")
        Log.i("AppiumXml", "View bounds: [${layoutParams.leftMargin}, ${layoutParams.topMargin}, ${layoutParams.leftMargin + layoutParams.width}, ${layoutParams.topMargin + layoutParams.height}]")
        Log.i("AppiumXml", "View measured dimensions: width=${androidView.measuredWidth}, height=${androidView.measuredHeight}")
        Log.i("AppiumXml", "View actual dimensions: width=${androidView.width}, height=${androidView.height}")
        Log.i("AppiumXml", "View layout: left=${androidView.left}, top=${androidView.top}, right=${androidView.right}, bottom=${androidView.bottom}")
        
        // Check if view is within screen bounds
        val isInScreenBounds = layoutParams.leftMargin < displayMetrics.widthPixels && 
                              layoutParams.topMargin < displayMetrics.heightPixels &&
                              (layoutParams.leftMargin + layoutParams.width) > 0 &&
                              (layoutParams.topMargin + layoutParams.height) > 0
        Log.i("AppiumXml", "Is view within screen bounds: $isInScreenBounds")
        Log.i("AppiumXml", "=== END VIEW POSITION DEBUG ===")

        // 递归渲染子控件
        if (androidView is ViewGroup) {
            Log.d("AppiumXml", "View is a ViewGroup with ${element.children.size} children to render")
            element.children.forEach { child ->
                renderUiElement(child, androidView)
            }
        }
    }

    // 核心：计算相对坐标（绝对坐标 → 相对屏幕比例）
    private fun calculateRelativeParams(bounds: Rect): RelativeParams {
        Log.d("AppiumXml", "=== BOUNDS CONVERSION DEBUG ===")
        Log.d("AppiumXml", "Original bounds: left=${bounds.left}, top=${bounds.top}, right=${bounds.right}, bottom=${bounds.bottom}")
        Log.d("AppiumXml", "Bounds width: ${bounds.width()}, height: ${bounds.height()}")
        
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()
        Log.d("AppiumXml", "Display metrics: width=${screenWidth}, height=${screenHeight}")
        
        // 相对左边距 = 绝对左边距 / 屏幕宽度
        val relativeLeft = bounds.left / screenWidth
        // 相对上边距 = 绝对上边距 / 屏幕高度
        val relativeTop = bounds.top / screenHeight
        // 相对宽度 = 绝对宽度 / 屏幕宽度
        val relativeWidth = bounds.width() / screenWidth
        // 相对高度 = 绝对高度 / 屏幕高度
        val relativeHeight = bounds.height() / screenHeight
        
        Log.d("AppiumXml", "Calculated relative params: left=$relativeLeft, top=$relativeTop, width=$relativeWidth, height=$relativeHeight")
        Log.d("AppiumXml", "=== END BOUNDS CONVERSION DEBUG ===")
        
        return RelativeParams(relativeLeft, relativeTop, relativeWidth, relativeHeight)
    }

    // 创建对应原生控件
    private fun createViewByClassName(className: String): View? {
        return try {
            when (className) {
                "android.widget.LinearLayout" -> LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                }
                "android.widget.FrameLayout" -> FrameLayout(requireContext())
                "android.widget.RelativeLayout" -> RelativeLayout(requireContext())
                "android.widget.TextView" -> TextView(requireContext())
                "android.widget.EditText" -> EditText(requireContext())
                "android.widget.Button" -> Button(requireContext())
                "android.widget.ImageView" -> ImageView(requireContext()).apply {
                    setBackgroundColor(Color.LTGRAY)
                }
                "android.widget.Image" -> ImageView(requireContext()).apply {
                    setBackgroundColor(Color.LTGRAY)
                }
                "android.widget.ImageButton" -> ImageButton(requireContext()).apply {
                    setBackgroundColor(Color.LTGRAY)
                }
                "android.view.View" -> View(requireContext())
                "android.webkit.WebView" -> {
                    // On Android 8.0, WebView has compatibility issues with missing TracingController
                    // Use a simple View as a fallback to avoid crashes
                    try {
                        // Check Android version first
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            WebView(requireContext()).apply {
                                setBackgroundColor(Color.LTGRAY)
                            }
                        } else {
                            // For Android 8.0 and below, use a simple View instead
                            Log.w("AppiumXml", "Using View fallback for WebView on Android ${Build.VERSION.SDK_INT}")
                            View(requireContext()).apply {
                                setBackgroundColor(Color.LTGRAY)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AppiumXml", "Failed to create WebView: ${e.message}")
                        // Fallback to a simple View if WebView creation fails
                        View(requireContext()).apply {
                            setBackgroundColor(Color.LTGRAY)
                        }
                    }
                }
                "androidx.drawerlayout.widget.DrawerLayout" -> DrawerLayout(requireContext())
                else -> {
                    Log.w("AppiumXml", "未支持的控件类型：$className")
                    TextView(requireContext()).apply {
                        text = "未支持控件：$className"
                        setTextColor(Color.RED)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppiumXml", "Error creating view for $className: ${e.message}")
            // Return a fallback view
            TextView(requireContext()).apply {
                text = "错误: $className"
                setTextColor(Color.RED)
            }
        }
    }

    // 实体类和解析工具类
    data class UiElement(
        val className: String,
        val text: String?,
        val bounds: Rect,
        val children: MutableList<UiElement> = mutableListOf()
    )

    data class RelativeParams(
        val relativeLeft: Float,
        val relativeTop: Float,
        val relativeWidth: Float,
        val relativeHeight: Float
    )

    object AppiumXmlParser {
        fun parse(xmlInputStream: InputStream): UiElement? {
            val parser = Xml.newPullParser()
            parser.setInput(xmlInputStream, "UTF-8")
            var eventType = parser.eventType
            var rootElement: UiElement? = null
            val elementStack = Stack<UiElement>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        // Handle both hierarchy and all element tags (not just "node")
                        if (parser.name == "hierarchy" || parser.name.contains(".")) {
                            val className = parser.getAttributeValue(null, "class") ?: parser.name
                            val text = parser.getAttributeValue(null, "text")
                            val boundsStr = parser.getAttributeValue(null, "bounds") ?: "[0,0][0,0]"
                            val bounds = parseBounds(boundsStr)
                            val currentElement = UiElement(className, text, bounds)

                            if (parser.name == "hierarchy") {
                                rootElement = currentElement
                            } else if (elementStack.isNotEmpty()) {
                                elementStack.peek().children.add(currentElement)
                            }
                            elementStack.push(currentElement)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "hierarchy" || parser.name.contains(".")) {
                            if (elementStack.isNotEmpty()) {
                                elementStack.pop()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            return rootElement
        }

        private fun parseBounds(boundsStr: String): Rect {
            val regex = Regex("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]")
            val matchResult = regex.find(boundsStr) ?: return Rect()
            val (left, top, right, bottom) = matchResult.destructured
            return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        }
    }
}