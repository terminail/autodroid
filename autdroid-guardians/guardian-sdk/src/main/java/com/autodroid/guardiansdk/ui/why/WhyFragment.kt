package com.autodroid.guardiansdk.ui.why

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.databinding.GuardianFragmentWhyBinding
import com.autodroid.guardiansdk.ui.why.adapter.WhyAdapter
import com.autodroid.guardiansdk.ui.why.model.WhyItem
import com.autodroid.guardiansdk.ui.why.model.WhyItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.autodroid.guardiansdk.util.EncryptionUtils
import android.util.Base64
import android.widget.Toast
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.repository.SettingRepository

class WhyFragment : Fragment() {

    private var _binding: GuardianFragmentWhyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhyViewModel by viewModels { WhyViewModelFactory(requireContext()) }
    private lateinit var adapter: WhyAdapter
    
    private lateinit var settingRepository: SettingRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GuardianFragmentWhyBinding.inflate(inflater, container, false)
        val database = GuardianDatabase.getDatabase(requireContext())
        settingRepository = SettingRepository(database.settingDao())
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
        adapter.setOnItemClickListener(object : WhyAdapter.OnItemClickListener {
            override fun onShareGuardianClick() {
                shareGuardianInfo()
            }

            override fun onImportWardClick() {
                importWardInfo()
            }
        })
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

    private fun shareGuardianInfo() {
        lifecycleScope.launch {
            try {
                val phoneNumber = withContext(Dispatchers.IO) {
                    settingRepository.getString("my_phone_number", "")
                }
                
                val alarmPassword = withContext(Dispatchers.IO) {
                    settingRepository.getString("secure_book", "")
                }
                
                val emailAddress = withContext(Dispatchers.IO) {
                    settingRepository.getString("email_address", "")
                }
                
                val emailPassword = withContext(Dispatchers.IO) {
                    settingRepository.getString("email_password", "")
                }

                val shareData = mapOf(
                    "phone_number" to phoneNumber,
                    "alarm_password" to alarmPassword,
                    "email_address" to emailAddress,
                    "email_password" to emailPassword
                )

                val jsonData = shareData.entries.joinToString("&") { "${it.key}=${it.value}" }
                val encryptedData = EncryptionUtils.encryptString(jsonData)
                val base64Data = Base64.encodeToString(encryptedData.toByteArray(), Base64.NO_WRAP)

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, base64Data)
                    putExtra(Intent.EXTRA_SUBJECT, "监护人配置信息")
                }

                startActivity(Intent.createChooser(shareIntent, "分享监护人信息"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importWardInfo() {
        showImportWardDialog()
    }

    private fun showImportWardDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.guardian_dialog_import_ward, null)

        val tvSharedData = dialogView.findViewById<TextView>(R.id.tv_ward_shared_data)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_import_message)

        lifecycleScope.launch {
            val sharedData = withContext(Dispatchers.IO) {
                settingRepository.getString("ward_shared_data_temp", "")
            }

            if (sharedData.isEmpty()) {
                tvSharedData.text = "暂无待导入的被监护人信息"
                tvMessage.text = "请让被监护人先分享配置信息，然后通过蓝牙或WiFi接收"
                tvMessage.setTextColor(android.graphics.Color.parseColor("#FF5722"))
            } else {
                tvSharedData.text = sharedData
                tvMessage.text = "已接收到被监护人分享的加密数据，确认导入后将保存到本地数据库"
                tvMessage.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("导入被监护人信息")
            .setView(dialogView)
            .setPositiveButton("确认导入") { dialog, _ ->
                lifecycleScope.launch {
                    val sharedData = withContext(Dispatchers.IO) {
                        settingRepository.getString("ward_shared_data_temp", "")
                    }

                    if (sharedData.isNotEmpty()) {
                        try {
                            val encryptedData = Base64.decode(sharedData, Base64.NO_WRAP)
                            val decryptedJson = EncryptionUtils.decryptString(String(encryptedData))
                            
                            val dataMap = decryptedJson.split("&").associate { entry ->
                                val (key, value) = entry.split("=", limit = 2)
                                key to value
                            }

                            val phoneNumber = dataMap["phone_number"] ?: ""
                            val alarmPassword = dataMap["alarm_password"] ?: ""
                            val emailAddress = dataMap["email_address"] ?: ""
                            val emailPassword = dataMap["email_password"] ?: ""

                            settingRepository.putString("ward_shared_data", sharedData, "被监护人分享数据")

                            val contactDao = GuardianDatabase.getDatabase(requireContext()).contactDao()
                            val existingWard = contactDao.getWards().firstOrNull()

                            if (existingWard != null) {
                                contactDao.insertOrUpdate(
                                    existingWard.copy(
                                        phoneNumber = phoneNumber,
                                        name = "被监护人",
                                        type = com.autodroid.guardiansdk.data.entity.ContactType.WARD
                                    )
                                )
                            } else {
                                contactDao.insertOrUpdate(
                                    com.autodroid.guardiansdk.data.entity.Contact(
                                        phoneNumber = phoneNumber,
                                        name = "被监护人",
                                        type = com.autodroid.guardiansdk.data.entity.ContactType.WARD,
                                        relationship = "被监护人",
                                        isPrimary = false,
                                        orderIndex = 0
                                    )
                                )
                            }

                            Toast.makeText(requireContext(), "成功导入被监护人信息", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "没有可导入的数据", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}