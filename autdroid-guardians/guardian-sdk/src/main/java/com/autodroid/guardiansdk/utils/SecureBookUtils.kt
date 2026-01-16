package com.autodroid.guardiansdk.utils

import com.autodroid.guardiansdk.data.model.SecureBook
import java.util.*

/**
 * 密码本工具类
 * 用于生成、验证、编码和解码密码本
 */
object SecureBookUtils {

    private val COMMON_CHINESE_CHARS = listOf(
        "零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
        "百", "千", "万", "亿", "兆", "吉", "太", "拍", "艾",
        "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸",
        "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥",
        "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪",
        "东", "南", "西", "北", "中", "上", "下", "左", "右", "前", "后",
        "春", "夏", "秋", "冬", "朝", "夕", "日", "月", "星", "云",
        "金", "木", "水", "火", "土", "风", "雷", "电", "冰", "霜",
        "红", "橙", "黄", "绿", "蓝", "紫", "白", "黑", "灰", "粉",
        "大", "小", "多", "少", "新", "旧", "高", "低", "快", "慢",
        "好", "坏", "真", "假", "对", "错", "是", "非", "有", "无",
        "天", "地", "人", "山", "河", "海", "湖", "草", "树", "花",
        "吃", "喝", "玩", "乐", "学", "工", "睡", "醒", "走", "跑"
    )

    /**
     * 生成随机密码本
     */
    fun generateRandomSecureBook(): SecureBook {
        // 从常用字符池中随机选取16个不重复的字符
        val shuffled = COMMON_CHINESE_CHARS.shuffled()
        val randomChars = shuffled.take(16)

        return SecureBook(
            digit0 = randomChars[0],
            digit1 = randomChars[1],
            digit2 = randomChars[2],
            digit3 = randomChars[3],
            digit4 = randomChars[4],
            digit5 = randomChars[5],
            digit6 = randomChars[6],
            digit7 = randomChars[7],
            digit8 = randomChars[8],
            digit9 = randomChars[9],
            comma = randomChars[10],
            alarm0 = randomChars[11],
            alarm1 = randomChars[12],
            reserve0 = randomChars[13],
            reserve1 = randomChars[14],
            reserve2 = randomChars[15]
        )
    }

    /**
     * 编码密码本为JSON字符串
     */
    fun encodeSecureBook(secureBook: SecureBook): String {
        return secureBook.toJson()
    }

    /**
     * 解码JSON字符串为密码本
     */
    fun decodeSecureBook(json: String): SecureBook {
        return SecureBook.fromJson(json) ?: SecureBook.getDefault()
    }

    /**
     * 将位置字符串转换为加密字符串
     * 例如: "31.2304,121.4737" -> "三一逗二三四零逗一二一逗四七三七"
     */
    fun encryptLocation(location: String, secureBook: SecureBook): String {
        val digitMap = secureBook.getDigitMap()
        val result = StringBuilder()

        for (char in location) {
            when (char) {
                in '0'..'9' -> result.append(digitMap[char.toString().toInt()])
                ',' -> result.append(secureBook.comma)
                '.' -> result.append(secureBook.comma) // 小数点也用逗号表示
                else -> result.append(char) // 保留其他字符
            }
        }

        return result.toString()
    }

    /**
     * 生成报警信息（包含加密后的位置坐标）
     * @param alarmType 报警类型：0=普通报警，1=紧急报警
     * @param latitude 纬度
     * @param longitude 经度
     * @param secureBook 密码本
     */
    fun generateAlarmMessage(
        alarmType: Int,
        latitude: Double,
        longitude: Double,
        secureBook: SecureBook
    ): String {
        val alarmText = if (alarmType == 0) secureBook.alarm0 else secureBook.alarm1
        
        // 格式化坐标，保留6位小数
        val latStr = String.format(Locale.getDefault(), "%.6f", latitude)
        val lngStr = String.format(Locale.getDefault(), "%.6f", longitude)
        val coordinates = "$latStr,$lngStr"
        
        // 加密坐标
        val encryptedLocation = encryptLocation(coordinates, secureBook)
        
        return "$alarmText[$encryptedLocation]"
    }

    /**
     * 验证密码本格式是否正确
     */
    fun validateSecureBook(secureBook: SecureBook): Boolean {
        return secureBook.isValid()
    }

    /**
     * 生成密码本分享文本
     */
    fun generateShareText(secureBook: SecureBook): String {
        val sb = StringBuilder()
        sb.append("【密码本分享】\n")
        sb.append("==================\n")
        sb.append("0: ${secureBook.digit0}\n")
        sb.append("1: ${secureBook.digit1}\n")
        sb.append("2: ${secureBook.digit2}\n")
        sb.append("3: ${secureBook.digit3}\n")
        sb.append("4: ${secureBook.digit4}\n")
        sb.append("5: ${secureBook.digit5}\n")
        sb.append("6: ${secureBook.digit6}\n")
        sb.append("7: ${secureBook.digit7}\n")
        sb.append("8: ${secureBook.digit8}\n")
        sb.append("9: ${secureBook.digit9}\n")
        sb.append(",: ${secureBook.comma}\n")
        sb.append("\n")
        sb.append("普通报警: ${secureBook.alarm0}\n")
        sb.append("紧急报警: ${secureBook.alarm1}\n")
        sb.append("==================\n")
        return sb.toString()
    }

    /**
     * 从分享文本解析密码本
     */
    fun parseShareText(shareText: String): SecureBook? {
        return try {
            val lines = shareText.lines()
            val map = mutableMapOf<String, String>()
            
            for (line in lines) {
                if (line.contains(":")) {
                    val parts = line.split(":")
                    if (parts.size >= 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        map[key] = value
                    }
                }
            }
            
            SecureBook(
                digit0 = map["0"] ?: "",
                digit1 = map["1"] ?: "",
                digit2 = map["2"] ?: "",
                digit3 = map["3"] ?: "",
                digit4 = map["4"] ?: "",
                digit5 = map["5"] ?: "",
                digit6 = map["6"] ?: "",
                digit7 = map["7"] ?: "",
                digit8 = map["8"] ?: "",
                digit9 = map["9"] ?: "",
                comma = map[","] ?: "",
                alarm0 = map["普通报警"] ?: "准备去麦当劳",
                alarm1 = map["紧急报警"] ?: "准备去肯德基"
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查分享文本是否是密码本分享
     */
    fun isSecureBookShareText(shareText: String): Boolean {
        return shareText.contains("【密码本分享】") &&
               shareText.contains("普通报警:") &&
               shareText.contains("紧急报警:")
    }
}
