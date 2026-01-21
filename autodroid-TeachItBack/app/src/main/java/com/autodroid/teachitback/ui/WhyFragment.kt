package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.teachitback.R
import com.autodroid.teachitback.databinding.FragmentWhyBinding

class WhyFragment : Fragment() {
    private var _binding: FragmentWhyBinding? = null
    private val binding get() = _binding!!

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
        setupUI()
    }

    private fun setupUI() {
        // Add click listeners for interactive elements
        setupCardInteractions()
    }

    private fun setupCardInteractions() {
        // This method can be expanded to add interactive functionality
        // For example, expanding/collapsing sections, showing more details, etc.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
