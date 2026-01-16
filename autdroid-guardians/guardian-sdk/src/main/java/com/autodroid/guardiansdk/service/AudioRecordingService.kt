package com.autodroid.guardiansdk.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.autodroid.guardiansdk.util.EncryptionUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecordingService : Service() {
    
    companion object {
        private const val TAG = "AudioRecordingService"
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val EXTRA_TRIGGER_TYPE = "TRIGGER_TYPE"
        const val EXTRA_DURATION_MINUTES = "DURATION_MINUTES"
        const val DEFAULT_DURATION_MINUTES = 5
        
        fun startRecording(context: Context, triggerType: String, durationMinutes: Int = DEFAULT_DURATION_MINUTES) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_TRIGGER_TYPE, triggerType)
                putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            }
            ContextCompat.startForegroundService(context, intent)
        }
        
        fun stopRecording(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingTimer: android.os.CountDownTimer? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val triggerType = intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: "UNKNOWN"
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, DEFAULT_DURATION_MINUTES)
                startAudioRecording(triggerType, durationMinutes)
            }
            ACTION_STOP_RECORDING -> {
                stopAudioRecording()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    private fun startAudioRecording(triggerType: String, durationMinutes: Int) {
        try {
            // 创建录音文件
            recordingFile = createRecordingFile(triggerType)
            
            // 初始化MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }
            
            // 启动定时器，自动停止录音
            recordingTimer = object : android.os.CountDownTimer(durationMinutes * 60 * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    // 每秒钟检查是否需要停止
                }
                
                override fun onFinish() {
                    stopAudioRecording()
                    stopSelf()
                }
            }.start()
            
            Log.i(TAG, "开始录音，触发类型: $triggerType, 时长: ${durationMinutes}分钟")
            
        } catch (e: Exception) {
            Log.e(TAG, "录音启动失败", e)
            stopSelf()
        }
    }
    
    private fun stopAudioRecording() {
        try {
            recordingTimer?.cancel()
            
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            // 加密录音文件
            recordingFile?.let { file ->
                encryptAudioFile(file)
            }
            
            Log.i(TAG, "录音停止完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "录音停止失败", e)
        }
    }
    
    private fun createRecordingFile(triggerType: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "audio_${triggerType}_${timestamp}.mp4"
        
        val audioDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recordings")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        
        return File(audioDir, fileName)
    }
    
    private fun encryptAudioFile(file: File) {
        try {
            val encryptedData = EncryptionUtils.encryptFile(file)
            val encryptedFile = File(file.parent, "encrypted_" + file.name)
            
            FileOutputStream(encryptedFile).use { fos ->
                fos.write(encryptedData)
            }
            
            // 删除原始未加密文件
            file.delete()
            
            Log.i(TAG, "录音文件加密完成: ${encryptedFile.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "录音文件加密失败", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAudioRecording()
    }
}