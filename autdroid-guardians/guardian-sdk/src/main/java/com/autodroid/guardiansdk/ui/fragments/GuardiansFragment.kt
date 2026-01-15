package com.autodroid.guardiansdk.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.guardiansdk.databinding.FragmentGuardiansBinding
import com.autodroid.guardiansdk.ui.adapters.GuardianAdapter
import com.autodroid.guardiansdk.data.model.GuardianInfo

/**
 * 被监护人列表界面
 * 显示被监护人的基本信息和最近联系情况
 */
class GuardiansFragment : Fragment() {

    private var _binding: FragmentGuardiansBinding? = null
    private val binding get() = _binding!!
    private lateinit var guardianAdapter: GuardianAdapter

    companion object {
        fun newInstance() = GuardiansFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuardiansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadGuardianData()
    }

    private fun setupRecyclerView() {
        guardianAdapter = GuardianAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guardianAdapter
        }
    }

    private fun loadGuardianData() {
        // For now, using mock data - this will be replaced with actual data later
        val mockGuardians = listOf(
            GuardianInfo(
                id = 1,
                name = "爸爸",
                avatar = "", // Will use default avatar in adapter
                lastContactTime = "2分钟前",
                lastLocation = "北京市朝阳区xx街道",
                lastAlarmMessage = "收到您的位置信息"
            ),
            GuardianInfo(
                id = 2,
                name = "妈妈",
                avatar = "",
                lastContactTime = "1小时前",
                lastLocation = "上海市浦东新区xx路",
                lastAlarmMessage = "安全到达目的地"
            ),
            GuardianInfo(
                id = 3,
                name = "哥哥",
                avatar = "",
                lastContactTime = "昨天",
                lastLocation = "广州市天河区xx大厦",
                lastAlarmMessage = "正在开会，暂时无法接听"
            )
        )

        // Update adapter with data
        guardianAdapter.updateData(mockGuardians)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}