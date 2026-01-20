package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentTopicsBinding
import com.autodroid.teachitback.databinding.ItemTopicBinding
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.viewmodel.AppViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TopicsFragment : Fragment() {
    private var _binding: FragmentTopicsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AppViewModel
    private lateinit var topicsAdapter: TopicsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        setupRecyclerView()
        setupFab()
        observeTopics()
    }

    private fun setupRecyclerView() {
        topicsAdapter = TopicsAdapter { topic ->
            val chatFragment = ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("topicId", topic.id)
                    putString("topicTitle", topic.title)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.topicsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = topicsAdapter
        }
    }

    private fun setupFab() {
        binding.addTopicFab.setOnClickListener {
            showAddTopicDialog()
        }
    }

    private fun observeTopics() {
        viewModel.topics.observe(viewLifecycleOwner) { topics ->
            if (topics.isNullOrEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.topicsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.topicsRecyclerView.visibility = View.VISIBLE
                topicsAdapter.submitList(topics)
            }
        }
    }

    private fun showAddTopicDialog() {
        val title = binding.newTopicTitle.text.toString().trim()
        val description = binding.newTopicDescription.text.toString().trim()

        if (title.isNotEmpty()) {
            viewModel.insertTopic(title, description)
            binding.newTopicTitle.text?.clear()
            binding.newTopicDescription.text?.clear()
            Toast.makeText(requireContext(), "主题已创建", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "请输入主题名称", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
