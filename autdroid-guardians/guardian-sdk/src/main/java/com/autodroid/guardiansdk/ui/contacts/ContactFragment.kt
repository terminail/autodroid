package com.autodroid.guardiansdk.ui.contacts

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
import com.autodroid.guardiansdk.data.entity.Contact
import com.autodroid.guardiansdk.data.entity.ContactType
import com.autodroid.guardiansdk.ui.contacts.adapter.ContactAdapter
import com.autodroid.guardiansdk.ui.contacts.model.ContactItem
import com.autodroid.guardiansdk.ui.contacts.detail.ContactDetailFragment
import kotlinx.coroutines.launch

class ContactFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    
    private val viewModel: ContactViewModel by viewModels {
        ContactViewModelFactory(
            GuardianDatabase.getDatabase(requireContext())
        )
    }

    companion object {
        fun newInstance() = ContactFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.guardian_fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupObservers()
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        android.util.Log.d("ContactFragment", "=== RecyclerView found: ${recyclerView != null} ===")
        
        contactAdapter = ContactAdapter()
        android.util.Log.d("ContactFragment", "=== ContactAdapter created ===")
        
        // 设置点击监听器
        contactAdapter.setOnItemClickListener(object : ContactAdapter.OnItemClickListener {
            override fun onWardClick(phoneNumber: String, name: String) {
                android.util.Log.d("ContactFragment", "=== onWardClick received: phoneNumber=$phoneNumber, name=$name ===")
                openContactDetail(phoneNumber, name)
            }
        })
        android.util.Log.d("ContactFragment", "=== Click listener set on adapter ===")
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactAdapter
        }
        android.util.Log.d("ContactFragment", "=== RecyclerView setup complete ===")
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.contacts.collect { contacts ->
                android.util.Log.d("ContactFragment", "=== 收到联系人数据，数量: ${contacts.size} ===")
                
                contacts.forEach { contact ->
                    android.util.Log.d("ContactFragment", "=== 联系人: ${contact.name} (${contact.phoneNumber}) - 类型: ${contact.type} ===")
                }
                
                val contactItems = contacts.map { contact ->
                    ContactItem.ContactCard(
                        phoneNumber = contact.phoneNumber,
                        name = contact.name,
                        contactType = if (contact.type == ContactType.WARD) "被监护人" else "监护人",
                        lastContactTime = getLastContactTime(contact), // 根据实际数据填充
                        lastLocation = getLastLocation(contact), // 根据实际数据填充
                        lastAlarmMessage = getLastAlarmMessage(contact) // 根据实际数据填充
                    )
                }
                contactAdapter.updateData(contactItems)
                
                android.util.Log.d("ContactFragment", "=== 更新适配器数据完成 ===")
            }
        }
    }
    
    /**
     * 获取最后联系时间
     */
    private fun getLastContactTime(contact: Contact): String {
        return if (contact.lastMessageTime > 0) {
            // 这里可以根据时间戳转换为可读的时间格式
            "最近联系: ${android.text.format.DateFormat.format("MM-dd HH:mm", contact.lastMessageTime)}"
        } else {
            "暂无联系"
        }
    }
    
    /**
     * 获取最后位置信息
     */
    private fun getLastLocation(contact: Contact): String {
        // 这里可以从消息数据中获取最后的位置信息
        return if (contact.type == ContactType.WARD) {
            "待获取位置信息"
        } else {
            ""
        }
    }
    
    /**
     * 获取最后报警信息
     */
    private fun getLastAlarmMessage(contact: Contact): String {
        return if (contact.type == ContactType.WARD && contact.alarmCount > 0) {
            "报警次数: ${contact.alarmCount}"
        } else {
            ""
        }
    }

    /**
     * 打开被监护人详情页面
     */
    private fun openContactDetail(phoneNumber: String, name: String) {
        android.util.Log.d("ContactFragment", "=== openWardDetail called: phoneNumber=$phoneNumber, name=$name ===")
        
        try {
            val detailFragment = ContactDetailFragment.newInstance(phoneNumber, name)
            
            // 使用Activity的FragmentManager，而不是parentFragmentManager
            val fragmentManager = (activity as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
            if (fragmentManager == null) {
                android.util.Log.e("ContactFragment", "=== ERROR: Cannot get supportFragmentManager from activity ===")
                return
            }
            
            android.util.Log.d("ContactFragment", "=== Executing fragment transaction... ===")
            
            // 执行Fragment事务
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit()
                
            android.util.Log.d("ContactFragment", "=== Fragment transaction committed successfully ===")
        } catch (e: Exception) {
            android.util.Log.e("ContactFragment", "=== ERROR in openWardDetail ===", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}

