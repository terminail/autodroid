package com.autodroid.guardiansdk.ui.settings.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.autodroid.guardiansdk.R

/**
 * 监护人详细设置页面
 */
class ContactGuardianDetailFragment : Fragment() {

    companion object {
        fun newInstance() = ContactGuardianDetailFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.guardian_fragment_contact_guardian_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews(view)
        loadContactData()
    }

    private fun setupViews(view: View) {
        val btnAddContact = view.findViewById<Button>(R.id.btnAddContact)
        val btnSyncPasswordBook = view.findViewById<Button>(R.id.btnSyncPasswordBook)
        
        btnAddContact.setOnClickListener {
            // 打开添加联系人对话框
            showAddContactDialog()
        }
        
        btnSyncPasswordBook.setOnClickListener {
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
    }
}