import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit

data class Message(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int,
    val temperature: Double
)

data class DeepSeekChatResponse(
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val message: Message,
    val index: Int,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

fun main() {
    val apiKey = "sk-5739b781d2bf41d3861fb89f5a4e4ced"
    val baseUrl = "https://api.deepseek.com/v1"
    val model = "deepseek-chat"

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val gson = Gson()

    val testMessage = "What is 2 + 2? Please answer briefly."

    val chatRequest = ChatRequest(
        model = model,
        messages = listOf(Message("user", testMessage)),
        maxTokens = 200,
        temperature = 0.7
    )

    val requestBody = gson.toJson(chatRequest).toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url("$baseUrl/chat/completions")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(requestBody)
        .build()

    println("Testing DeepSeek API...")
    println("Question: $testMessage")
    println("Model: $model")
    println("API Key: ${apiKey.take(10)}...")
    println("-" * 50)

    try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody == null) {
            println("❌ API Request Failed!")
            println("Status Code: ${response.code}")
            println("Response: $responseBody")
            return
        }

        val chatResponse = gson.fromJson(responseBody, DeepSeekChatResponse::class.java)
        val content = chatResponse.choices.firstOrNull()?.message?.content

        println("✅ API Request Successful!")
        println("Response: $content")
        println("-" * 50)
        println("Usage:")
        chatResponse.usage?.let { usage ->
            println("  Prompt Tokens: ${usage.promptTokens}")
            println("  Completion Tokens: ${usage.completionTokens}")
            println("  Total Tokens: ${usage.totalTokens}")
        }
        println("Finish Reason: ${chatResponse.choices.firstOrNull()?.finishReason}")

    } catch (e: Exception) {
        println("❌ Error occurred: ${e.message}")
        e.printStackTrace()
    }
}
