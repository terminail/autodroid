package com.autodroid.guardiansdk.ui.wards

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
import com.autodroid.guardiansdk.ui.wards.adapter.WardAdapter
import com.autodroid.guardiansdk.ui.wards.model.WardItem
import com.autodroid.guardiansdk.ui.wards.detail.WardDetailFragment
import kotlinx.coroutines.launch

class WardFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var wardAdapter: WardAdapter
    
    private val viewModel: WardViewModel by viewModels {
        WardViewModelFactory(
            GuardianDatabase.getDatabase(requireContext())
        )
    }

    companion object {
        fun newInstance() = WardFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.guardian_fragment_wards, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupObservers()
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        android.util.Log.d("WardFragment", "=== RecyclerView found: ${recyclerView != null} ===")
        
        wardAdapter = WardAdapter()
        android.util.Log.d("WardFragment", "=== WardAdapter created ===")
        
        // 设置点击监听器
        wardAdapter.setOnItemClickListener(object : WardAdapter.OnItemClickListener {
            override fun onWardClick(phoneNumber: String, name: String) {
                android.util.Log.d("WardFragment", "=== onWardClick received: phoneNumber=$phoneNumber, name=$name ===")
                openWardDetail(phoneNumber, name)
            }
        })
        android.util.Log.d("WardFragment", "=== Click listener set on adapter ===")
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = wardAdapter
        }
        android.util.Log.d("WardFragment", "=== RecyclerView setup complete ===")
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.wards.collect { wards ->
                val wardItems = wards.map { ward ->
                    WardItem.WardCard(
                        phoneNumber = ward.phoneNumber,
                        name = ward.name,
                        lastContactTime = "", // 可以根据实际数据填充
                        lastLocation = "", // 可以根据实际数据填充
                        lastAlarmMessage = "" // 可以根据实际数据填充
                    )
                }
                wardAdapter.updateData(wardItems)
            }
        }
    }

    /**
     * 打开被监护人详情页面
     */
    private fun openWardDetail(phoneNumber: String, name: String) {
        android.util.Log.d("WardFragment", "=== openWardDetail called: phoneNumber=$phoneNumber, name=$name ===")
        
        try {
            val detailFragment = WardDetailFragment.newInstance(phoneNumber, name)
            
            // 使用Activity的FragmentManager，而不是parentFragmentManager
            val fragmentManager = (activity as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
            if (fragmentManager == null) {
                android.util.Log.e("WardFragment", "=== ERROR: Cannot get supportFragmentManager from activity ===")
                return
            }
            
            android.util.Log.d("WardFragment", "=== Executing fragment transaction... ===")
            
            // 执行Fragment事务
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit()
                
            android.util.Log.d("WardFragment", "=== Fragment transaction committed successfully ===")
        } catch (e: Exception) {
            android.util.Log.e("WardFragment", "=== ERROR in openWardDetail ===", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}

