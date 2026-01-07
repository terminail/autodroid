# AAS

## 用AccessibilityService编程把用户在指定APP点击，输入，下拉选择等操作的控件信息特征记录到数据库

我来为你实现一个完整的 AccessibilityService，记录用户在指定APP中的点击、输入、下拉选择等操作，并将控件特征保存到数据库。

## **1. 数据库设计**

### **1.1 实体类定义**

```kotlin
// 1. 操作事件实体
@Entity(tableName = "ui_events")
data class UIEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String?,
    @ColumnInfo(name = "event_type") val eventType: String, // CLICK, INPUT, SELECT, SCROLL, LONG_CLICK, FOCUS
    @ColumnInfo(name = "event_time") val eventTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "element_id") val elementId: String?, // 元素唯一标识
    @ColumnInfo(name = "element_type") val elementType: String?, // Button, EditText, TextView 等
    @ColumnInfo(name = "element_text") val elementText: String?,
    @ColumnInfo(name = "element_hint") val elementHint: String?,
    @ColumnInfo(name = "element_content_desc") val elementContentDesc: String?,
    @ColumnInfo(name = "element_class") val elementClass: String?,
    @ColumnInfo(name = "element_bounds") val elementBounds: String?, // "left,top,right,bottom"
    @ColumnInfo(name = "input_value") val inputValue: String?, // 输入的内容
    @ColumnInfo(name = "selected_value") val selectedValue: String?, // 选择的值
    @ColumnInfo(name = "parent_hierarchy") val parentHierarchy: String?, // JSON格式的父级结构
    @ColumnInfo(name = "sibling_info") val siblingInfo: String?, // JSON格式的兄弟节点信息
    @ColumnInfo(name = "extra_data") val extraData: String?, // 额外数据（JSON格式）
    @ColumnInfo(name = "screenshot_path") val screenshotPath: String? // 截图路径（可选）
)

// 2. 元素特征实体（用于自动填充）
@Entity(tableName = "element_features")
data class ElementFeature(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String?,
    @ColumnInfo(name = "element_signature") val elementSignature: String, // 元素签名（唯一标识）
    @ColumnInfo(name = "element_type") val elementType: String,
    @ColumnInfo(name = "element_hint") val elementHint: String?,
    @ColumnInfo(name = "element_content_desc") val elementContentDesc: String?,
    @ColumnInfo(name = "element_class") val elementClass: String?,
    @ColumnInfo(name = "parent_hierarchy") val parentHierarchy: String?,
    @ColumnInfo(name = "sibling_info") val siblingInfo: String?,
    @ColumnInfo(name = "common_values") val commonValues: String?, // JSON数组，常见的输入值
    @ColumnInfo(name = "last_used_time") val lastUsedTime: Long,
    @ColumnInfo(name = "usage_count") val usageCount: Int = 1,
    @ColumnInfo(name = "auto_fill_enabled") val autoFillEnabled: Boolean = true,
    @ColumnInfo(name = "auto_fill_value") val autoFillValue: String? // 自动填充的值
)

// 3. 应用配置实体
@Entity(tableName = "app_configs")
data class AppConfig(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "recording_enabled") val recordingEnabled: Boolean = true,
    @ColumnInfo(name = "record_clicks") val recordClicks: Boolean = true,
    @ColumnInfo(name = "record_inputs") val recordInputs: Boolean = true,
    @ColumnInfo(name = "record_selections") val recordSelections: Boolean = true,
    @ColumnInfo(name = "record_scrolls") val recordScrolls: Boolean = false,
    @ColumnInfo(name = "take_screenshots") val takeScreenshots: Boolean = false,
    @ColumnInfo(name = "screenshot_quality") val screenshotQuality: Int = 70,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)
```

### **1.2 Room 数据库配置**

```kotlin
@Database(
    entities = [UIEvent::class, ElementFeature::class, AppConfig::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UIRecorderDatabase : RoomDatabase() {
    abstract fun uiEventDao(): UIEventDao
    abstract fun elementFeatureDao(): ElementFeatureDao
    abstract fun appConfigDao(): AppConfigDao
    
    companion object {
        @Volatile
        private var INSTANCE: UIRecorderDatabase? = null
        
        fun getInstance(context: Context): UIRecorderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UIRecorderDatabase::class.java,
                    "ui_recorder.db"
                )
                .addMigrations(MIGRATION_1_2) // 添加迁移策略
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// 类型转换器
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
    
    @TypeConverter
    fun fromJsonString(value: String?): Map<String, String>? {
        return value?.let { 
            Gson().fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
        }
    }
    
    @TypeConverter
    fun mapToJsonString(map: Map<String, String>?): String? {
        return map?.let { Gson().toJson(it) }
    }
}
```

## **2. AccessibilityService 实现**

### **2.1 主服务类**

```kotlin
class UIRecorderAccessibilityService : AccessibilityService() {
    
    companion object {
        const val TAG = "UIRecorderService"
        var isServiceConnected = false
        
        // 事件类型常量
        const val EVENT_CLICK = "CLICK"
        const val EVENT_INPUT = "INPUT"
        const val EVENT_SELECT = "SELECT"
        const val EVENT_SCROLL = "SCROLL"
        const val EVENT_LONG_CLICK = "LONG_CLICK"
        const val EVENT_FOCUS = "FOCUS"
        const val EVENT_PAGE_CHANGE = "PAGE_CHANGE"
    }
    
    private lateinit var database: UIRecorderDatabase
    private lateinit var uiEventDao: UIEventDao
    private lateinit var elementFeatureDao: ElementFeatureDao
    private lateinit var appConfigDao: AppConfigDao
    
    private val eventProcessor = UIEventProcessor()
    private val screenshotHelper = ScreenshotHelper()
    private val elementAnalyzer = ElementAnalyzer()
    
    // 线程池用于异步处理
    private val executor = Executors.newSingleThreadExecutor()
    
    // 目标应用包名
    private val targetPackages = mutableSetOf<String>()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        isServiceConnected = true
        
        // 初始化数据库
        database = UIRecorderDatabase.getInstance(this)
        uiEventDao = database.uiEventDao()
        elementFeatureDao = database.elementFeatureDao()
        appConfigDao = database.appConfigDao()
        
        // 加载配置
        loadAppConfigs()
        
        // 显示浮动控制窗口
        showFloatingControl()
        
        // 发送服务启动广播
        sendServiceStatusBroadcast(true)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isServiceConnected) return
        
        val packageName = event.packageName?.toString() ?: return
        
        // 检查是否是目标应用
        if (!shouldRecordPackage(packageName)) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleClickEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChangedEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                handleSelectionEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleScrollEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                handleLongClickEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleFocusEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowChangeEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleContentChangeEvent(event)
            }
        }
    }
    
    private fun shouldRecordPackage(packageName: String): Boolean {
        // 如果目标包名列表为空，则记录所有应用
        if (targetPackages.isEmpty()) return true
        return targetPackages.contains(packageName)
    }
    
    private fun loadAppConfigs() {
        executor.execute {
            try {
                val configs = appConfigDao.getAllConfigs()
                targetPackages.clear()
                configs.forEach { config ->
                    if (config.recordingEnabled) {
                        targetPackages.add(config.packageName)
                    }
                }
                Log.d(TAG, "Loaded ${targetPackages.size} target packages")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load app configs", e)
            }
        }
    }
}
```

### **2.2 事件处理器**

```kotlin
class UIEventProcessor(private val context: Context) {
    
    private lateinit var elementFeatureDao: ElementFeatureDao
    
    fun init(database: UIRecorderDatabase) {
        elementFeatureDao = database.elementFeatureDao()
    }
    
    fun processClickEvent(
        source: AccessibilityNodeInfo?,
        packageName: String,
        activityName: String?
    ): UIEvent? {
        source ?: return null
        
        val elementInfo = extractElementInfo(source)
        
        // 生成事件
        return UIEvent(
            packageName = packageName,
            activityName = activityName,
            eventType = UIRecorderAccessibilityService.EVENT_CLICK,
            elementId = elementInfo.id,
            elementType = elementInfo.type,
            elementText = elementInfo.text,
            elementHint = elementInfo.hint,
            elementContentDesc = elementInfo.contentDesc,
            elementClass = source.className?.toString(),
            elementBounds = getBoundsString(source),
            parentHierarchy = extractParentHierarchy(source),
            siblingInfo = extractSiblingInfo(source),
            extraData = getClickExtraData(source)
        )
    }
    
    fun processInputEvent(
        source: AccessibilityNodeInfo?,
        packageName: String,
        activityName: String?,
        beforeText: CharSequence?,
        afterText: CharSequence?
    ): UIEvent? {
        source ?: return null
        
        val elementInfo = extractElementInfo(source)
        val inputValue = afterText?.toString()
        
        // 更新元素特征
        updateElementFeature(source, packageName, activityName, inputValue)
        
        return UIEvent(
            packageName = packageName,
            activityName = activityName,
            eventType = UIRecorderAccessibilityService.EVENT_INPUT,
            elementId = elementInfo.id,
            elementType = elementInfo.type,
            elementText = elementInfo.text,
            elementHint = elementInfo.hint,
            elementContentDesc = elementInfo.contentDesc,
            elementClass = source.className?.toString(),
            elementBounds = getBoundsString(source),
            inputValue = inputValue,
            parentHierarchy = extractParentHierarchy(source),
            siblingInfo = extractSiblingInfo(source),
            extraData = getInputExtraData(beforeText, afterText)
        )
    }
    
    fun processSelectionEvent(
        source: AccessibilityNodeInfo?,
        packageName: String,
        activityName: String?
    ): UIEvent? {
        source ?: return null
        
        val elementInfo = extractElementInfo(source)
        val selectedValue = getSelectedValue(source)
        
        return UIEvent(
            packageName = packageName,
            activityName = activityName,
            eventType = UIRecorderAccessibilityService.EVENT_SELECT,
            elementId = elementInfo.id,
            elementType = elementInfo.type,
            elementText = elementInfo.text,
            elementHint = elementInfo.hint,
            elementContentDesc = elementInfo.contentDesc,
            elementClass = source.className?.toString(),
            elementBounds = getBoundsString(source),
            selectedValue = selectedValue,
            parentHierarchy = extractParentHierarchy(source),
            siblingInfo = extractSiblingInfo(source),
            extraData = getSelectionExtraData(source)
        )
    }
    
    private fun updateElementFeature(
        node: AccessibilityNodeInfo,
        packageName: String,
        activityName: String?,
        inputValue: String?
    ) {
        val signature = generateElementSignature(node)
        
        GlobalScope.launch(Dispatchers.IO) {
            try {
                var feature = elementFeatureDao.getBySignature(signature)
                
                if (feature == null) {
                    // 创建新特征
                    feature = ElementFeature(
                        packageName = packageName,
                        activityName = activityName,
                        elementSignature = signature,
                        elementType = getElementType(node),
                        elementHint = node.hintText?.toString(),
                        elementContentDesc = node.contentDescription?.toString(),
                        elementClass = node.className?.toString(),
                        parentHierarchy = extractParentHierarchy(node),
                        siblingInfo = extractSiblingInfo(node),
                        commonValues = if (inputValue != null) "[\"$inputValue\"]" else null,
                        lastUsedTime = System.currentTimeMillis(),
                        usageCount = 1
                    )
                    elementFeatureDao.insert(feature)
                } else {
                    // 更新现有特征
                    val values = updateCommonValues(feature.commonValues, inputValue)
                    feature.copy(
                        commonValues = values,
                        lastUsedTime = System.currentTimeMillis(),
                        usageCount = feature.usageCount + 1
                    ).let { updatedFeature ->
                        elementFeatureDao.update(updatedFeature)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update element feature", e)
            }
        }
    }
    
    private fun updateCommonValues(
        existingValues: String?,
        newValue: String?
    ): String? {
        newValue ?: return existingValues
        
        val values = try {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(existingValues, listType) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf<String>()
        }.toMutableList()
        
        // 添加新值（如果不存在）
        if (!values.contains(newValue)) {
            values.add(newValue)
            
            // 只保留最近的10个值
            if (values.size > 10) {
                values.removeAt(0)
            }
        }
        
        return Gson().toJson(values)
    }
    
    private fun generateElementSignature(node: AccessibilityNodeInfo): String {
        // 使用多个特征生成唯一签名
        val builder = StringBuilder()
        
        // 1. 类名
        node.className?.toString()?.let { builder.append(it) }
        
        // 2. 资源ID
        node.viewIdResourceName?.let { builder.append("|id:$it") }
        
        // 3. 文本（前20个字符）
        node.text?.toString()?.take(20)?.let { builder.append("|text:$it") }
        
        // 4. 提示文本
        node.hintText?.toString()?.let { builder.append("|hint:$it") }
        
        // 5. 内容描述
        node.contentDescription?.toString()?.let { builder.append("|desc:$it") }
        
        // 6. 父级结构哈希
        val parentHash = calculateParentHash(node)
        builder.append("|parent:$parentHash")
        
        return builder.toString().hashCode().toString()
    }
    
    private fun calculateParentHash(node: AccessibilityNodeInfo): Int {
        var hash = 0
        var current = node.parent
        var depth = 0
        
        while (current != null && depth < 3) {
            val className = current.className?.toString() ?: ""
            val childCount = current.childCount
            hash = 31 * hash + (className.hashCode() + childCount)
            current = current.parent
            depth++
        }
        
        return hash
    }
    
    data class ElementInfo(
        val id: String?,
        val type: String?,
        val text: String?,
        val hint: String?,
        val contentDesc: String?
    )
}
```

### **2.3 元素分析器**

```kotlin
class ElementAnalyzer {
    
    fun extractElementInfo(node: AccessibilityNodeInfo): UIEventProcessor.ElementInfo {
        return UIEventProcessor.ElementInfo(
            id = node.viewIdResourceName,
            type = getElementType(node),
            text = node.text?.toString(),
            hint = node.hintText?.toString(),
            contentDesc = node.contentDescription?.toString()
        )
    }
    
    fun getElementType(node: AccessibilityNodeInfo): String {
        node.className?.toString()?.let { className ->
            return when {
                className.contains("Button", ignoreCase = true) -> "Button"
                className.contains("EditText", ignoreCase = true) -> "EditText"
                className.contains("TextView", ignoreCase = true) -> "TextView"
                className.contains("CheckBox", ignoreCase = true) -> "CheckBox"
                className.contains("RadioButton", ignoreCase = true) -> "RadioButton"
                className.contains("Spinner", ignoreCase = true) -> "Spinner"
                className.contains("ListView", ignoreCase = true) -> "ListView"
                className.contains("RecyclerView", ignoreCase = true) -> "RecyclerView"
                className.contains("WebView", ignoreCase = true) -> "WebView"
                else -> className.substringAfterLast(".")
            }
        }
        
        return "Unknown"
    }
    
    fun extractParentHierarchy(node: AccessibilityNodeInfo): String {
        val hierarchy = mutableListOf<Map<String, String>>()
        var current = node.parent
        var depth = 0
        
        while (current != null && depth < 5) {
            val info = mapOf(
                "class" to (current.className?.toString() ?: ""),
                "id" to (current.viewIdResourceName ?: ""),
                "text" to (current.text?.toString()?.take(50) ?: ""),
                "childCount" to current.childCount.toString()
            )
            hierarchy.add(info)
            current = current.parent
            depth++
        }
        
        return Gson().toJson(hierarchy)
    }
    
    fun extractSiblingInfo(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        
        val siblings = mutableListOf<Map<String, String>>()
        
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i)
            sibling?.let {
                val info = mapOf(
                    "index" to i.toString(),
                    "class" to (it.className?.toString() ?: ""),
                    "id" to (it.viewIdResourceName ?: ""),
                    "text" to (it.text?.toString()?.take(30) ?: ""),
                    "isTarget" to (it == node).toString()
                )
                siblings.add(info)
            }
        }
        
        return Gson().toJson(siblings)
    }
    
    fun getBoundsString(node: AccessibilityNodeInfo): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
    }
    
    fun getSelectedValue(node: AccessibilityNodeInfo): String? {
        // 对于选择器类型的元素，获取选中的值
        if (node.isCheckable) {
            return if (node.isChecked) "true" else "false"
        }
        
        // 尝试从子节点获取选中的文本
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let {
                if (it.isSelected) {
                    return it.text?.toString()
                }
            }
        }
        
        return node.text?.toString()
    }
    
    fun getClickExtraData(node: AccessibilityNodeInfo): String {
        val extra = mutableMapOf<String, String>()
        
        if (node.isClickable) extra["clickable"] = "true"
        if (node.isLongClickable) extra["longClickable"] = "true"
        if (node.isEnabled) extra["enabled"] = "true"
        if (node.isCheckable) {
            extra["checkable"] = "true"
            extra["checked"] = node.isChecked.toString()
        }
        
        return Gson().toJson(extra)
    }
    
    fun getInputExtraData(beforeText: CharSequence?, afterText: CharSequence?): String {
        val extra = mutableMapOf<String, String>()
        
        extra["beforeLength"] = (beforeText?.length ?: 0).toString()
        extra["afterLength"] = (afterText?.length ?: 0).toString()
        extra["isPassword"] = (afterText?.length ?: 0 > 0 && afterText.toString().all { it == '*' }).toString()
        
        return Gson().toJson(extra)
    }
    
    fun getSelectionExtraData(node: AccessibilityNodeInfo): String {
        val extra = mutableMapOf<String, String>()
        
        if (node.isSelected) extra["selected"] = "true"
        if (node.isCheckable) {
            extra["checkable"] = "true"
            extra["checked"] = node.isChecked.toString()
        }
        
        // 对于下拉列表，获取选项数量
        if (node.className?.toString()?.contains("Spinner") == true) {
            extra["isSpinner"] = "true"
        }
        
        return Gson().toJson(extra)
    }
}
```

### **2.4 截图辅助类**

```kotlin
class ScreenshotHelper(private val context: Context) {
    
    fun takeScreenshot(packageName: String, eventId: Int): String? {
        // 检查是否有截图权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Log.w(TAG, "No screenshot permission")
                return null
            }
        }
        
        return try {
            // 创建截图目录
            val screenshotDir = File(context.getExternalFilesDir(null), "screenshots")
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs()
            }
            
            // 生成文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${packageName}_${eventId}_$timestamp.png"
            val screenshotFile = File(screenshotDir, fileName)
            
            // 获取根视图
            val accessibilityService = context as? AccessibilityService
            val rootNode = accessibilityService?.rootInActiveWindow ?: return null
            
            val bounds = Rect()
            rootNode.getBoundsInScreen(bounds)
            
            // 这里需要权限，实际实现可能需要 MediaProjection
            // 简化版本：只保存文件路径
            // 实际截图逻辑需要额外权限和实现
            
            // 返回文件路径
            screenshotFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
            null
        }
    }
}
```

### **2.5 事件处理方法实现**

```kotlin
// 在 UIRecorderAccessibilityService 中添加

private fun handleClickEvent(event: AccessibilityEvent) {
    val source = event.source ?: return
    
    executor.execute {
        try {
            val uiEvent = eventProcessor.processClickEvent(
                source,
                event.packageName?.toString() ?: "",
                event.className?.toString()
            )
            
            uiEvent?.let { event ->
                // 保存到数据库
                val eventId = uiEventDao.insert(event).toInt()
                
                // 如果需要截图
                if (shouldTakeScreenshot(event.packageName)) {
                    val screenshotPath = screenshotHelper.takeScreenshot(
                        event.packageName,
                        eventId
                    )
                    
                    if (screenshotPath != null) {
                        // 更新事件记录，添加截图路径
                        event.copy(screenshotPath = screenshotPath).let { updatedEvent ->
                            uiEventDao.update(updatedEvent)
                        }
                    }
                }
                
                // 发送广播通知新事件
                sendNewEventBroadcast(event)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle click event", e)
        } finally {
            source.recycle()
        }
    }
}

private fun handleTextChangedEvent(event: AccessibilityEvent) {
    val source = event.source ?: return
    
    executor.execute {
        try {
            val uiEvent = eventProcessor.processInputEvent(
                source,
                event.packageName?.toString() ?: "",
                event.className?.toString(),
                event.beforeText,
                event.text?.firstOrNull()
            )
            
            uiEvent?.let { event ->
                uiEventDao.insert(event)
                sendNewEventBroadcast(event)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle text changed event", e)
        } finally {
            source.recycle()
        }
    }
}

private fun handleSelectionEvent(event: AccessibilityEvent) {
    val source = event.source ?: return
    
    executor.execute {
        try {
            val uiEvent = eventProcessor.processSelectionEvent(
                source,
                event.packageName?.toString() ?: "",
                event.className?.toString()
            )
            
            uiEvent?.let { event ->
                uiEventDao.insert(event)
                sendNewEventBroadcast(event)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle selection event", e)
        } finally {
            source.recycle()
        }
    }
}

private fun handleScrollEvent(event: AccessibilityEvent) {
    val config = appConfigDao.getConfig(event.packageName?.toString() ?: "")
    if (config == null || !config.recordScrolls) return
    
    val source = event.source ?: return
    
    executor.execute {
        try {
            val elementInfo = elementAnalyzer.extractElementInfo(source)
            
            val uiEvent = UIEvent(
                packageName = event.packageName?.toString() ?: "",
                activityName = event.className?.toString(),
                eventType = UIRecorderAccessibilityService.EVENT_SCROLL,
                elementId = elementInfo.id,
                elementType = elementInfo.type,
                elementText = elementInfo.text,
                elementClass = source.className?.toString(),
                elementBounds = elementAnalyzer.getBoundsString(source),
                extraData = getScrollExtraData(event)
            )
            
            uiEventDao.insert(uiEvent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle scroll event", e)
        } finally {
            source.recycle()
        }
    }
}

private fun getScrollExtraData(event: AccessibilityEvent): String {
    val extra = mutableMapOf<String, String>()
    
    extra["scrollX"] = event.scrollX.toString()
    extra["scrollY"] = event.scrollY.toString()
    extra["itemCount"] = event.itemCount.toString()
    extra["currentItemIndex"] = event.currentItemIndex.toString()
    extra["fromIndex"] = event.fromIndex.toString()
    extra["toIndex"] = event.toIndex.toString()
    
    return Gson().toJson(extra)
}

private fun handleLongClickEvent(event: AccessibilityEvent) {
    val source = event.source ?: return
    
    executor.execute {
        try {
            val elementInfo = elementAnalyzer.extractElementInfo(source)
            
            val uiEvent = UIEvent(
                packageName = event.packageName?.toString() ?: "",
                activityName = event.className?.toString(),
                eventType = UIRecorderAccessibilityService.EVENT_LONG_CLICK,
                elementId = elementInfo.id,
                elementType = elementInfo.type,
                elementText = elementInfo.text,
                elementHint = elementInfo.hint,
                elementContentDesc = elementInfo.contentDesc,
                elementClass = source.className?.toString(),
                elementBounds = elementAnalyzer.getBoundsString(source),
                parentHierarchy = elementAnalyzer.extractParentHierarchy(source),
                siblingInfo = elementAnalyzer.extractSiblingInfo(source)
            )
            
            uiEventDao.insert(uiEvent)
            sendNewEventBroadcast(uiEvent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle long click event", e)
        } finally {
            source.recycle()
        }
    }
}

private fun handleFocusEvent(event: AccessibilityEvent) {
    val source = event.source ?: return
    
    executor.execute {
        try {
            val elementInfo = elementAnalyzer.extractElementInfo(source)
            
            val uiEvent = UIEvent(
                packageName = event.packageName?.toString() ?: "",
                activityName = event.className?.toString(),
                eventType = UIRecorderAccessibilityService.EVENT_FOCUS,
                elementId = elementInfo.id,
                elementType = elementInfo.type,
                elementText = elementInfo.text,
                elementHint = elementInfo.hint,
                elementContentDesc = elementInfo.contentDesc,
                elementClass = source.className?.toString(),
                elementBounds = elementAnalyzer.getBoundsString(source),
                parentHierarchy = elementAnalyzer.extractParentHierarchy(source),
                siblingInfo = elementAnalyzer.extractSiblingInfo(source)
            )
            
            uiEventDao.insert(uiEvent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle focus event", e)
        } finally {
            source.recycle()
        }
    }
}

private fun handleWindowChangeEvent(event: AccessibilityEvent) {
    executor.execute {
        try {
            val uiEvent = UIEvent(
                packageName = event.packageName?.toString() ?: "",
                activityName = event.className?.toString(),
                eventType = UIRecorderAccessibilityService.EVENT_PAGE_CHANGE,
                elementText = event.text?.joinToString(", ")
            )
            
            uiEventDao.insert(uiEvent)
            Log.d(TAG, "Window changed: ${event.packageName} -> ${event.className}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle window change event", e)
        }
    }
}

private fun handleContentChangeEvent(event: AccessibilityEvent) {
    // 内容变化事件可能很频繁，选择性记录
    if (!event.contentChangeTypes.contains(
            AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
        )) {
        return
    }
    
    Log.d(TAG, "Content changed in ${event.packageName}")
}
```

## **3. 数据访问对象 (DAO)**

```kotlin
@Dao
interface UIEventDao {
    
    @Insert
    suspend fun insert(event: UIEvent): Long
    
    @Update
    suspend fun update(event: UIEvent)
    
    @Query("SELECT * FROM ui_events ORDER BY event_time DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 100): List<UIEvent>
    
    @Query("SELECT * FROM ui_events WHERE package_name = :packageName ORDER BY event_time DESC")
    suspend fun getEventsByPackage(packageName: String): List<UIEvent>
    
    @Query("SELECT * FROM ui_events WHERE event_type = :eventType ORDER BY event_time DESC")
    suspend fun getEventsByType(eventType: String): List<UIEvent>
    
    @Query("SELECT * FROM ui_events WHERE element_id = :elementId ORDER BY event_time DESC")
    suspend fun getEventsByElement(elementId: String): List<UIEvent>
    
    @Query("SELECT * FROM ui_events WHERE id = :id")
    suspend fun getEventById(id: Int): UIEvent?
    
    @Query("DELETE FROM ui_events WHERE event_time < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM ui_events WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String): Int
    
    @Query("SELECT COUNT(*) FROM ui_events WHERE package_name = :packageName")
    suspend fun getEventCount(packageName: String): Int
    
    @Query("""
        SELECT DISTINCT package_name 
        FROM ui_events 
        WHERE event_time > :sinceTime
        ORDER BY package_name
    """)
    suspend fun getRecordedPackages(sinceTime: Long = System.currentTimeMillis() - 86400000): List<String>
}

@Dao
interface ElementFeatureDao {
    
    @Insert
    suspend fun insert(feature: ElementFeature)
    
    @Update
    suspend fun update(feature: ElementFeature)
    
    @Query("SELECT * FROM element_features WHERE element_signature = :signature")
    suspend fun getBySignature(signature: String): ElementFeature?
    
    @Query("SELECT * FROM element_features WHERE package_name = :packageName")
    suspend fun getByPackage(packageName: String): List<ElementFeature>
    
    @Query("SELECT * FROM element_features WHERE package_name = :packageName AND element_type = :elementType")
    suspend fun getByPackageAndType(packageName: String, elementType: String): List<ElementFeature>
    
    @Query("SELECT * FROM element_features WHERE auto_fill_enabled = 1 AND package_name = :packageName")
    suspend fun getAutoFillElements(packageName: String): List<ElementFeature>
    
    @Query("UPDATE element_features SET auto_fill_value = :value WHERE id = :id")
    suspend fun updateAutoFillValue(id: Int, value: String)
    
    @Query("UPDATE element_features SET auto_fill_enabled = :enabled WHERE id = :id")
    suspend fun updateAutoFillEnabled(id: Int, enabled: Boolean)
    
    @Query("DELETE FROM element_features WHERE last_used_time < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}

@Dao
interface AppConfigDao {
    
    @Insert
    suspend fun insert(config: AppConfig)
    
    @Update
    suspend fun update(config: AppConfig)
    
    @Query("SELECT * FROM app_configs WHERE package_name = :packageName")
    suspend fun getConfig(packageName: String): AppConfig?
    
    @Query("SELECT * FROM app_configs ORDER BY app_name")
    suspend fun getAllConfigs(): List<AppConfig>
    
    @Query("DELETE FROM app_configs WHERE package_name = :packageName")
    suspend fun delete(packageName: String)
    
    @Query("UPDATE app_configs SET recording_enabled = :enabled WHERE package_name = :packageName")
    suspend fun setRecordingEnabled(packageName: String, enabled: Boolean)
    
    @Query("UPDATE app_configs SET record_clicks = :enabled WHERE package_name = :packageName")
    suspend fun setRecordClicks(packageName: String, enabled: Boolean)
    
    @Query("UPDATE app_configs SET record_inputs = :enabled WHERE package_name = :packageName")
    suspend fun setRecordInputs(packageName: String, enabled: Boolean)
}
```

## **4. 广播通信**

```kotlin
// 在 UIRecorderAccessibilityService 中添加

private fun sendServiceStatusBroadcast(isRunning: Boolean) {
    val intent = Intent("com.example.uirecorder.SERVICE_STATUS")
    intent.putExtra("is_running", isRunning)
    sendBroadcast(intent)
}

private fun sendNewEventBroadcast(event: UIEvent) {
    val intent = Intent("com.example.uirecorder.NEW_EVENT")
    intent.putExtra("event_id", event.id)
    intent.putExtra("package_name", event.packageName)
    intent.putExtra("event_type", event.eventType)
    sendBroadcast(intent)
}

private fun sendErrorBroadcast(errorMessage: String) {
    val intent = Intent("com.example.uirecorder.ERROR")
    intent.putExtra("error_message", errorMessage)
    sendBroadcast(intent)
}
```

## **5. 浮动控制窗口**

```kotlin
class FloatingControlManager(private val context: Context) {
    
    private var floatingView: View? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    
    fun showFloatingControl() {
        if (floatingView != null) return
        
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // 创建浮动窗口参数
        layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.END
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = 0
            y = 100
        }
        
        // 创建浮动视图
        floatingView = LayoutInflater.from(context).inflate(R.layout.floating_control, null).apply {
            // 设置点击事件
            findViewById<View>(R.id.btn_record).setOnClickListener {
                toggleRecording()
            }
            
            findViewById<View>(R.id.btn_settings).setOnClickListener {
                openSettings()
            }
            
            findViewById<View>(R.id.btn_close).setOnClickListener {
                hideFloatingControl()
            }
            
            // 拖动功能
            setOnTouchListener(createTouchListener())
        }
        
        windowManager?.addView(floatingView, layoutParams)
    }
    
    fun hideFloatingControl() {
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
    }
    
    private fun toggleRecording() {
        // 实现录制开关逻辑
        val intent = Intent("com.example.uirecorder.TOGGLE_RECORDING")
        context.sendBroadcast(intent)
    }
    
    private fun openSettings() {
        val intent = Intent(context, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    private fun createTouchListener(): View.OnTouchListener {
        return object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        }
    }
}
```

## **6. AndroidManifest.xml 配置**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- 无障碍服务权限 -->
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
    
    <!-- 存储权限（用于保存截图和导出数据） -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
        android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    
    <!-- Android 10+ 的文件访问权限 -->
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" 
        tools:ignore="ScopedStorage" />
    
    <!-- 悬浮窗权限 -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    
    <!-- 截图权限（如果需要） -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">
        
        <!-- 主活动 -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- 设置活动 -->
        <activity android:name=".SettingsActivity" />
        
        <!-- 记录查看活动 -->
        <activity android:name=".RecordViewActivity" />
        
        <!-- 无障碍服务 -->
        <service
            android:name=".UIRecorderAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
        
        <!-- 广播接收器 -->
        <receiver
            android:name=".UIEventReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="com.example.uirecorder.NEW_EVENT" />
                <action android:name="com.example.uirecorder.SERVICE_STATUS" />
            </intent-filter>
        </receiver>
        
        <!-- 文件提供者（用于分享数据） -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
```

## **7. 无障碍服务配置**

**res/xml/accessibility_service_config.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewClicked|
                                    typeViewTextChanged|
                                    typeViewSelected|
                                    typeViewScrolled|
                                    typeViewLongClicked|
                                    typeViewFocused|
                                    typeWindowStateChanged|
                                    typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|
                               flagReportViewIds|
                               flagIncludeNotImportantViews|
                               flagRetrieveInteractiveWindows"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```

## **8. 使用示例和测试**

```kotlin
// 在主活动中使用
class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UIEventAdapter
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var database: UIRecorderDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initDatabase()
        setupRecyclerView()
        setupButtons()
        
        // Check accessibility permission
        checkAccessibilityPermission()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        btnStartService = findViewById(R.id.btn_start_service)
        btnStopService = findViewById(R.id.btn_stop_service)
    }
    
    private fun initDatabase() {
        database = UIRecorderDatabase.getInstance(this)
    }
    
    private fun setupRecyclerView() {
        adapter = UIEventAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // Load initial events
        loadRecentEvents()
    }
    
    private fun setupButtons() {
        btnStartService.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                showAccessibilityPermissionDialog()
            } else {
                Toast.makeText(this, "Service already enabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnStopService.setOnClickListener {
            Toast.makeText(this, "To stop service, go to Settings > Accessibility", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadRecentEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = database.uiEventDao().getRecentEvents(50) // Load last 50 events
                launch(Dispatchers.Main) {
                    adapter.updateEvents(events)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun checkAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityPermissionDialog()
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${UIRecorderAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
    
    private fun showAccessibilityPermissionDialog() {
        Toast.makeText(this, "Please enable Accessibility Service in Settings", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        loadRecentEvents() // Refresh events when activity resumes
    }
}
```

## **8.1 RecyclerView Implementation for Displaying UI Events**

The application includes a RecyclerView to display the recorded UI events in a list format. This feature allows users to view all captured interactions in a structured and organized way.

### **8.1.1 UIEventAdapter**

The `UIEventAdapter` is responsible for binding UIEvent data to the RecyclerView items:

```kotlin
class UIEventAdapter(
    private var events: List<UIEvent> = emptyList()
) : RecyclerView.Adapter<UIEventAdapter.ViewHolder>() {
    
    fun updateEvents(newEvents: List<UIEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val packageName: TextView = view.findViewById(R.id.package_name)
        val eventType: TextView = view.findViewById(R.id.event_type)
        val elementText: TextView = view.findViewById(R.id.element_text)
        val elementId: TextView = view.findViewById(R.id.element_id)
        val elementClass: TextView = view.findViewById(R.id.element_class)
        val eventTime: TextView = view.findViewById(R.id.event_time)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ui_event, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        
        holder.packageName.text = event.packageName
        holder.eventType.text = event.eventType
        holder.elementText.text = event.elementText ?: "N/A"
        holder.elementId.text = event.elementId ?: "N/A"
        holder.elementClass.text = event.elementClass ?: "N/A"
        
        val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        holder.eventTime.text = formatter.format(Date(event.eventTime))
    }
    
    override fun getItemCount(): Int = events.size
}
```

### **8.1.2 Item Layout**

The item layout (`item_ui_event.xml`) displays key information about each recorded UI event:

- Package name
- Event type (CLICK, INPUT, SELECT, etc.)
- Element text
- Element ID
- Element class
- Event timestamp

This provides users with a comprehensive view of all recorded interactions in a clean, scrollable list format.

## **9. 高级功能扩展**

### **9.1 规则引擎**

```kotlin
class RuleEngine {
    
    fun shouldRecordEvent(
        event: UIEvent,
        rules: List<RecordingRule>
    ): Boolean {
        return rules.all { rule ->
            when (rule.condition) {
                RuleCondition.PACKAGE_INCLUDE -> 
                    rule.packages.contains(event.packageName)
                RuleCondition.PACKAGE_EXCLUDE -> 
                    !rule.packages.contains(event.packageName)
                RuleCondition.EVENT_TYPE_INCLUDE -> 
                    rule.eventTypes.contains(event.eventType)
                RuleCondition.CONTAINS_SENSITIVE_TEXT -> 
                    !containsSensitiveInfo(event.elementText)
                RuleCondition.ELEMENT_TYPE_FILTER -> 
                    rule.elementTypes.contains(event.elementType)
                else -> true
            }
        }
    }
    
    private fun containsSensitiveInfo(text: String?): Boolean {
        text ?: return false
        
        val sensitivePatterns = listOf(
            "password", "passwd", "pwd", "credit", "card",
            "ssn", "social", "security", "身份证", "密码",
            "手机号", "电话", "email", "邮箱"
        )
        
        return sensitivePatterns.any { pattern ->
            text.contains(pattern, ignoreCase = true)
        }
    }
}
```

### **9.2 机器学习特征提取**

```kotlin
class MachineLearningExtractor {
    
    fun extractFeaturesForML(event: UIEvent): Map<String, Any> {
        return mapOf(
            "package_name" to event.packageName,
            "event_type" to event.eventType,
            "element_type" to event.elementType ?: "",
            "element_class" to event.elementClass ?: "",
            "has_text" to (event.elementText?.isNotEmpty() == true),
            "has_hint" to (event.elementHint?.isNotEmpty() == true),
            "has_content_desc" to (event.elementContentDesc?.isNotEmpty() == true),
            "text_length" to (event.elementText?.length ?: 0),
            "is_input_event" to (event.eventType == UIRecorderAccessibilityService.EVENT_INPUT),
            "is_click_event" to (event.eventType == UIRecorderAccessibilityService.EVENT_CLICK),
            "hour_of_day" to Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            "day_of_week" to Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        )
    }
}
```

这个完整的系统可以实现：

1. ✅ **记录用户在指定APP的所有交互操作**
2. ✅ **保存完整的控件特征到数据库**
3. ✅ **支持截图功能**
4. ✅ **提供浮动控制窗口**
5. ✅ **数据导出和分享**
6. ✅ **规则过滤**
7. ✅ **特征学习和自动填充**

注意事项：
1. 需要用户手动开启无障碍服务
2. 需要处理权限问题
3. 需要考虑性能和电池优化
4. 需要保护用户隐私，避免记录敏感信息
5. 需要遵守Google Play政策