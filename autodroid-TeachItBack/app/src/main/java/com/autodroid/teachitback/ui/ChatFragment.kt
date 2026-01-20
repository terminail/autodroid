package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentChatBinding
import com.autodroid.teachitback.viewmodel.AppViewModel

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AppViewModel
    private lateinit var messagesAdapter: MessagesAdapter
    private var topicId: String? = null
    private var topicTitle: String? = null

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
            viewModel.insertMessage(
                topicId = topicId!!,
                content = content,
                senderType = "USER",
                messageType = "TEXT"
            )
            binding.messageInput.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
