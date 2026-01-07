package com.autodroid.aas.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

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