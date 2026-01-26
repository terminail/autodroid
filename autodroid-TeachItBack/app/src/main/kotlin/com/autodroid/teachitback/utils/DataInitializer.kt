package com.autodroid.teachitback.utils

import android.content.Context
import com.autodroid.teachitback.config.PresetTopicCategories
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.SettingEntity
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.model.WhyEntity
import com.autodroid.teachitback.ui.adapter.SettingsItem
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log


class DataInitializer(private val context: Context) {

    private val gson = Gson()

    companion object {
        fun getAIServiceItems(): List<SettingsItem> {
            return listOf(
                SettingsItem.DoubaoAIServiceItem(),
                SettingsItem.ErnieAIServiceItem(),
                SettingsItem.QwenAIServiceItem(),
                SettingsItem.DeepSeekAIServiceItem(),
                SettingsItem.ZhipuAIServiceItem(),
                SettingsItem.SparkAIServiceItem(),
                SettingsItem.MinimaxAIServiceItem(),
                SettingsItem.KimiAIServiceItem(),
                SettingsItem.HunyuanAIServiceItem(),
                SettingsItem.BaichuanAIServiceItem(),
                SettingsItem.LingyiAIServiceItem(),
                SettingsItem.JieyueAIServiceItem()
            )
        }
    }

    fun initializeDemoData() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            
            val existingTopics = database.topicDao().getAllTopics().first()
            
            if (existingTopics.isNotEmpty()) {
                return@launch
            }

            // 初始化主题分类树结构
            initializeTopicTreeNodes()

            initializeAIServices(database)

            // Create preset topics with proper category assignments
            val presetTopics = listOf(
                // ===== CFP财务规划相关 =====
                TopicEntity(
                    title = "CFP财务规划",
                    description = "系统化学习CFP考试内容，掌握核心财务规划概念",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("CFP财务规划"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "投资组合管理",
                    description = "掌握资产配置策略，理解风险与收益平衡",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("投资组合管理"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "税务规划",
                    description = "深入理解税务优化方法，学习税收规划策略",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("税务规划"),
                    masteryLevel = 0,
                    isPreset = true
                ),

                // ===== 高中教育相关 =====
                TopicEntity(
                    title = "高中数学",
                    description = "涵盖函数、几何、代数、概率统计等核心数学知识点",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("高中数学"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中物理",
                    description = "学习力学、电磁学、热学、光学等物理基础知识",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("高中物理"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中化学",
                    description = "掌握元素周期表、化学反应、有机化学等化学核心内容",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("高中化学"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中生物",
                    description = "了解细胞结构、遗传学、生态系统等生物学基础知识",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("高中生物"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中英语",
                    description = "提升听说读写能力，掌握语法、词汇和阅读理解技巧",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("高中英语"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中历史",
                    description = "学习中国历史和世界历史的重要事件和发展脉络",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("高中历史"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中地理",
                    description = "掌握自然地理和人文地理知识，理解地球环境与人类活动",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("高中地理"),
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中政治",
                    description = "学习马克思主义基本原理、中国特色社会主义理论体系",
                    topicCategoryId = PresetTopicCategories.getCategoryForTopic("高中政治"),
                    masteryLevel = 0,
                    isPreset = true
                )
            )

            presetTopics.forEach { topic ->
                database.topicDao().insertTopic(topic)
            }

            val presetMindMapCreator = PresetMindMapCreator(database)
            presetTopics.forEach { topic ->
                android.util.Log.d("DataInitializer", "Creating MindMap for topic: ${topic.id}, title: ${topic.title}")
                val mindMapId = presetMindMapCreator.createPresetCourseMindMap(topic.id, topic.title)
                android.util.Log.d("DataInitializer", "Created MindMap with id: $mindMapId for topic: ${topic.id}")
            }

            // 初始化MindMap演示数据
            initializeMindMapDemoData(database)
        }
    }

    private suspend fun initializeMindMapDemoData(database: AppDatabase) {
        val existingMindMaps = database.mindMapDao().getAllMindMaps()

        if (existingMindMaps.isNotEmpty()) {
            return
        }

        val presetMindMapCreator = PresetMindMapCreator(database)
        presetMindMapCreator.createAllPresetMindMaps()
    }

    /**
     * 初始化主题分类树结构
     * 
     * 当前实现：硬编码的分类结构（PresetTopicCategories）
     * 未来扩展：可以从Git仓库动态下载分类结构
     * 
     * 初始化流程：
     * 1. 加载预设分类配置
     * 2. 验证分类结构的完整性
     * 3. 记录分类信息到日志
     */
    private fun initializeTopicTreeNodes() {
        val allCategories = PresetTopicCategories.getAllCategoriesFlattened()

        Log.d("DataInitializer", "=== 初始化主题分类树结构 ===")
        Log.d("DataInitializer", "分类节点总数: ${allCategories.size}")

        allCategories.forEach { category ->
            val parentInfo = if (category.parentId != null) {
                " (父节点: ${category.parentId})"
            } else {
                " (根节点)"
            }
            Log.d("DataInitializer", "  - [${category.source}] ${category.name} (ID: ${category.id})${parentInfo}")
        }

        // 验证主题分类映射
        Log.d("DataInitializer", "=== 主题到分类的映射 ===")
        PresetTopicCategories.topicCategoryMapping.forEach { (topicTitle, categoryId) ->
            val category = PresetTopicCategories.getCategoryById(categoryId)
            val categoryName = category?.name ?: "未知"
            Log.d("DataInitializer", "  - '$topicTitle' -> '$categoryName' (ID: $categoryId)")
        }

        Log.d("DataInitializer", "主题分类树结构初始化完成")
    }

    private suspend fun initializeAIServices(database: AppDatabase) {
        val existingSettings = database.settingDao().getAllSettings().first()
        
        if (existingSettings.isNotEmpty()) {
            return
        }
        
        val aiServiceItems = getAIServiceItems()
        
        aiServiceItems.forEach { settingsItem ->
            val id = when (settingsItem) {
                is SettingsItem.DoubaoAIServiceItem -> settingsItem.id
                is SettingsItem.ErnieAIServiceItem -> settingsItem.id
                is SettingsItem.QwenAIServiceItem -> settingsItem.id
                is SettingsItem.DeepSeekAIServiceItem -> settingsItem.id
                is SettingsItem.ZhipuAIServiceItem -> settingsItem.id
                is SettingsItem.SparkAIServiceItem -> settingsItem.id
                is SettingsItem.MinimaxAIServiceItem -> settingsItem.id
                is SettingsItem.KimiAIServiceItem -> settingsItem.id
                is SettingsItem.HunyuanAIServiceItem -> settingsItem.id
                is SettingsItem.BaichuanAIServiceItem -> settingsItem.id
                is SettingsItem.LingyiAIServiceItem -> settingsItem.id
                is SettingsItem.JieyueAIServiceItem -> settingsItem.id
                else -> return@forEach
            }
            
            val json = serializeSettingsItem(settingsItem)
            val entity = SettingEntity(
                key = "ai_$id",
                value = json,
                lastUpdated = System.currentTimeMillis()
            )
            database.settingDao().insertSetting(entity)
        }
        
        initializeWhyFragmentContent(database)
    }
    
    private suspend fun initializeWhyFragmentContent(database: AppDatabase) {
        val existingWhyContent = database.whyDao().getAllWhyContent().first()
        
        if (existingWhyContent.isNotEmpty()) {
            return
        }
        
        val whyContents = listOf(
            WhyEntity(
                id = "app_intro",
                type = "app_intro_card",
                title = "Teach It Back - 深度学习的智能伴侣",
                content = """
                    以目标为导向，智能分解可掌握的小目标，结合苏格拉底教学法和费曼学习法的AI驱动学习应用，帮助您通过'教回去'的方式真正掌握知识。
                    
                    学会的最高境界是教会别人
                    
                    主要特性：
                    • 目标驱动学习：AI智能分解学习目标为可执行路径
                    • 循循善诱：苏格拉底式提问引导深度思考
                    • 主动教学：费曼技巧强化知识掌握
                    • 可视化进度：MindMap实时跟踪学习进展
                """.trimIndent(),
                orderIndex = 1
            ),
            WhyEntity(
                id = "application_value",
                type = "application_value_card",
                title = "应用价值",
                content = """
                    基于CFP-Study的真实学习过程
                    
                    Teach It Back通过模拟真实教学场景，帮助您深入理解复杂概念。基于CFP-Study的实际学习经验，本应用能够：
                    
                    • 识别知识盲点，针对性强化学习
                    • 通过教学反馈，提升表达和逻辑能力
                    • 建立系统知识框架，避免碎片化学习
                    • 跟踪学习进度，量化掌握程度
                """.trimIndent(),
                orderIndex = 2
            ),
            WhyEntity(
                id = "real_results",
                type = "real_results_card",
                title = "真实学习成果",
                content = """
                    基于CFP-Study的实际应用效果
                    
                    在CFP考试准备过程中，Teach It Back帮助用户：
                    
                    • 1031交换：从困惑到掌握平衡等式框架
                    • IRA规则：清晰区分贡献资格与抵扣性
                    • 投资风险：理解R平方与系统性风险关系
                    • 学习进度：82%知识点掌握，3天倒计时
                    
                    通过苏格拉底式提问和费曼解释方法，用户能够：
                    
                    • 识别并解决概念混淆点
                    • 建立系统化的知识框架
                    • 提升表达和解释能力
                    
                    "Teach It Back让我真正理解了复杂的财务规划概念。通过向AI解释，我发现了很多自己以为懂但实际模糊的地方。"
                """.trimIndent(),
                orderIndex = 3
            ),
            WhyEntity(
                id = "how_to_use",
                type = "how_to_use_card",
                title = "如何使用",
                content = """
                    1. 在「主题」页面创建学习主题
                       输入您想要掌握的知识领域，AI会分析您的学习需求
                    
                    2. 选择主题进入对话页面
                       设置自己的学习目标，AI会将你的目标分解为可管理的小目标，形成可视化学习路径
                    
                    3. 向AI讲解你的知识
                       通过苏格拉底式对话和费曼教学法，在舒适的环境中深入学习
                    
                    4. AI提问并给出反馈
                       MindMap实时更新学习进度，AI根据您的表现调整学习路径
                    
                    5. 持续加深理解，提升掌握度
                       循环学习，直到完全掌握
                """.trimIndent(),
                orderIndex = 4
            ),
            WhyEntity(
                id = "features",
                type = "features_card",
                title = "主要功能",
                content = """
                    • 多主题管理
                    • AI 智能提问
                    • 学习进度跟踪
                    • 语音输入支持
                    • 文件内容分析
                    • 思维导图可视化
                """.trimIndent(),
                orderIndex = 6
            ),
            WhyEntity(
                id = "learning_philosophy",
                type = "learning_philosophy_card",
                title = "学习哲学：苏格拉底×费曼的完美结合",
                content = """
                    苏格拉底方法：深度提问引导思考
                    AI通过精心设计的提问，帮助您发现知识盲点，培养批判性思维能力。
                    
                    费曼技巧：通过教学巩固知识
                    '如果您不能简单地解释一个概念，说明您还没有真正理解它。' - 通过向AI解释，检验您的理解深度。
                    
                    MindMap可视化：学习路径一目了然
                    AI将复杂知识分解为树状结构，实时显示学习进度，让您清晰看到自己的进步。
                """.trimIndent(),
                orderIndex = 7
            ),
            WhyEntity(
                id = "target_audience",
                type = "target_audience_card",
                title = "适合人群",
                content = """
                    学生群体
                    备考各类考试，系统化掌握复杂概念
                    
                    职场人士
                    快速掌握专业技能，提升工作效率
                    
                    终身学习者
                    持续学习新知识，保持思维活跃
                """.trimIndent(),
                orderIndex = 8
            )
        )
        
        database.whyDao().insertAllWhyContent(whyContents)
    }

    /**
     * 序列化 SettingsItem 为 JSON
     */
    private fun serializeSettingsItem(item: SettingsItem): String {
        val type = when (item) {
            is SettingsItem.DoubaoAIServiceItem -> "DoubaoAIServiceItem"
            is SettingsItem.ErnieAIServiceItem -> "ErnieAIServiceItem"
            is SettingsItem.QwenAIServiceItem -> "QwenAIServiceItem"
            is SettingsItem.DeepSeekAIServiceItem -> "DeepSeekAIServiceItem"
            is SettingsItem.ZhipuAIServiceItem -> "ZhipuAIServiceItem"
            is SettingsItem.SparkAIServiceItem -> "SparkAIServiceItem"
            is SettingsItem.MinimaxAIServiceItem -> "MinimaxAIServiceItem"
            is SettingsItem.KimiAIServiceItem -> "KimiAIServiceItem"
            is SettingsItem.HunyuanAIServiceItem -> "HunyuanAIServiceItem"
            is SettingsItem.BaichuanAIServiceItem -> "BaichuanAIServiceItem"
            is SettingsItem.LingyiAIServiceItem -> "LingyiAIServiceItem"
            is SettingsItem.JieyueAIServiceItem -> "JieyueAIServiceItem"
            else -> return ""
        }
        
        val itemJson = gson.toJson(item)
        return """{"type":"$type","data":$itemJson}"""
    }
}
