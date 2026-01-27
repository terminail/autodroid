package com.autodroid.teachitback.framework

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * BERT 分词器
 * 用于将文本转换为 token IDs
 */
class BertTokenizer private constructor(
    private val vocab: Map<String, Int>,
    private val maxSeqLength: Int = 512
) {
    companion object {
        private const val TAG = "BertTokenizer"
        
        // 特殊标记
        const val CLS_TOKEN = "[CLS]"
        const val SEP_TOKEN = "[SEP]"
        const val PAD_TOKEN = "[PAD]"
        const val UNK_TOKEN = "[UNK]"
        
        // 特殊标记的 token ID（根据 BERT 标准）
        private const val CLS_TOKEN_ID = 101
        private const val SEP_TOKEN_ID = 102
        private const val PAD_TOKEN_ID = 0
        private const val UNK_TOKEN_ID = 100
        
        /**
         * 从文件加载分词器
         */
        fun fromFile(context: Context, vocabFilePath: String, maxSeqLength: Int = 512): BertTokenizer? {
            Log.d(TAG, "加载词汇表文件: $vocabFilePath")
            
            val vocab = mutableMapOf<String, Int>()
            
            try {
                var inputStream: java.io.InputStream? = null
                
                // 首先检查是否是绝对路径
                val file = java.io.File(vocabFilePath)
                if (file.exists() && file.canRead()) {
                    Log.d(TAG, "从绝对路径加载: $vocabFilePath")
                    inputStream = java.io.FileInputStream(file)
                } else {
                    // 尝试从 assets 加载
                    try {
                        Log.d(TAG, "尝试从 assets 加载: $vocabFilePath")
                        inputStream = context.assets.open(vocabFilePath)
                    } catch (e: Exception) {
                        Log.d(TAG, "无法从 assets 加载: ${e.message}")
                        
                        // 尝试从应用的 files 目录加载
                        val filesDirFile = java.io.File(context.filesDir, vocabFilePath)
                        if (filesDirFile.exists() && filesDirFile.canRead()) {
                            Log.d(TAG, "从 files 目录加载: ${filesDirFile.absolutePath}")
                            inputStream = java.io.FileInputStream(filesDirFile)
                        } else {
                            // 尝试从外部存储加载
                            val externalFile = java.io.File("/sdcard/$vocabFilePath")
                            if (externalFile.exists() && externalFile.canRead()) {
                                Log.d(TAG, "从 sdcard 加载: ${externalFile.absolutePath}")
                                inputStream = java.io.FileInputStream(externalFile)
                            } else {
                                Log.e(TAG, "无法找到词汇表文件: $vocabFilePath")
                                return null
                            }
                        }
                    }
                }
                
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                var index = 0
                
                while (reader.readLine().also { line = it } != null) {
                    vocab[line!!.trim()] = index++
                }
                
                reader.close()
                inputStream?.close()
                
                Log.d(TAG, "词汇表加载完成，共 ${vocab.size} 个 token")
                Log.d(TAG, "特殊标记: CLS=$CLS_TOKEN_ID, SEP=$SEP_TOKEN_ID, PAD=$PAD_TOKEN_ID, UNK=$UNK_TOKEN_ID")
                
                return BertTokenizer(vocab, maxSeqLength)
            } catch (e: Exception) {
                Log.e(TAG, "加载词汇表失败", e)
                return null
            }
        }
    }
    
    /**
     * 将文本转换为 token IDs
     */
    fun tokenize(text: String): List<Int> {
        Log.d(TAG, "tokenize: 输入文本='$text', 长度=${text.length}")
        Log.d(TAG, "tokenize: 前3个字符: ${text.take(3).map { "U+${it.code.toString(16).uppercase()}" }}")
        
        val tokens = mutableListOf<Int>()
        
        // 添加 [CLS] 标记
        tokens.add(CLS_TOKEN_ID)
        
        // 分词
        val words = splitText(text)
        Log.d(TAG, "splitText 结果: $words")
        
        // 转换为 token IDs
        for (word in words) {
            val tokenId = vocab[word.lowercase()] ?: UNK_TOKEN_ID
            Log.d(TAG, "  词: '$word' -> ID: $tokenId")
            tokens.add(tokenId)
        }
        
        // 添加 [SEP] 标记
        tokens.add(SEP_TOKEN_ID)
        
        // 截断或 padding
        return padOrTruncate(tokens)
    }
    
    /**
     * 创建 attention mask
     */
    fun createAttentionMask(tokenIds: List<Int>): List<Int> {
        return tokenIds.map { if (it == PAD_TOKEN_ID) 0 else 1 }
    }
    
    /**
     * 创建 token type IDs
     */
    fun createTokenTypeIds(tokenIds: List<Int>): List<Int> {
        return tokenIds.map { 0 }
    }
    
    /**
     * 分割文本
     * 简单实现：按空格和标点符号分割
     * 对于中文，每个字符作为一个 token
     */
    private fun splitText(text: String): List<String> {
        val words = mutableListOf<String>()
        var currentWord = StringBuilder()
        
        for (char in text) {
            if (char.isWhitespace()) {
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord.toString())
                    currentWord.clear()
                }
            } else if (isChinese(char)) {
                // 中文字符单独作为一个 token
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord.toString())
                    currentWord.clear()
                }
                words.add(char.toString())
            } else if (isPunctuation(char)) {
                // 标点符号单独作为一个 token
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord.toString())
                    currentWord.clear()
                }
                words.add(char.toString())
            } else {
                currentWord.append(char)
            }
        }
        
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }
        
        return words
    }
    
    /**
     * 判断是否为中文字符
     */
    private fun isChinese(char: Char): Boolean {
        val codePoint = char.code
        return codePoint in 0x4E00..0x9FFF ||
               codePoint in 0x3400..0x4DBF ||
               codePoint in 0x20000..0x2A6DF ||
               codePoint in 0x2A700..0x2B73F ||
               codePoint in 0x2B740..0x2B81F ||
               codePoint in 0x2B820..0x2CEAF ||
               codePoint in 0xF900..0xFAFF ||
               codePoint in 0x2F800..0x2FA1F
    }
    
    /**
     * 判断是否为标点符号
     */
    private fun isPunctuation(char: Char): Boolean {
        return char in ".,!?;:\"'()[]{}<>-_=+*/&^%$#@~`|\\"
    }
    
    /**
     * 截断或 padding 到固定长度
     */
    private fun padOrTruncate(tokens: List<Int>): List<Int> {
        return if (tokens.size > maxSeqLength) {
            tokens.take(maxSeqLength)
        } else {
            tokens + List(maxSeqLength - tokens.size) { PAD_TOKEN_ID }
        }
    }
    
    /**
     * 将 token IDs 转换为文本
     */
    fun decode(tokenIds: List<Int>): String {
        val words = mutableListOf<String>()
        
        for (tokenId in tokenIds) {
            // 跳过特殊标记
            if (tokenId == CLS_TOKEN_ID || tokenId == SEP_TOKEN_ID || tokenId == PAD_TOKEN_ID) {
                continue
            }
            
            // 查找 token
            val word = vocab.entries.find { it.value == tokenId }?.key
            if (word != null && word != UNK_TOKEN) {
                words.add(word)
            }
        }
        
        return words.joinToString(" ")
    }
}
