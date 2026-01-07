package com.autodroid.trader.aas.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "element_features")
data class ElementFeature(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String?,
    @ColumnInfo(name = "element_signature") val elementSignature: String, // 元素签名（唯一标识）
    @ColumnInfo(name = "element_id") val elementId: String?, // 元素ID
    @ColumnInfo(name = "element_type") val elementType: String,
    @ColumnInfo(name = "element_text") val elementText: String?,
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