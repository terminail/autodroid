package com.autodroid.guardiansdk.ui.why

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.guardiansdk.databinding.GuardianFragmentWhyBinding
import com.autodroid.guardiansdk.ui.why.adapter.WhyAdapter
import com.autodroid.guardiansdk.ui.why.model.WhyItem
import com.autodroid.guardiansdk.ui.why.model.WhyItemType

class WhyFragment : Fragment() {

    private var _binding: GuardianFragmentWhyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhyViewModel by viewModels { WhyViewModelFactory(requireContext()) }
    private lateinit var adapter: WhyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GuardianFragmentWhyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        
        viewModel.loadWhyItems()
    }

    private fun setupRecyclerView() {
        adapter = WhyAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WhyFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.whyItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}