package com.autodroid.teachitback.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentChatBinding
import com.autodroid.teachitback.service.VoiceProcessor
import com.autodroid.teachitback.viewmodel.AppViewModel

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AppViewModel
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var voiceProcessor: VoiceProcessor

    private var topicId: String? = null
    private var topicTitle: String? = null

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
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        topicId = arguments?.getString("topicId")
        topicTitle = arguments?.getString("topicTitle")

        voiceProcessor = VoiceProcessor(requireContext())

        setupUI()
        setupRecyclerView()
        observeMessages()
        loadMessages()
    }

    private fun setupUI() {
        binding.topicTitle.text = topicTitle ?: "聊天"
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        binding.voiceButton.setOnClickListener {
            requestOrStartVoiceInput()
        }
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter()

        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messagesAdapter
        }
    }

    private fun observeMessages() {
        viewModel.currentTopicMessages?.observe(viewLifecycleOwner) { messages ->
            messagesAdapter.submitList(messages)
            binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
        }
    }

    private fun loadMessages() {
        topicId?.let {
            viewModel.loadMessagesForTopic(it)
        }
    }

    private fun sendMessage() {
        val content = binding.messageInput.text.toString().trim()
        if (content.isNotEmpty() && topicId != null) {
            // Save user message
            viewModel.insertMessage(
                topicId = topicId!!,
                content = content,
                senderType = "USER",
                messageType = "TEXT"
            )
            binding.messageInput.text?.clear()

            // Send to AI and get response
            viewModel.sendMessageToAI(topicId!!, topicTitle ?: "")
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
        binding.voiceButton.isEnabled = false
        binding.voiceInputProgress.visibility = View.VISIBLE

        voiceProcessor.startListening(
            onResult = { text ->
                if (text.isNotEmpty()) {
                    binding.messageInput.setText(text)
                    sendMessage()
                }
                binding.voiceButton.isEnabled = true
                binding.voiceInputProgress.visibility = View.GONE
            },
            onError = { error ->
                Toast.makeText(requireContext(), "语音识别错误: $error", Toast.LENGTH_SHORT).show()
                binding.voiceButton.isEnabled = true
                binding.voiceInputProgress.visibility = View.GONE
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        voiceProcessor.destroy()
        _binding = null
    }
}
