package com.autodroid.guardiansdk.ui.wards.detail

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
import com.autodroid.guardiansdk.ui.wards.detail.adapter.WardDetailAdapter
import kotlinx.coroutines.launch

class WardDetailFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WardDetailAdapter
    
    private val viewModel: WardDetailViewModel by viewModels {
        val database = GuardianDatabase.getDatabase(requireContext())
        WardDetailViewModelFactory(database.wardDao(), database.messageDao())
    }

    companion object {
        private const val ARG_WARD_PHONE = "ward_phone_number"
        private const val ARG_WARD_NAME = "ward_name"

        fun newInstance(wardPhoneNumber: String, wardName: String): WardDetailFragment {
            val fragment = WardDetailFragment()
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
        return inflater.inflate(R.layout.guardian_ward_detail_fragment, container, false)
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
        adapter = WardDetailAdapter()
        adapter.setCurrentUserPhoneNumber(viewModel.getCurrentUserPhoneNumber())
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WardDetailFragment.adapter
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