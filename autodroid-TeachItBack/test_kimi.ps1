$apiKey = "sk-sCe8gzn7awcucLLuI1vGAVfNVbQmrukBm4zXepWjJavR8v6C"
$baseUrl = "https://api.moonshot.cn/v1"

Write-Host "Testing Kimi K2 API..."
Write-Host "API Key: $($apiKey.Substring(0,10))..."
Write-Host "URL: $baseUrl"
Write-Host "-" * 50

$body = @{
    model = "moonshot-v1-8k"
    messages = @(
        @{
            role = "user"
            content = "What is 2 + 2? Please answer briefly."
        }
    )
    max_tokens = 200
    temperature = 0.7
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $apiKey"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/chat/completions" -Method Post -Body $body -Headers $headers
    
    Write-Host "✅ API Request Successful!"
    Write-Host "Response: $($response.choices[0].message.content)"
    Write-Host "-" * 50
    Write-Host "Usage:"
    Write-Host "  Prompt Tokens: $($response.usage.prompt_tokens)"
    Write-Host "  Completion Tokens: $($response.usage.completion_tokens)"
    Write-Host "  Total Tokens: $($response.usage.total_tokens)"
    Write-Host "Finish Reason: $($response.choices[0].finish_reason)"
} catch {
    Write-Host "❌ Error occurred: $($_.Exception.Message)"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Response: $($_.ErrorDetails.Message)"
}
