package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentWhyBinding
import com.autodroid.teachitback.ui.adapter.WhyAdapter
import com.autodroid.teachitback.viewmodel.WhyViewModel
import kotlinx.coroutines.launch

class WhyFragment : Fragment() {
    private var _binding: FragmentWhyBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: WhyViewModel
    private lateinit var whyAdapter: WhyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[WhyViewModel::class.java]
        
        setupUI()
        setupRecyclerView()
        observeViewModel()
        viewModel.loadWhyData()
    }

    private fun setupUI() {
    }

    private fun setupRecyclerView() {
        whyAdapter = WhyAdapter()
        
        whyAdapter.setOnCopyTopic { presetTopic ->
            viewModel.copyPresetTopic(presetTopic) { success ->
                if (success) {
                    android.widget.Toast.makeText(requireContext(), "已复制到学习列表", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(requireContext(), "复制失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.whyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = whyAdapter
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.whyItems.collect { items ->
                whyAdapter.submitList(items)
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    // Show error message (e.g., using Toast or Snackbar)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
