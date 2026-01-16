package com.autodroid.guardiansdk.util

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 256
    
    // 使用应用特定的密钥（实际应用中应该更安全地存储）
    private val SECRET_KEY = "guardian_sdk_secret_key_2024".toByteArray()
    
    /**
     * 加密文件
     */
    fun encryptFile(file: File): ByteArray {
        val fileData = file.readBytes()
        return encryptData(fileData)
    }
    
    /**
     * 解密文件
     */
    fun decryptFile(encryptedData: ByteArray): ByteArray {
        return decryptData(encryptedData)
    }
    
    /**
     * 加密数据
     */
    private fun encryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(generateKey(), ALGORITHM)
        val iv = generateIV()
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
        
        val encryptedData = cipher.doFinal(data)
        
        // 将IV和加密数据合并
        val result = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)
        
        return result
    }
    
    /**
     * 解密数据
     */
    private fun decryptData(encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(generateKey(), ALGORITHM)
        
        // 提取IV
        val iv = encryptedData.copyOfRange(0, 16)
        val actualData = encryptedData.copyOfRange(16, encryptedData.size)
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
        
        return cipher.doFinal(actualData)
    }
    
    /**
     * 生成加密密钥
     */
    private fun generateKey(): ByteArray {
        // 简单的密钥派生函数，实际应用中应该使用更安全的密钥管理
        val key = ByteArray(KEY_SIZE / 8)
        System.arraycopy(SECRET_KEY, 0, key, 0, minOf(SECRET_KEY.size, key.size))
        return key
    }
    
    /**
     * 生成初始化向量
     */
    private fun generateIV(): ByteArray {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        return iv
    }
    
    /**
     * 加密字符串
     */
    fun encryptString(text: String): String {
        val encrypted = encryptData(text.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }
    
    /**
     * 解密字符串
     */
    fun decryptString(encryptedText: String): String {
        val encryptedData = Base64.decode(encryptedText, Base64.DEFAULT)
        val decrypted = decryptData(encryptedData)
        return String(decrypted, Charsets.UTF_8)
    }
}