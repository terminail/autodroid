package com.autodroid.sms.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.autodroid.sms.R
import com.autodroid.sms.service.SmsSenderService

class ComposeActivity : AppCompatActivity() {
    
    private lateinit var etRecipient: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    
    private val REQUEST_SMS_PERMISSION = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ComposeActivity", "onCreate called")
        setContentView(R.layout.activity_compose)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "新建消息"
        
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        etRecipient = findViewById(R.id.et_recipient)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)
    }
    
    private fun setupClickListeners() {
        btnSend.setOnClickListener {
            Log.d("ComposeActivity", "Send button clicked")
            val recipient = etRecipient.text.toString().trim()
            val message = etMessage.text.toString().trim()
            
            Log.d("ComposeActivity", "Recipient: $recipient, Message: $message")
            
            if (recipient.isEmpty()) {
                Toast.makeText(this, "请输入收件人号码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (message.isEmpty()) {
                Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (checkSmsPermission()) {
                sendSms(recipient, message)
            } else {
                requestSmsPermission()
            }
        }
    }
    
    private fun checkSmsPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        Log.d("ComposeActivity", "SMS permission granted: $granted")
        return granted
    }
    
    private fun requestSmsPermission() {
        Log.d("ComposeActivity", "Requesting SMS permission")
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("ComposeActivity", "onRequestPermissionsResult: requestCode=$requestCode, grantResults=${grantResults.toList()}")
        
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val recipient = etRecipient.text.toString().trim()
                val message = etMessage.text.toString().trim()
                sendSms(recipient, message)
            } else {
                Toast.makeText(this, "需要发送短信权限", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun sendSms(recipient: String, message: String) {
        Log.d("ComposeActivity", "sendSms called: recipient=$recipient, message=$message")
        
        try {
            val intent = SmsSenderService.createSendSmsIntent(this, recipient, message)
            Log.d("ComposeActivity", "Starting service with intent: $intent")
            val result = startService(intent)
            Log.d("ComposeActivity", "Service started, result: $result")
            
            Toast.makeText(this, "短信发送中...", Toast.LENGTH_SHORT).show()
            
            etRecipient.text.clear()
            etMessage.text.clear()
        } catch (e: Exception) {
            Log.e("ComposeActivity", "Failed to start service", e)
            Toast.makeText(this, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}