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
        const val SEGMENT_DURATION_MINUTES = 2 // 每段录音时长：2分钟

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
    private var totalRecordingTimer: android.os.CountDownTimer? = null
    private var totalDurationMinutes = 0
    private var segmentTimer: android.os.CountDownTimer? = null
    private var currentSegment = 0
    private var emailSender: EmailSender? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        emailSender = EmailSender(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val triggerType = intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: "UNKNOWN"
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, DEFAULT_DURATION_MINUTES)
                startSegmentedRecording(triggerType, durationMinutes)
            }
            ACTION_STOP_RECORDING -> {
                stopSegmentedRecording()
                stopSelf()
            }
        }
        return START_STICKY
    }

    /**
     * 开始分段录音
     * 每段录音2分钟，录完一段后自动加密并发送邮件，然后开始下一段
     */
    private fun startSegmentedRecording(triggerType: String, requestedDuration: Int) {
        this.totalDurationMinutes = requestedDuration.coerceIn(1, 30)
        currentSegment = 0
        Log.i(TAG, "开始分段录音，总时长: ${this.totalDurationMinutes}分钟，每段: ${SEGMENT_DURATION_MINUTES}分钟")

        // 启动第一段录音
        startNextSegment(triggerType, this.totalDurationMinutes)

        // 设置总时长定时器
        totalRecordingTimer = object : android.os.CountDownTimer(
            this.totalDurationMinutes * 60 * 1000L,
            1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                // 每秒检查
            }

            override fun onFinish() {
                stopSegmentedRecording()
                stopSelf()
            }
        }.start()
    }

    /**
     * 开始下一段录音
     */
    private fun startNextSegment(triggerType: String, totalDuration: Int) {
        currentSegment++
        val remainingTime = totalDuration * 60 - (currentSegment - 1) * SEGMENT_DURATION_MINUTES * 60
        val currentSegmentDuration = minOf(SEGMENT_DURATION_MINUTES * 60 * 1000L, remainingTime * 1000L)

        startAudioRecording(triggerType, currentSegment, currentSegmentDuration)

        // 设置本段录音定时器
        segmentTimer = object : android.os.CountDownTimer(currentSegmentDuration, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                // 每秒检查
            }

            override fun onFinish() {
                // 本段录音结束，停止当前录音
                stopCurrentSegment()

                // 检查是否还有剩余时间，如果有则开始下一段
                if (remainingTime > SEGMENT_DURATION_MINUTES * 60) {
                    startNextSegment(triggerType, totalDuration)
                }
            }
        }.start()
    }

    private fun startAudioRecording(triggerType: String, segmentNumber: Int, durationMillis: Long) {
        try {
            // 创建录音文件（包含段号）
            recordingFile = createRecordingFile(triggerType, segmentNumber)

            // 初始化MediaRecorder
            // 使用 64kbps 比特率，确保每段2分钟录音文件大小约 1MB
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000) // 64kbps，降低比特率以减小文件大小
                setAudioSamplingRate(44100)
                setAudioChannels(1) // 单声道，进一步减小文件大小
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }

            Log.i(TAG, "开始第${segmentNumber}段录音，触发类型: $triggerType, 预计时长: ${durationMillis / 1000}秒")

        } catch (e: Exception) {
            Log.e(TAG, "录音启动失败", e)
            stopSelf()
        }
    }

    /**
     * 停止当前段录音，加密并发送邮件
     */
    private fun stopCurrentSegment() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // 加密录音文件
            recordingFile?.let { file ->
                val encryptedFile = encryptAudioFile(file)
                // 尝试发送邮件
                emailSender?.let { sender ->
                    if (sender.canSendEmail()) {
                        sender.sendEmailWithAttachment(encryptedFile)
                        Log.i(TAG, "第${currentSegment}段录音已发送邮件: ${encryptedFile.name}")
                    } else {
                        Log.w(TAG, "未配置邮箱，第${currentSegment}段录音仅本地保存")
                    }
                }
            }

            Log.i(TAG, "第${currentSegment}段录音完成")

        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
        }
    }

    /**
     * 停止所有录音
     */
    private fun stopSegmentedRecording() {
        try {
            totalRecordingTimer?.cancel()
            segmentTimer?.cancel()
            stopCurrentSegment()
            Log.i(TAG, "所有录音已停止，共${currentSegment}段")

        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
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
    
    private fun createRecordingFile(triggerType: String, segmentNumber: Int): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "audio_${triggerType}_seg${segmentNumber}_${timestamp}.mp4"

        val audioDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recordings")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        return File(audioDir, fileName)
    }
    
    private fun encryptAudioFile(file: File): File {
        return try {
            val encryptedData = EncryptionUtils.encryptFile(file)
            val encryptedFile = File(file.parent, "encrypted_" + file.name)

            FileOutputStream(encryptedFile).use { fos ->
                fos.write(encryptedData)
            }

            // 删除原始未加密文件
            file.delete()

            Log.i(TAG, "录音文件加密完成: ${encryptedFile.name}")

            encryptedFile
        } catch (e: Exception) {
            Log.e(TAG, "录音文件加密失败", e)
            // 加密失败返回原文件
            file
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSegmentedRecording()
    }
}