package com.autodroid.guardiansdk.ui.settings.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.autodroid.guardiansdk.databinding.FragmentEmergencyContactDetailBinding

/**
 * 紧急联系人详细设置页面
 */
class EmergencyContactDetailFragment : Fragment() {

    private var _binding: FragmentEmergencyContactDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = EmergencyContactDetailFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyContactDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        loadContactData()
    }

    private fun setupViews() {
        binding.btnAddContact.setOnClickListener {
            // 打开添加联系人对话框
            showAddContactDialog()
        }
        
        binding.btnSyncPasswordBook.setOnClickListener {
            // 同步密码本
            syncPasswordBook()
        }
    }

    private fun loadContactData() {
        // TODO: 从数据库加载联系人数据
    }

    private fun showAddContactDialog() {
        // TODO: 实现添加联系人对话框
    }

    private fun syncPasswordBook() {
        // TODO: 实现密码本同步
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}