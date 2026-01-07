package com.autodroid.aas.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

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