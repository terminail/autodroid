import java.net.URL
import java.net.HttpURLConnection

fun main() {
    println("Testing connection to Appium server...")
    
    try {
        val url = URL("http://127.0.0.1:6790/wd/hub/status")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "GET"
        
        val responseCode = connection.responseCode
        println("Response code: $responseCode")
        
        if (responseCode == 200) {
            val inputStream = connection.inputStream
            val content = inputStream.bufferedReader().use { it.readText() }
            println("Response content: $content")
        }
        
        connection.disconnect()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
    
    // Test IPv6
    println("\nTesting IPv6 connection...")
    try {
        val url = URL("http://[::1]:6790/wd/hub/status")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "GET"
        
        val responseCode = connection.responseCode
        println("IPv6 Response code: $responseCode")
        
        connection.disconnect()
    } catch (e: Exception) {
        println("IPv6 Error: ${e.message}")
    }
}