package com.autodroid.guardiansdk.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 密码本数据模型
 * 用于加密位置信息和报警信息
 */
@Parcelize
data class SecureBook(
    val digit0: String = "零",      // 数字 0
    val digit1: String = "一",      // 数字 1
    val digit2: String = "二",      // 数字 2
    val digit3: String = "三",      // 数字 3
    val digit4: String = "四",      // 数字 4
    val digit5: String = "五",      // 数字 5
    val digit6: String = "六",      // 数字 6
    val digit7: String = "七",      // 数字 7
    val digit8: String = "八",      // 数字 8
    val digit9: String = "九",      // 数字 9
    val comma: String = "逗",       // 逗号 ,
    val alarm0: String = "去",  // 普通报警信息
    val alarm1: String = "肯",    // 紧急报警信息
    val reserve0: String = "空",      // 预留0
    val reserve1: String = "白",      // 预留1
    val reserve2: String = "佛"       // 预留2
) : Parcelable {

    /**
     * 获取数字映射
     */
    fun getDigitMap(): Map<Int, String> {
        return mapOf(
            0 to digit0,
            1 to digit1,
            2 to digit2,
            3 to digit3,
            4 to digit4,
            5 to digit5,
            6 to digit6,
            7 to digit7,
            8 to digit8,
            9 to digit9
        )
    }

    /**
     * 获取所有字符列表（用于检查重复）
     */
    fun getAllCharacters(): List<String> {
        return listOf(
            digit0, digit1, digit2, digit3,
            digit4, digit5, digit6, digit7,
            digit8, digit9, comma,
            alarm0, alarm1, reserve0, reserve1, reserve2
        )
    }

    /**
     * 验证密码本格式是否正确
     */
    fun isValid(): Boolean {
        val allChars = getAllCharacters()
        
        // 检查是否有空值
        if (allChars.any { it.isBlank() }) {
            return false
        }
        
        // 检查16个字是否都不重复
        if (allChars.size != allChars.toSet().size) {
            return false
        }
        
        return true
    }

    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return """
            {
                "digit0":"$digit0",
                "digit1":"$digit1",
                "digit2":"$digit2",
                "digit3":"$digit3",
                "digit4":"$digit4",
                "digit5":"$digit5",
                "digit6":"$digit6",
                "digit7":"$digit7",
                "digit8":"$digit8",
                "digit9":"$digit9",
                "comma":"$comma",
                "alarm0":"$alarm0",
                "alarm1":"$alarm1",
                "reserve0":"$reserve0",
                "reserve1":"$reserve1",
                "reserve2":"$reserve2"
            }
        """.trimIndent()
    }

    companion object {
        /**
         * 从 JSON 字符串创建 SecureBook
         */
        fun fromJson(json: String): SecureBook? {
            return try {
                // 简单的 JSON 解析（实际项目中可使用 Gson/Moshi）
                val digit0 = extractValue(json, "digit0")
                val digit1 = extractValue(json, "digit1")
                val digit2 = extractValue(json, "digit2")
                val digit3 = extractValue(json, "digit3")
                val digit4 = extractValue(json, "digit4")
                val digit5 = extractValue(json, "digit5")
                val digit6 = extractValue(json, "digit6")
                val digit7 = extractValue(json, "digit7")
                val digit8 = extractValue(json, "digit8")
                val digit9 = extractValue(json, "digit9")
                val comma = extractValue(json, "comma")
                val alarm0 = extractValue(json, "alarm0")
                val alarm1 = extractValue(json, "alarm1")
                val reserve0 = extractValue(json, "reserve0")
                val reserve1 = extractValue(json, "reserve1")
                val reserve2 = extractValue(json, "reserve2")

                SecureBook(
                    digit0, digit1, digit2, digit3,
                    digit4, digit5, digit6, digit7,
                    digit8, digit9, comma,
                    alarm0, alarm1,
                    reserve0, reserve1, reserve2
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun extractValue(json: String, key: String): String {
            val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            val match = pattern.find(json)
            return match?.groupValues?.get(1) ?: ""
        }

        /**
         * 获取默认密码本
         */
        fun getDefault(): SecureBook {
            return SecureBook(
                digit0 = "零",
                digit1 = "一",
                digit2 = "二",
                digit3 = "三",
                digit4 = "四",
                digit5 = "五",
                digit6 = "六",
                digit7 = "七",
                digit8 = "八",
                digit9 = "九",
                comma = "逗",
                alarm0 = "去",
                alarm1 = "肯",
                reserve0 = "空",
                reserve1 = "白",
                reserve2 = "佛"
            )
        }
    }
}
