package com.autodroid.teachitback.api

import com.autodroid.teachitback.model.MessageEntity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TencentCloudAIServiceTest {

    @Test
    fun `TencentCloudAIService interface should be defined`() {
        // 这个测试验证接口是否正确定义
        val serviceMethods = TencentCloudAIService::class.java.declaredMethods
        val methodNames = serviceMethods.map { it.name }

        // 验证所有必需的方法都存在
        assertTrue("sendMessage方法必须存在", methodNames.contains("sendMessage"))
        assertTrue("generateMindMap方法必须存在", methodNames.contains("generateMindMap"))
        assertTrue("analyzeLearningProgress方法必须存在", methodNames.contains("analyzeLearningProgress"))
        assertTrue("generateSocraticQuestions方法必须存在", methodNames.contains("generateSocraticQuestions"))
        assertTrue("evaluateAnswer方法必须存在", methodNames.contains("evaluateAnswer"))
        assertTrue("parseDocument方法必须存在", methodNames.contains("parseDocument"))
        assertTrue("extractKeyConcepts方法必须存在", methodNames.contains("extractKeyConcepts"))
        assertTrue("buildKnowledgeGraph方法必须存在", methodNames.contains("buildKnowledgeGraph"))
    }
}
