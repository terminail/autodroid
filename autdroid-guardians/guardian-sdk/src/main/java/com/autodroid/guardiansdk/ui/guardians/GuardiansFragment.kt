package com.autodroid.guardiansdk.ui.guardians

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.guardians.adapter.GuardianAdapter
import com.autodroid.guardiansdk.ui.guardians.model.GuardianItem

class GuardiansFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var guardianAdapter: GuardianAdapter

    companion object {
        fun newInstance() = GuardiansFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.guardian_fragment_guardians, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        loadGuardianData()
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        guardianAdapter = GuardianAdapter()
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guardianAdapter
        }
    }

    private fun loadGuardianData() {
        val guardianItems = listOf(
            GuardianItem.GuardianCard(
                id = 1,
                name = "爸爸",
                lastContactTime = "2分钟前",
                lastLocation = "北京市朝阳区xx街道",
                lastAlarmMessage = "收到您的位置信息"
            ),
            GuardianItem.GuardianCard(
                id = 2,
                name = "妈妈",
                lastContactTime = "1小时前",
                lastLocation = "上海市浦东新区xx路",
                lastAlarmMessage = "安全到达目的地"
            ),
            GuardianItem.GuardianCard(
                id = 3,
                name = "哥哥",
                lastContactTime = "昨天",
                lastLocation = "广州市天河区xx大厦",
                lastAlarmMessage = "正在开会，暂时无法接听"
            )
        )

        guardianAdapter.updateData(guardianItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}

