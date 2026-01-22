package com.autodroid.teachitback.utils

import android.content.Context
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.database.SettingDao
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.model.SettingEntity
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.model.WhyEntity
import com.autodroid.teachitback.ui.adapter.SettingsItem
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import java.io.Serializable



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
            
            initializeAIServices(database)

            // Create demo topics based on real CFP Study progress
            val demoTopics = listOf(
                TopicEntity(
                    title = "CFP Exam Study Tracker",
                    description = "CFP考试准备 - 82%进度，3天倒计时",
                    masteryLevel = 82,
                    lastAccessed = System.currentTimeMillis() - 3600000
                ),
                TopicEntity(
                    title = "1031 Exchange - Boot计算",
                    description = "平衡等式框架，FMV vs 成本基础",
                    masteryLevel = 95,
                    lastAccessed = System.currentTimeMillis() - 86400000
                ),
                TopicEntity(
                    title = "Traditional IRA贡献规则",
                    description = "Earned income vs pension income区别",
                    masteryLevel = 90,
                    lastAccessed = System.currentTimeMillis() - 172800000
                ),
                TopicEntity(
                    title = "投资风险与相关性",
                    description = "R平方、系统性与非系统性风险",
                    masteryLevel = 88,
                    lastAccessed = System.currentTimeMillis() - 259200000
                ),
                TopicEntity(
                    title = "退休计划分类",
                    description = "Defined Contribution vs Defined Benefit",
                    masteryLevel = 85,
                    lastAccessed = System.currentTimeMillis() - 345600000
                )
            )

            demoTopics.forEach { topic ->
                database.topicDao().insertTopic(topic)
            }

            // Create preset topics for WhyFragment
            val presetTopics = listOf(
                TopicEntity(
                    title = "CFP财务规划",
                    description = "系统化学习CFP考试内容，掌握核心财务规划概念",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "投资组合管理",
                    description = "掌握资产配置策略，理解风险与收益平衡",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "税务规划",
                    description = "深入理解税务优化方法，学习税收规划策略",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中数学",
                    description = "涵盖函数、几何、代数、概率统计等核心数学知识点",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中物理",
                    description = "学习力学、电磁学、热学、光学等物理基础知识",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中化学",
                    description = "掌握元素周期表、化学反应、有机化学等化学核心内容",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中生物",
                    description = "了解细胞结构、遗传学、生态系统等生物学基础知识",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中英语",
                    description = "提升听说读写能力，掌握语法、词汇和阅读理解技巧",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中历史",
                    description = "学习中国历史和世界历史的重要事件和发展脉络",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中地理",
                    description = "掌握自然地理和人文地理知识，理解地球环境与人类活动",
                    masteryLevel = 0,
                    isPreset = true
                ),
                TopicEntity(
                    title = "高中政治",
                    description = "学习马克思主义基本原理、中国特色社会主义理论体系",
                    masteryLevel = 0,
                    isPreset = true
                )
            )

            presetTopics.forEach { topic ->
                database.topicDao().insertTopic(topic)
            }

            // Add real learning sessions from CFP-Study directory
            val trackerTopic = demoTopics[0]
            val baseTime = System.currentTimeMillis()
            
            // Session from 2025-10-18: 1031 Exchange breakthrough
            val session1Messages = listOf(
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "关于1031同类交换，我搞混了。当你说'total value'时，是指FMV吗？需要减去成本基础吗？需要减去抵押贷款吗？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 600000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "理解你的困惑！我们用一个平衡等式来想：\n\n**放弃的 = 获得的**\n\n左边（放弃）：旧房产FMV + 你支付的现金 + 你承担的新债务\n右边（获得）：新房产FMV + 你收到的现金 + 你被免除的旧债务\n\n当你两边平衡但你收到了现金/债务减免 → 那就是 BOOT！这样想清楚吗？",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 590000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "是的，这样讲清楚了！那怎么计算boot呢？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 580000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "Boot = 收到的现金 + 未被替换的债务减免\n\n试试Sarah的例子：\n- 旧房产：FMV $1.5M，基础$800k，抵押$600k\n- 新房产：FMV $1.2M，新抵押$500k，收到现金$200k\n\nBoot是多少？",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 570000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "boot = $200k + ($600k - $500k) = $300k",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 560000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "完美！你立即正确应用了公式。债务减免是$600k-$500k=$100k，加上现金$200k，总共$300k boot。\n\n现在新成本基础是多少？旧基础是$800k。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 550000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "新基础 = $800k - $300k = $500k",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 540000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "差一点！你忘记加回已确认的收益。公式是：\n**新基础 = 旧基础 - Boot + 已确认收益**\n\n在这个例子中，Boot = 已确认收益 = $300k。所以新基础 = $800k - $300k + $300k = **$800k**。\n\n你想到了为什么吗？Boot和收益在公式中抵消了！",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 530000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "噢！所以已确认收益 = boot？或者我搞混了？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 520000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "这是非常好的问题！是的，通常情况下 Boot = 已确认收益！\n\n逻辑是：1) 你减去boot因为你收到了价值（现金+债务减免）\n2) 但你为这$300k收益付了税\n3) 所以你把它加回——它现在是你新房产'成本'的一部分\n4) 你不应该再次对它征税\n\n当Boot = 已确认收益时（典型情况），新基础 = 旧基础。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 510000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "现在完全明白了！这个平衡等式的框架让1031交换变得直观多了。",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 500000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "太棒了！这就是费曼方法的核心——用简单的框架解释复杂概念。现在你能用自己的话解释1031交换给一个初学者吗？",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000 - 490000
                )
            )

            session1Messages.forEach { message ->
                database.messageDao().insertMessage(message)
            }

            // Session from 2025-10-11: Traditional IRA rules
            val session2Messages = listOf(
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "关于 Sharon 的问题：她58岁，单身，退休2年，每月收到$600养老金。新工作在CPA事务所，没有养老金计划，年收入$5,000。问她2024年传统IRA的最大可抵扣贡献是多少？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 600000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "好问题！首先，你想了解的关键点是什么？你觉得养老金收入会影响IRA贡献吗？",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 590000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "我不确定。我选了B) $0，因为以为如果有养老金就不能贡献IRA。",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 580000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "理解你的困惑。关键区别是：贡献IRA只需要 earned income（劳动收入），而抵扣贡献才受active participant影响。养老金不是earned income，所以Sharon的earned income是$5,000。2024年限额是$7,000（50岁以上+$1,000）。所以最大贡献是$5,000和$8,000中的较小值，即$5,000。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 570000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "明白了！earned income和pension income的区别。那如果Sharon只赚$10,000呢？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 560000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "完全正确！那时earned income（$10,000）超过了限额（$8,000），所以限额$8,000适用。你很好地理解了\"较小值\"规则。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 550000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "那主动参与者规则呢？如果新雇主有401(k)呢？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 540000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "很好的追问！主动参与者只影响**可抵扣性**，不影响**贡献资格**。2024年单身纳税人的phase-out范围是$77k-$87k。如果Sharon收入$90,000且有401(k)：a) 她能贡献IRA吗？ b) 她能抵扣吗？",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 530000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "a) 能贡献，因为earned income b) 不能抵扣，因为收入超过$87k",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 520000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "完美！你已经完全掌握了贡献与抵扣的区别。这时可以考虑Roth IRA作为非抵扣传统IRA的替代方案。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000 - 510000
                )
            )

            session2Messages.forEach { message ->
                database.messageDao().insertMessage(message)
            }

            // Session from 2025-10-13: Investment risk concepts
            val session3Messages = listOf(
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "关于投资风险，R平方是什么意思？为什么R平方=0.8意味着80%的回报变化由市场因素解释？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 259200000 - 600000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "很好的问题！让我们用苏格拉底式方法思考：如果你有一个投资组合，它的回报变化有多少是受市场整体影响，有多少是投资组合特有的？",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 259200000 - 590000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "我觉得市场因素影响大部分，但具体怎么计算呢？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 259200000 - 580000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "R平方是相关系数的平方。如果相关系数=0.9，R平方=0.81，意味着81%的回报变化由市场因素解释。剩下的19%是投资组合特有的非系统性风险。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 259200000 - 570000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "那系统性风险和非系统性风险的区别是什么？",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 259200000 - 560000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "系统性风险是市场整体风险（经济衰退、利率变化），无法通过多样化消除。非系统性风险是公司特有风险（CEO辞职、产品失败），可以通过多样化消除。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 259200000 - 550000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "明白了！所以R平方高意味着投资组合与市场高度相关，系统性风险大。",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 259200000 - 540000
                ),
                MessageEntity(
                    topicId = trackerTopic.id,
                    content = "完全正确！这就是费曼方法的应用——用自己的话解释复杂概念。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 259200000 - 530000
                )
            )

            session3Messages.forEach { message ->
                database.messageDao().insertMessage(message)
            }

            // Add messages to individual topic conversations
            val topics = database.topicDao().getAllTopics().first()
            
            // Add specific messages to 1031 Exchange topic
            val exchangeTopic = topics.find { it.title.contains("1031") } ?: topics[1]
            val exchangeMessages = listOf(
                MessageEntity(
                    topicId = exchangeTopic.id,
                    content = "我需要复习1031交换的平衡等式框架。",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 86400000
                ),
                MessageEntity(
                    topicId = exchangeTopic.id,
                    content = "好的！让我们回顾：放弃的 = 获得的。左边：旧FMV + 支付现金 + 新债务。右边：新FMV + 收到现金 + 债务减免。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 86300000
                ),
                MessageEntity(
                    topicId = exchangeTopic.id,
                    content = "Boot计算：收到的现金 + 未被替换的债务减免。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 86200000
                )
            )

            exchangeMessages.forEach { message ->
                database.messageDao().insertMessage(message)
            }

            // Add messages to Traditional IRA topic
            val iraTopic = topics.find { it.title.contains("Traditional IRA") } ?: topics[2]
            val iraMessages = listOf(
                MessageEntity(
                    topicId = iraTopic.id,
                    content = "复习IRA贡献规则：earned income vs pension income。",
                    senderType = "USER",
                    messageType = "TEXT",
                    timestamp = baseTime - 172800000
                ),
                MessageEntity(
                    topicId = iraTopic.id,
                    content = "关键点：贡献资格只需要earned income。抵扣性受active participant和收入限制影响。",
                    senderType = "AI",
                    messageType = "TEXT",
                    timestamp = baseTime - 172790000
                )
            )

            iraMessages.forEach { message ->
                database.messageDao().insertMessage(message)
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
        
        // 获取所有话题
        val topics = database.topicDao().getAllTopics().first()
        
        // 为每个话题创建对应的MindMap
        topics.forEach { topic ->
            val mindMap = MindMapEntity(
                topicId = topic.id,
                title = "${topic.title}学习路径",
                structure = "{}"
            )
            database.mindMapDao().insert(mindMap)
            
            // 为每个话题创建标准MindMap结构
            when {
                topic.title.contains("CFP") -> createCFPStudyMindMap(mindMap.id, database)
                topic.title.contains("1031") -> create1031ExchangeMindMap(mindMap.id, database)
                topic.title.contains("Traditional IRA") -> createIRAMindMap(mindMap.id, database)
                topic.title.contains("投资风险") -> createInvestmentRiskMindMap(mindMap.id, database)
                topic.title.contains("退休计划") -> createRetirementPlanMindMap(mindMap.id, database)
                else -> createDefaultMindMap(mindMap.id, topic.title, database)
            }
        }
    }

    private suspend fun createCFPStudyMindMap(mindMapId: String, database: AppDatabase) {
        val nodes = listOf(
            MindMapNode(mindMapId = mindMapId, title = "财务规划基础", progress = 95),
            MindMapNode(mindMapId = mindMapId, title = "投资规划", progress = 88),
            MindMapNode(mindMapId = mindMapId, title = "退休规划", progress = 85),
            MindMapNode(mindMapId = mindMapId, title = "税务规划", progress = 78),
            MindMapNode(mindMapId = mindMapId, title = "风险管理", progress = 82),
            MindMapNode(mindMapId = mindMapId, title = "遗产规划", progress = 70)
        )
        nodes.forEach { database.mindMapDao().insertNode(it) }
    }

    private suspend fun create1031ExchangeMindMap(mindMapId: String, database: AppDatabase) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "1031同类交换", progress = 95)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "平衡等式框架", progress = 95),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "Boot计算", progress = 90),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "成本基础计算", progress = 85),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "时间限制", progress = 80)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 初始化 AI 服务设置到数据库
     */
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
        
        // 初始化WhyFragment文案到数据库
        initializeWhyFragmentContent(database)
    }
    
    /**
     * 初始化WhyFragment文案内容
     */
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

    private suspend fun createIRAMindMap(mindMapId: String, database: AppDatabase) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "传统IRA规则", progress = 90)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "贡献资格", progress = 90),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "抵扣限制", progress = 85),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "提款规则", progress = 80),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "与Roth IRA比较", progress = 75)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    private suspend fun createInvestmentRiskMindMap(mindMapId: String, database: AppDatabase) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "投资风险与相关性", progress = 88)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "R平方概念", progress = 90),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "系统性风险", progress = 85),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "非系统性风险", progress = 80),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "多样化策略", progress = 75)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    private suspend fun createRetirementPlanMindMap(mindMapId: String, database: AppDatabase) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "退休计划分类", progress = 85)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "Defined Contribution", progress = 85),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "Defined Benefit", progress = 80),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "401(k)计划", progress = 75),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "养老金计算", progress = 70)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    private suspend fun createDefaultMindMap(mindMapId: String, title: String, database: AppDatabase) {
        val nodes = listOf(
            MindMapNode(mindMapId = mindMapId, title = "基础知识", progress = 0),
            MindMapNode(mindMapId = mindMapId, title = "核心概念", progress = 0),
            MindMapNode(mindMapId = mindMapId, title = "应用实例", progress = 0),
            MindMapNode(mindMapId = mindMapId, title = "疑难解答", progress = 0)
        )
        nodes.forEach { database.mindMapDao().insertNode(it) }
    }
}
