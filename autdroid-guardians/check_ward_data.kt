import android.content.Context
import android.util.Log
import com.autodroid.guardiansdk.data.database.GuardianDatabase
import com.autodroid.guardiansdk.data.entity.ContactType
import kotlinx.coroutines.runBlocking

/**
 * 检查被监护人数据的简单测试脚本
 */
fun checkWardData(context: Context) {
    Log.d("CheckWardData", "=== 开始检查被监护人数据 ===")
    
    runBlocking {
        try {
            val database = GuardianDatabase.getDatabase(context)
            val contactDao = database.contactDao()
            
            // 检查所有联系人
            val allContacts = contactDao.getContactsByType(ContactType.WARD)
            Log.d("CheckWardData", "=== WARD类型联系人数量: ${allContacts.size} ===")
            
            allContacts.forEach { contact ->
                Log.d("CheckWardData", "=== 被监护人: ${contact.name} (${contact.phoneNumber}) ===")
            }
            
            // 检查活跃联系人
            val activeWards = contactDao.getActiveContactsByType(ContactType.WARD)
            Log.d("CheckWardData", "=== 活跃WARD类型联系人数量: ${activeWards.size} ===")
            
            activeWards.forEach { contact ->
                Log.d("CheckWardData", "=== 活跃被监护人: ${contact.name} (${contact.phoneNumber}) ===")
            }
            
            // 检查GUARDIAN类型联系人
            val guardians = contactDao.getGuardians()
            Log.d("CheckWardData", "=== GUARDIAN类型联系人数量: ${guardians.size} ===")
            
            guardians.forEach { contact ->
                Log.d("CheckWardData", "=== 报警联系人: ${contact.name} (${contact.phoneNumber}) - 索引: ${contact.orderIndex} ===")
            }
            
        } catch (e: Exception) {
            Log.e("CheckWardData", "=== 检查数据时出错 ===", e)
        }
    }
    
    Log.d("CheckWardData", "=== 检查完成 ===")
}