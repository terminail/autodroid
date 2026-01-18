package com.autodroid.sms.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.sms.R
import com.autodroid.sms.data.model.SmsMessage
import com.autodroid.sms.service.SmsSenderService
import com.autodroid.sms.ui.adapter.MessageAdapter
import com.autodroid.sms.ui.viewmodel.ConversationViewModel
import com.autodroid.sms.ui.viewmodel.ConversationViewModelFactory

class ConversationActivity : AppCompatActivity() {
    
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var messageAdapter: MessageAdapter
    
    private val viewModel: ConversationViewModel by viewModels {
        ConversationViewModelFactory(this)
    }
    
    private var threadId: Long = 0
    private var address: String = ""
    
    private val REQUEST_SMS_PERMISSION = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        val contactName = intent.getStringExtra("contact_name") ?: intent.getStringExtra("address") ?: "未知联系人"
        supportActionBar?.title = contactName
        
        threadId = intent.getLongExtra("thread_id", 0)
        address = intent.getStringExtra("address") ?: ""
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
    }
    
    private fun initViews() {
        rvMessages = findViewById(R.id.rv_messages)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)
    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        
        rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ConversationActivity)
            adapter = messageAdapter
        }
    }
    
    private fun setupClickListeners() {
        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            
            if (message.isEmpty()) {
                Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (checkSmsPermission()) {
                sendMessage(message)
            } else {
                requestSmsPermission()
            }
        }
    }
    
    private fun loadMessages() {
        if (threadId == 0L && address.isNotEmpty()) {
            loadMessagesByAddress(address)
        } else if (threadId > 0L) {
            loadMessagesByThreadId(threadId)
        }
    }
    
    private fun loadMessagesByThreadId(threadId: Long) {
        viewModel.getMessagesByThread(threadId).observe(this) { messages ->
            messageAdapter.submitList(messages)
            rvMessages.scrollToPosition(messages.size - 1)
        }
    }
    
    private fun loadMessagesByAddress(address: String) {
        viewModel.getMessagesByAddress(address).observe(this) { messages ->
            messageAdapter.submitList(messages)
            rvMessages.scrollToPosition(messages.size - 1)
        }
    }
    
    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val message = etMessage.text.toString().trim()
                sendMessage(message)
            } else {
                Toast.makeText(this, "需要发送短信权限", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun sendMessage(message: String) {
        val intent = SmsSenderService.createSendSmsIntent(this, address, message)
        startService(intent)
        
        Toast.makeText(this, "发送中...", Toast.LENGTH_SHORT).show()
        
        etMessage.text.clear()
        
        loadMessages()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}