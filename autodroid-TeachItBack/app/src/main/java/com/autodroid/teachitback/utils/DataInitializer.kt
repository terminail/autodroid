package com.autodroid.teachitback.utils

import android.content.Context
import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MessageEntity
import com.autodroid.teachitback.model.TopicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import java.io.Serializable

// 基础AI服务接口
interface AIServiceProvider : Serializable {
    val id: String
    val name: String
    val description: String
    val baseUrl: String
    val defaultModel: String
    val freeQuota: String
    val apiKeyUrl: String
    val officialWebsite: String
    val isEnabled: Boolean
}

// 各个AI服务的具体实现
data class AIServiceDoubaoProvider(
    override val id: String = "doubao",
    override val name: String = "豆包",
    override val description: String = "字节跳动AI助手，中文优化，MoE架构，多模态能力强",
    override val baseUrl: String = "https://ark.cn-beijing.volces.com/api/v3",
    override val defaultModel: String = "doubao-pro-32k",
    override val freeQuota: String = "注册即送100万tokens；部分基础功能永久免费",
    override val apiKeyUrl: String = "https://developer.doubao.com",
    override val officialWebsite: String = "https://www.doubao.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceErnieProvider(
    override val id: String = "ernie",
    override val name: String = "文心一言",
    override val description: String = "百度AI大模型，中文语义理解突出，适配国产芯片",
    override val baseUrl: String = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1",
    override val defaultModel: String = "ernie-bot-turbo",
    override val freeQuota: String = "新用户免费额度100万tokens，有效期3个月",
    override val apiKeyUrl: String = "https://console.bce.baidu.com/qianfan",
    override val officialWebsite: String = "https://yiyan.baidu.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceQwenProvider(
    override val id: String = "qwen",
    override val name: String = "通义千问",
    override val description: String = "阿里巴巴AI大模型，多模态性能优异，开源生态活跃",
    override val baseUrl: String = "https://dashscope.aliyuncs.com/api/v1",
    override val defaultModel: String = "qwen-turbo",
    override val freeQuota: String = "新用户赠送100万tokens免费额度",
    override val apiKeyUrl: String = "https://dashscope.aliyuncs.com",
    override val officialWebsite: String = "https://tongyi.aliyun.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceDeepseekProvider(
    override val id: String = "deepseek",
    override val name: String = "DeepSeek",
    override val description: String = "深度求索AI，支持长文本处理，数理推理与工程优化突出",
    override val baseUrl: String = "https://api.deepseek.com/v1",
    override val defaultModel: String = "deepseek-chat",
    override val freeQuota: String = "注册赠送50万tokens；部分模型有短期免费调用次数",
    override val apiKeyUrl: String = "https://platform.deepseek.com",
    override val officialWebsite: String = "https://www.deepseek.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceZhipuProvider(
    override val id: String = "zhipu",
    override val name: String = "智谱AI",
    override val description: String = "清华系AI大模型，代码生成能力强，开源生态完善",
    override val baseUrl: String = "https://open.bigmodel.cn/api/paas/v3",
    override val defaultModel: String = "chatglm_turbo",
    override val freeQuota: String = "GLM-4.7-Flash免费调用；新用户初始额度50万tokens",
    override val apiKeyUrl: String = "https://open.bigmodel.cn",
    override val officialWebsite: String = "https://www.zhipuai.cn",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceSparkProvider(
    override val id: String = "spark",
    override val name: String = "讯飞星火",
    override val description: String = "科大讯飞AI大模型，语音交互与方言识别能力突出",
    override val baseUrl: String = "https://spark-api.xf-yun.com/v1.1",
    override val defaultModel: String = "general",
    override val freeQuota: String = "新用户免费额度60万tokens，语音功能有额外免费时长",
    override val apiKeyUrl: String = "https://console.xfyun.cn/services/spark",
    override val officialWebsite: String = "https://xinghuo.xfyun.cn",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceMinimaxProvider(
    override val id: String = "minimax",
    override val name: String = "MiniMax",
    override val description: String = "稀宇科技，代码生成与Agent能力突出，M2开源且商用友好",
    override val baseUrl: String = "https://api.minimax.chat/v1",
    override val defaultModel: String = "abab5.5-chat",
    override val freeQuota: String = "新用户注册送15元API余额；M2开源版可免费商用",
    override val apiKeyUrl: String = "https://platform.minimaxi.com",
    override val officialWebsite: String = "https://minimax.chat",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceKimiProvider(
    override val id: String = "kimi",
    override val name: String = "Kimi",
    override val description: String = "月之暗面，20万汉字上下文窗口，长文档处理效率高",
    override val baseUrl: String = "https://api.moonshot.cn/v1",
    override val defaultModel: String = "moonshot-v1-8k",
    override val freeQuota: String = "新用户免费额度80万tokens，部分基础功能无调用限制",
    override val apiKeyUrl: String = "https://platform.moonshot.cn",
    override val officialWebsite: String = "https://kimi.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceHunyuanProvider(
    override val id: String = "hunyuan",
    override val name: String = "混元大模型",
    override val description: String = "腾讯AI大模型，支持100万字长文本，无缝对接微信生态",
    override val baseUrl: String = "https://hunyuan.tencent.com",
    override val defaultModel: String = "hunyuan-standard",
    override val freeQuota: String = "新用户免费调用额度50万tokens，有效期1个月",
    override val apiKeyUrl: String = "https://cloud.tencent.com/product/hunyuan",
    override val officialWebsite: String = "https://混元.tencent.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceBaichuanProvider(
    override val id: String = "baichuan",
    override val name: String = "百川智能",
    override val description: String = "中文理解与创作能力强，开源模型生态完善，适配中小企业",
    override val baseUrl: String = "https://api.baichuan-ai.com",
    override val defaultModel: String = "baichuan2-7b-chat",
    override val freeQuota: String = "新用户免费额度50万tokens，开源版本可免费商用",
    override val apiKeyUrl: String = "https://platform.baichuan-ai.com",
    override val officialWebsite: String = "https://www.baichuan-ai.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceLingyiProvider(
    override val id: String = "lingyi",
    override val name: String = "零一万物",
    override val description: String = "轻量化部署优势明显，多模态与小样本学习能力强",
    override val baseUrl: String = "https://open.lingyiwanwu.com",
    override val defaultModel: String = "yi-34b-chat",
    override val freeQuota: String = "新用户免费额度30万tokens，部分轻量模型可免费商用",
    override val apiKeyUrl: String = "https://open.lingyiwanwu.com",
    override val officialWebsite: String = "https://www.lingyiwanwu.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

data class AIServiceJieyueProvider(
    override val id: String = "jieyue",
    override val name: String = "阶跃星辰",
    override val description: String = "高效推理与低延迟响应，适配工业互联网与物联网场景",
    override val baseUrl: String = "https://open.jieyuesx.com",
    override val defaultModel: String = "jieyue-standard",
    override val freeQuota: String = "新用户免费额度30万tokens，特定垂类场景有额外试用权益",
    override val apiKeyUrl: String = "https://open.jieyuesx.com",
    override val officialWebsite: String = "https://www.jieyuesx.com",
    override val isEnabled: Boolean = true
) : AIServiceProvider

class DataInitializer(private val context: Context) {

    companion object {
        fun getAIServiceProviders(): List<AIServiceProvider> {
            return listOf(
                AIServiceDoubaoProvider(),
                AIServiceErnieProvider(),
                AIServiceQwenProvider(),
                AIServiceDeepseekProvider(),
                AIServiceZhipuProvider(),
                AIServiceSparkProvider(),
                AIServiceMinimaxProvider(),
                AIServiceKimiProvider(),
                AIServiceHunyuanProvider(),
                AIServiceBaichuanProvider(),
                AIServiceLingyiProvider(),
                AIServiceJieyueProvider()
            )
        }
    }

    fun initializeDemoData() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            
            // 清除现有数据并重新初始化
            database.topicDao().deleteAllTopics()
            database.messageDao().deleteAllMessages()

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
        }
    }
}
