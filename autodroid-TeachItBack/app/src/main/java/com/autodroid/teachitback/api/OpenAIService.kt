package com.autodroid.teachitback.api

import com.autodroid.teachitback.model.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIService(private val apiKey: String, private val model: String = "gpt-3.5-turbo") : AIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun sendMessage(messages: List<MessageEntity>, context: String): String =
        withContext(Dispatchers.IO) {
            val requestBody = createChatRequestBody(messages, context)
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("OpenAI API error: ${response.code} ${response.message}")
            }

            parseResponse(response)
        }

    override suspend fun processFileContent(content: String, context: String): String =
        withContext(Dispatchers.IO) {
            val requestBody = createProcessFileRequestBody(content, context)
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("OpenAI API error: ${response.code} ${response.message}")
            }

            parseResponse(response)
        }

    private fun createChatRequestBody(messages: List<MessageEntity>, context: String): okhttp3.RequestBody {
        val json = JSONObject().apply {
            put("model", model)

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", """
                    你是一个帮助学生学习的AI助手。使用"教给别人"（Teach It Back）的方法。
                    
                    上下文信息：
                    $context
                    
                    你的任务：
                    1. 引导学生用自己的话解释概念
                    2. 提供反馈和澄清
                    3. 识别理解中的漏洞
                    4. 鼓励深度思考
                    
                    请用简洁、友好的方式回应。
                """.trimIndent())
            })

            messages.forEach { message ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (message.senderType == "USER") "user" else "assistant")
                    put("content", message.content)
                })
            }

            put("messages", messagesArray)
            put("max_tokens", 500)
            put("temperature", 0.7)
        }

        return json.toString().toRequestBody(jsonMediaType)
    }

    private fun createProcessFileRequestBody(content: String, context: String): okhttp3.RequestBody {
        val json = JSONObject().apply {
            put("model", model)

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", """
                    你是一个帮助学生分析学习材料的AI助手。
                    
                    上下文信息：
                    $context
                    
                    以下是学生提供的文档内容，请：
                    1. 总结主要内容
                    2. 识别关键概念
                    3. 提出学习建议
                    4. 生成相关的测试问题
                """.trimIndent())
            })
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", "请分析以下文档内容：\n\n$content")
            })

            put("messages", messagesArray)
            put("max_tokens", 1000)
            put("temperature", 0.5)
        }

        return json.toString().toRequestBody(jsonMediaType)
    }

    private fun parseResponse(response: okhttp3.Response): String {
        val responseBody = response.body?.string() ?: return "Empty response"

        return try {
            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                choice.getJSONObject("message").getString("content")
            } else {
                "No response generated"
            }
        } catch (e: Exception) {
            "Error parsing response: ${e.message}"
        }
    }
}
