package com.autodroid.teachitback.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.teachitback.MainActivity
import com.autodroid.teachitback.R
import com.autodroid.teachitback.service.AIRouterService
import com.autodroid.teachitback.ui.adapter.ChatItem
import com.autodroid.teachitback.databinding.FragmentChatBinding
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.service.VoiceProcessor
import com.autodroid.teachitback.service.InputSuggestionDetector
import com.autodroid.teachitback.ui.adapter.ChatAdapter
import com.autodroid.teachitback.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatViewModel
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var voiceProcessor: VoiceProcessor
    private lateinit var aiRouterService: AIRouterService
    private lateinit var inputSuggestionDetector: InputSuggestionDetector

    private val args: ChatFragmentArgs by navArgs()
    
    // 输入建议相关变量
    private var lastInputTime = 0L
    private var suggestionDebounceDelay = 500L // 500ms防抖延迟

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startVoiceInput()
        } else {
            Toast.makeText(requireContext(), "需要录音权限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        
        voiceProcessor = VoiceProcessor(requireContext())
        aiRouterService = AIRouterService()
        inputSuggestionDetector = InputSuggestionDetector()

        setupUI()
        setupRecyclerView()
        observeViewModel()
        loadTopicAndMessages()
    }

    private fun setupUI() {
        // Set toolbar title to topic name
        setToolbarTitle(args.topicTitle ?: "聊天")

        // 初始化按钮状态：默认显示添加按钮，隐藏发送按钮
        binding.addButton.visibility = View.VISIBLE
        binding.sendButton.visibility = View.GONE
        binding.sendButton.isEnabled = false

        // 监听输入框内容变化，控制发送按钮状态
        binding.messageInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasText = !s.isNullOrEmpty()
                binding.sendButton.isEnabled = hasText
                
                // 微信逻辑：有文字时显示发送按钮，隐藏添加按钮
                if (hasText) {
                    binding.sendButton.visibility = View.VISIBLE
                    binding.addButton.visibility = View.GONE
                } else {
                    binding.sendButton.visibility = View.GONE
                    binding.addButton.visibility = View.VISIBLE
                }
            }
        })

        // 语音/文字输入切换
        var isVoiceMode = false
        binding.voiceToggleButton.setOnClickListener {
            isVoiceMode = !isVoiceMode
            if (isVoiceMode) {
                // 切换到语音模式
                binding.voiceToggleButton.setImageResource(R.drawable.ic_keyboard)
                binding.messageInput.hint = "点击说话"
                binding.messageInput.isEnabled = false
                binding.messageInput.setText("")
                // 这里可以添加语音输入逻辑
            } else {
                // 切换到文字模式
                binding.voiceToggleButton.setImageResource(R.drawable.ic_voice)
                binding.messageInput.hint = ""
                binding.messageInput.isEnabled = true
            }
        }

        // 发送按钮 - 集成输入建议系统
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                // 发送前检测，提供优化建议
                lifecycleScope.launch {
                    val suggestion = inputSuggestionDetector.analyzeBeforeSend(message)
                    if (suggestion.shouldShowDialog) {
                        showInputSuggestionDialog(suggestion, message)
                    } else {
                        // 直接发送，不弹出对话框
                        sendMessageDirectly(message, suggestion.suggestedService)
                    }
                }
            }
        }

        // 添加按钮 - 显示/隐藏附件功能面板
        binding.addButton.setOnClickListener {
            toggleAttachmentPanel()
        }

        // 点击输入框或其他区域时隐藏附件面板
        binding.messageInput.setOnClickListener {
            hideAttachmentPanel()
        }

        // 设置附件功能图标的点击事件
        setupAttachmentIcons()
    }

    private fun setToolbarTitle(title: String) {
        (requireActivity() as? MainActivity)?.supportActionBar?.title = title
    }

    private fun showAddOptionsMenu() {
        val options = arrayOf("相册", "拍照", "PDF", "文本")
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("选择附件类型")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectFromGallery()
                    1 -> takePhoto()
                    2 -> selectPDF()
                    3 -> selectTextFile()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun selectFromGallery() {
        Toast.makeText(requireContext(), "打开相册选择图片", Toast.LENGTH_SHORT).show()
        // 这里可以添加相册选择逻辑
    }

    private fun takePhoto() {
        Toast.makeText(requireContext(), "打开相机拍照", Toast.LENGTH_SHORT).show()
        // 这里可以添加拍照逻辑
    }

    private fun selectPDF() {
        Toast.makeText(requireContext(), "选择PDF文件", Toast.LENGTH_SHORT).show()
        // 这里可以添加PDF选择逻辑
    }

    private fun selectTextFile() {
        Toast.makeText(requireContext(), "选择文本文件", Toast.LENGTH_SHORT).show()
        // 这里可以添加文本文件选择逻辑
    }

    private fun toggleAttachmentPanel() {
        val isVisible = binding.attachmentPanel.visibility == View.VISIBLE
        if (isVisible) {
            hideAttachmentPanel()
        } else {
            showAttachmentPanel()
        }
    }

    private fun showAttachmentPanel() {
        binding.attachmentPanel.visibility = View.VISIBLE
        // 关闭输入法
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
    }

    private fun hideAttachmentPanel() {
        binding.attachmentPanel.visibility = View.GONE
    }

    private fun setupAttachmentIcons() {
        // 获取附件面板中的图标并设置点击事件
        val attachmentPanel = binding.attachmentPanel
        
        // 为每个图标容器设置点击事件
        for (i in 0 until attachmentPanel.childCount) {
            val iconContainer = attachmentPanel.getChildAt(i) as? LinearLayout
            iconContainer?.setOnClickListener {
                when (i) {
                    0 -> selectFromGallery()
                    1 -> takePhoto()
                    2 -> selectPDF()
                    3 -> selectTextFile()
                }
                hideAttachmentPanel()
            }
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()

        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.chatItems.collect { chatItems ->
                chatAdapter.submitList(chatItems)
                binding.messagesRecyclerView.scrollToPosition(chatItems.size - 1)
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadTopicAndMessages() {
        android.util.Log.d("ChatFragment", "Loading topic and messages for topicId: ${args.topicId}, topicTitle: ${args.topicTitle}")
        viewModel.loadTopicAndMessages(args.topicId)
    }

    /**
     * 发送消息
     */
    fun sendMessage() {
        val content = binding.messageInput.text.toString().trim()
        if (content.isNotEmpty()) {
            viewModel.sendUserMessage(content, args.topicId)
            binding.messageInput.text?.clear()
        }
    }

    /**
     * 显示输入建议对话框
     * 只显示两个按钮并排，点击外部取消
     */
    private fun showInputSuggestionDialog(suggestion: InputSuggestionDetector.SuggestionResult, originalMessage: String) {
        val message = buildString {
            append(suggestion.message)
            suggestion.suggestion?.let {
                append("\n\n")
                append(it)
            }
        }

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("输入优化建议")
            .setMessage(message)
            .setPositiveButton("采纳并发送") { _, _ ->
                // 优化后发送（可以在这里添加实际的优化逻辑）
                sendMessageDirectly(originalMessage, suggestion.suggestedService)
            }
            .setNegativeButton("忽略并发送") { _, _ ->
                // 原样发送
                sendMessageDirectly(originalMessage)
            }
            .setCancelable(true)  // 允许点击外部取消
            .create()
        
        dialog.show()
    }

    /**
     * 直接发送消息（可选择指定服务）- 公开方法，可从外部调用
     */
    fun sendMessageDirectly(message: String, suggestedService: String? = null) {
        if (message.isNotEmpty()) {
            android.util.Log.d("ChatFragment", "直接发送消息: '$message'")
            viewModel.sendUserMessage(message, args.topicId, suggestedService)
            binding.messageInput.text?.clear()
        }
    }

    /**
     * 获取服务的显示名称
     */
    private fun getServiceDisplayName(serviceId: String): String {
        return when (serviceId) {
            "tinybert_local" -> "TinyBERT（快速响应）"
            "chatglm_local" -> "ChatGLM（本地模型）"
            "tencent-hunyuan" -> "腾讯混元（云端）"
            "deepseek" -> "DeepSeek（云端）"
            "doubao" -> "豆包（云端）"
            "baidu" -> "百度文心（云端）"
            "alibaba" -> "阿里通义（云端）"
            "kimi" -> "Kimi（云端）"
            else -> serviceId
        }
    }

    private fun requestOrStartVoiceInput() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceInput()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(requireContext(), "需要录音权限才能使用语音输入", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startVoiceInput() {
        // 语音输入逻辑暂时保留，但需要重新设计界面
        voiceProcessor.startListening(
            onResult = { text ->
                if (text.isNotEmpty()) {
                    binding.messageInput.setText(text)
                    sendMessage()
                }
            },
            onError = { error ->
                Toast.makeText(requireContext(), "语音识别错误: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        voiceProcessor.destroy()
        _binding = null
    }
}
