package com.autodroid.guardiansdk.manager

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.autodroid.guardiansdk.service.AudioRecordingService
import com.autodroid.guardiansdk.util.PreferenceManager
import java.io.File

/**
 * 音频录音管理器
 * 负责管理隐秘录音功能的启动、停止和权限检查
 */
class AudioRecordingManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecordingManager"
        
        // 录音权限
        private const val PERMISSION_RECORD_AUDIO = "android.permission.RECORD_AUDIO"
    }
    
    private val preferenceManager = PreferenceManager(context)
    
    /**
     * 检查录音权限
     */
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, PERMISSION_RECORD_AUDIO) == 
                PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查录音功能是否启用
     */
    fun isAudioRecordingEnabled(): Boolean {
        return preferenceManager.getBoolean("audio_recording_enabled", true)
    }
    
    /**
     * 设置录音功能启用状态
     */
    fun setAudioRecordingEnabled(enabled: Boolean) {
        preferenceManager.putBoolean("audio_recording_enabled", enabled)
    }
    
    /**
     * 获取录音时长设置（分钟）
     */
    fun getRecordingDuration(): Int {
        return preferenceManager.getInt("recording_duration", 5)
    }
    
    /**
     * 设置录音时长
     */
    fun setRecordingDuration(durationMinutes: Int) {
        preferenceManager.putInt("recording_duration", durationMinutes)
    }
    
    /**
     * 启动隐秘录音
     * @param triggerType 触发类型（音量键、浮动窗口等）
     * @param durationMinutes 录音时长（分钟），如果为null则使用默认设置
     */
    fun startStealthRecording(triggerType: String, durationMinutes: Int? = null) {
        if (!isAudioRecordingEnabled()) {
            return
        }
        
        if (!hasRecordAudioPermission()) {
            // 记录权限不足的日志
            android.util.Log.w(TAG, "录音权限不足，无法启动录音")
            return
        }
        
        val actualDuration = durationMinutes ?: getRecordingDuration()
        
        // 验证时长范围（1-30分钟）
        val safeDuration = actualDuration.coerceIn(1, 30)
        
        try {
            AudioRecordingService.startRecording(context, triggerType, safeDuration)
            android.util.Log.i(TAG, "隐秘录音已启动，触发类型: $triggerType, 时长: ${safeDuration}分钟")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "启动隐秘录音失败", e)
        }
    }
    
    /**
     * 停止隐秘录音
     */
    fun stopStealthRecording() {
        try {
            AudioRecordingService.stopRecording(context)
            android.util.Log.i(TAG, "隐秘录音已停止")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "停止隐秘录音失败", e)
        }
    }
    
    /**
     * 检查是否正在录音
     * 注意：由于隐秘录音的特性，这个方法可能无法准确判断
     */
    fun isRecording(): Boolean {
        // 这里可以检查服务是否正在运行
        // 但由于隐秘性要求，可能需要更复杂的状态管理
        return false
    }
    
    /**
     * 获取录音文件列表
     */
    fun getRecordingFiles(): List<String> {
        val recordingsDir = context.getExternalFilesDir("recordings")
        return recordingsDir?.list()?.toList() ?: emptyList()
    }
    
    /**
     * 删除指定录音文件
     */
    fun deleteRecordingFile(fileName: String): Boolean {
        val file = File(context.getExternalFilesDir("recordings"), fileName)
        return file.delete()
    }
    
    /**
     * 清理过期录音文件
     * @param maxAgeDays 最大保留天数
     */
    fun cleanupOldRecordings(maxAgeDays: Int = 7): Int {
        val recordingsDir = context.getExternalFilesDir("recordings")
        val files = recordingsDir?.listFiles() ?: return 0
        
        val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
        var deletedCount = 0
        
        files.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        
        android.util.Log.i(TAG, "清理了 $deletedCount 个过期录音文件")
        return deletedCount
    }
}

/**
 * 录音文件管理器
 */
class RecordingFileManager(private val context: Context) {
    
    fun getRecordingsDirectory(): File {
        return File(context.getExternalFilesDir(null), "recordings").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    fun getRecordingFile(fileName: String): File {
        return File(getRecordingsDirectory(), fileName)
    }
    
    fun getAllRecordingFiles(): List<File> {
        return getRecordingsDirectory().listFiles()?.toList() ?: emptyList()
    }
}