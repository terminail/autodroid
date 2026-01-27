package com.autodroid.teachitback.api

import com.autodroid.teachitback.model.MessageEntity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TencentCloudAIServiceTest {

    @Test
    fun `AIService interface should be defined`() {
        val serviceMethods = AIService::class.java.declaredMethods
        val methodNames = serviceMethods.map { it.name }

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
