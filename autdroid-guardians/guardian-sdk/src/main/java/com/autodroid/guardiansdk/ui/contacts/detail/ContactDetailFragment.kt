package com.autodroid.guardiansdk.ui.contacts.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.ui.contacts.detail.adapter.ContactDetailAdapter
import kotlinx.coroutines.launch

class ContactDetailFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactDetailAdapter
    
    private val viewModel: ContactDetailViewModel by viewModels {
        val database = GuardianDatabase.getDatabase(requireContext())
        ContactDetailViewModelFactory(database.contactDao(), database.messageDao())
    }

    companion object {
        private const val ARG_WARD_PHONE = "ward_phone_number"
        private const val ARG_WARD_NAME = "ward_name"

        fun newInstance(wardPhoneNumber: String, wardName: String): ContactDetailFragment {
            val fragment = ContactDetailFragment()
            val args = Bundle()
            args.putString(ARG_WARD_PHONE, wardPhoneNumber)
            args.putString(ARG_WARD_NAME, wardName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.guardian_contact_detail_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val wardPhoneNumber = arguments?.getString(ARG_WARD_PHONE) ?: return
        val wardName = arguments?.getString(ARG_WARD_NAME) ?: ""

        // 设置标题
        activity?.title = wardName

        setupRecyclerView(view)
        setupObservers()

        // 加载数据
        viewModel.loadWardDetail(wardPhoneNumber)
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = ContactDetailAdapter()
        adapter.setCurrentUserPhoneNumber(viewModel.getCurrentUserPhoneNumber())
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ContactDetailFragment.adapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.detailItems.collect { items ->
                adapter.updateData(items)
                // 滚动到底部
                if (items.isNotEmpty()) {
                    recyclerView.scrollToPosition(items.size - 1)
                }
            }
        }
    }
}