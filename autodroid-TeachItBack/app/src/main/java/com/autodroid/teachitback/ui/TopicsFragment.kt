package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentTopicsBinding
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.ui.adapter.TopicsAdapter
import com.autodroid.teachitback.viewmodel.AppViewModel

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
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AppViewModel::class.java]

        setupRecyclerView()
        observeTopics()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_topics, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_topic -> {
                showAddTopicDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        topicsAdapter = TopicsAdapter { topic ->
            // 导航到聊天页面，传递topic参数
            val action = TopicsFragmentDirections.actionNavTopicsToChat(
                topicId = topic.id,
                topicTitle = topic.title
            )
            findNavController().navigate(action)
        }

        binding.topicsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = topicsAdapter
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
        val dialog = AddTopicDialogFragment.newInstance()
        dialog.setOnTopicAddedListener {
            
        }
        dialog.show(parentFragmentManager, "AddTopicDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
