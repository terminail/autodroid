package com.autodroid.teachitback.service

import com.autodroid.teachitback.model.AIServiceError


// ===== 缺失的异常类定义 =====

class ConfigurationException(val error: AIServiceError) : Exception(error.message)

class APIException(val error: AIServiceError) : Exception(error.message)

class NetworkException(val error: AIServiceError) : Exception(error.message)

class ContentProcessingException(val error: AIServiceError) : Exception(error.message)