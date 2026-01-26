package com.autodroid.teachitback.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.autodroid.teachitback.R
import com.autodroid.teachitback.config.PresetTopicCategories
import com.autodroid.teachitback.databinding.FragmentPresetDetailBinding
import com.autodroid.teachitback.model.TopicEntity
import com.autodroid.teachitback.viewmodel.WhyViewModel
import kotlinx.coroutines.launch

class PresetDetailFragment : Fragment() {
    private var _binding: FragmentPresetDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: WhyViewModel
    private var presetTopic: TopicEntity? = null
    private var personalTopicId: String? = null
    private var isCopying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[WhyViewModel::class.java]
        
        arguments?.let {
            val topicId = it.getString(ARG_TOPIC_ID)
            val topicTitle = it.getString(ARG_TOPIC_TITLE)
            val topicDescription = it.getString(ARG_TOPIC_DESCRIPTION)
            
            if (topicId != null && topicTitle != null && topicDescription != null) {
                // 使用 PresetTopicCategories 获取正确的分类节点ID
                val categoryNodeId = PresetTopicCategories.getCategoryForTopic(topicTitle)
                
                presetTopic = TopicEntity(
                    id = topicId,
                    title = topicTitle,
                    description = topicDescription,
                    topicCategoryId = categoryNodeId,
                    isPreset = true
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        presetTopic?.let { topic ->
            setupUI(topic)
            checkCopyStatus(topic.id)
        } ?: run {
            findNavController().navigateUp()
        }
    }

    private fun setupUI(topic: TopicEntity) {
        binding.titleText.text = topic.title
        binding.descriptionText.text = topic.description

        // 显示 breadcrumb 导航
        setupBreadcrumb(topic.topicCategoryId)
        
        val learningGoals = generateLearningGoals(topic.title)
        binding.learningGoalsText.text = learningGoals
        
        val knowledgePoints = generateKnowledgePoints(topic.title)
        binding.knowledgePointsText.text = knowledgePoints
        
        val recommendedPath = generateRecommendedPath(topic.title)
        binding.recommendedPathText.text = recommendedPath
        
        binding.copyButton.setOnClickListener {
            copyPresetTopic(topic)
        }
        
        binding.alreadyCopiedButton.setOnClickListener {
            personalTopicId?.let { id ->
                navigateToChat(id)
            }
        }
        
        binding.startLearningButton.setOnClickListener {
            personalTopicId?.let { id ->
                navigateToChat(id)
            }
        }
    }

    /**
     * 设置 breadcrumb 导航，显示主题在分类树中的位置
     */
    private fun setupBreadcrumb(treeNodeId: String) {
        val categoryPath = buildCategoryPath(treeNodeId)
        
        if (categoryPath.isNotEmpty()) {
            binding.breadcrumbContainer.visibility = View.VISIBLE
            binding.breadcrumbText.text = categoryPath
        } else {
            binding.breadcrumbContainer.visibility = View.GONE
        }
    }

    /**
     * 构建分类路径字符串
     * 例如：教育学习 > 财务金融 > CFP考试
     */
    private fun buildCategoryPath(treeNodeId: String): String {
        val path = mutableListOf<String>()
        var currentCategoryId: String? = treeNodeId
        
        // 递归构建路径
        while (currentCategoryId != null) {
            val currentCategory = PresetTopicCategories.getCategoryById(currentCategoryId)
                ?: break
            
            path.add(0, currentCategory.name) // 添加到开头
            
            // 移动到父节点
            currentCategoryId = currentCategory.parentId
        }
        
        return path.joinToString(" > ")
    }

    private fun checkCopyStatus(presetTopicId: String) {
        lifecycleScope.launch {
            val personalCopy = viewModel.checkPresetTopicCopied(presetTopicId)
            if (personalCopy != null) {
                personalTopicId = personalCopy.id
                showAlreadyCopiedState()
            } else {
                showNotCopiedState()
            }
        }
    }

    private fun copyPresetTopic(topic: TopicEntity) {
        if (isCopying) {
            return
        }
        
        lifecycleScope.launch {
            val existingCopy = viewModel.checkPresetTopicCopied(topic.id)
            if (existingCopy != null) {
                personalTopicId = existingCopy.id
                showAlreadyCopiedState()
                android.widget.Toast.makeText(requireContext(), "课程已在学习列表中", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            isCopying = true
            viewModel.copyPresetTopic(topic) { success ->
                isCopying = false
                if (success) {
                    android.widget.Toast.makeText(requireContext(), "已复制到学习列表", android.widget.Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        val personalCopy = viewModel.checkPresetTopicCopied(topic.id)
                        personalCopy?.let {
                            personalTopicId = it.id
                            showAlreadyCopiedState()
                        }
                    }
                } else {
                    android.widget.Toast.makeText(requireContext(), "复制失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showNotCopiedState() {
        binding.copyButton.visibility = View.VISIBLE
        binding.alreadyCopiedButton.visibility = View.GONE
        binding.startLearningButton.visibility = View.GONE
    }

    private fun showAlreadyCopiedState() {
        binding.copyButton.visibility = View.GONE
        binding.alreadyCopiedButton.visibility = View.VISIBLE
        binding.startLearningButton.visibility = View.GONE
    }

    private fun navigateToChat(topicId: String) {
        val bundle = Bundle().apply {
            putString("topicId", topicId)
            putString("topicTitle", presetTopic?.title ?: "")
        }
        findNavController().navigate(R.id.action_presetDetailFragment_to_chatFragment, bundle)
    }

    private fun generateLearningGoals(topicTitle: String): String {
        return when (topicTitle) {
            "CFP财务规划" -> "• 掌握CFP考试核心概念\n• 理解财务规划基本原则\n• 学习风险管理策略\n• 掌握投资规划方法\n• 理解税务规划要点"
            "投资组合管理" -> "• 理解资产配置原理\n• 掌握风险评估方法\n• 学习投资组合优化\n• 了解市场分析技巧\n• 掌握再平衡策略"
            "税务规划" -> "• 理解税收基本原理\n• 掌握税务优化方法\n• 学习税收筹划策略\n• 了解税收优惠政策\n• 掌握合规避税技巧"
            "高中数学" -> "• 掌握函数与方程\n• 理解几何图形性质\n• 学习数列与概率\n• 掌握导数与积分\n• 提升解题思维能力"
            "高中物理" -> "• 理解力学基本原理\n• 掌握电磁学知识\n• 学习热学与光学\n• 了解近代物理基础\n• 提升实验分析能力"
            "高中化学" -> "• 掌握元素周期表\n• 理解化学反应原理\n• 学习有机化学基础\n• 了解化学实验方法\n• 掌握化学计算技巧"
            "高中生物" -> "• 理解细胞结构与功能\n• 掌握遗传学基本原理\n• 学习生态系统知识\n• 了解生物进化理论\n• 提升实验探究能力"
            "高中英语" -> "• 掌握核心词汇与语法\n• 提升阅读理解能力\n• 练习听力与口语\n• 学习写作技巧\n• 培养英语思维"
            "高中历史" -> "• 掌握中国历史脉络\n• 了解世界历史发展\n• 理解历史事件背景\n• 学习历史分析方法\n• 培养历史思维能力"
            "高中地理" -> "• 掌握自然地理知识\n• 理解人文地理概念\n• 学习地图阅读技巧\n• 了解区域地理特征\n• 培养地理思维能力"
            "高中政治" -> "• 理解马克思主义基本原理\n• 掌握中国特色社会主义理论\n• 学习时事政治分析\n• 了解经济政治常识\n• 培养政治思维能力"
            else -> "• 设定明确学习目标\n• 制定详细学习计划\n• 持续跟踪学习进度\n• 及时复习巩固\n• 实践应用所学知识"
        }
    }

    private fun generateKnowledgePoints(topicTitle: String): String {
        return when (topicTitle) {
            "CFP财务规划" -> "• 财务规划流程\n• 风险管理\n• 投资规划\n• 税务规划\n• 退休规划\n• 遗产规划"
            "投资组合管理" -> "• 资产配置理论\n• 风险评估模型\n• 投资组合优化\n• 市场分析工具\n• 再平衡策略\n• 绩效评估"
            "税务规划" -> "• 个人所得税\n• 企业所得税\n• 财产税\n• 税收优惠政策\n• 税务筹划技巧\n• 合规避税方法"
            "高中数学" -> "• 集合与函数\n• 三角函数\n• 数列\n• 不等式\n• 解析几何\n• 导数与积分"
            "高中物理" -> "• 运动学\n• 动力学\n• 电磁学\n• 热学\n• 光学\n• 原子物理"
            "高中化学" -> "• 化学基本概念\n• 化学反应\n• 元素周期律\n• 化学键\n• 有机化学\n• 化学实验"
            "高中生物" -> "• 细胞结构\n• 遗传与变异\n• 生物进化\n• 生态系统\n• 生命活动调节\n• 生物技术"
            "高中英语" -> "• 词汇与语法\n• 阅读理解\n• 听力训练\n• 口语表达\n• 写作技巧\n• 翻译能力"
            "高中历史" -> "• 中国古代史\n• 中国近现代史\n• 世界古代史\n• 世界近现代史\n• 历史文化\n• 历史评价"
            "高中地理" -> "• 地球与地图\n• 自然地理\n• 人文地理\n• 区域地理\n• 地理信息技术\n• 地理实践"
            "高中政治" -> "• 经济生活\n• 政治生活\n• 文化生活\n• 生活与哲学\n• 时事政治\n• 政治参与"
            else -> "• 核心概念\n• 重要原理\n• 实际应用\n• 常见问题\n• 学习方法\n• 考试技巧"
        }
    }

    private fun generateRecommendedPath(topicTitle: String): String {
        return when (topicTitle) {
            "CFP财务规划" -> "1. 学习财务规划基础知识\n2. 掌握风险管理方法\n3. 学习投资规划策略\n4. 理解税务规划要点\n5. 综合应用与实践"
            "投资组合管理" -> "1. 学习资产配置理论\n2. 掌握风险评估方法\n3. 学习投资组合优化\n4. 实践市场分析\n5. 持续优化调整"
            "税务规划" -> "1. 理解税收基本原理\n2. 学习税务优化方法\n3. 掌握税收筹划策略\n4. 了解税收优惠政策\n5. 实践应用与合规"
            "高中数学" -> "1. 巩固基础概念\n2. 掌握解题方法\n3. 练习典型例题\n4. 总结归纳规律\n5. 提升应试能力"
            "高中物理" -> "1. 理解基本概念\n2. 掌握物理定律\n3. 练习计算题\n4. 做好实验分析\n5. 综合应用提升"
            "高中化学" -> "1. 记忆基础元素\n2. 理解反应原理\n3. 练习化学计算\n4. 做好化学实验\n5. 综合应用提升"
            "高中生物" -> "1. 理解细胞结构\n2. 掌握遗传规律\n3. 学习生态系统\n4. 了解生物进化\n5. 综合应用提升"
            "高中英语" -> "1. 积累核心词汇\n2. 掌握语法规则\n3. 练习阅读理解\n4. 提升听说能力\n5. 强化写作训练"
            "高中历史" -> "1. 梳理历史脉络\n2. 理解历史事件\n3. 分析历史背景\n4. 学习历史方法\n5. 培养历史思维"
            "高中地理" -> "1. 掌握基础知识\n2. 学习地图阅读\n3. 理解地理现象\n4. 分析区域特征\n5. 培养地理思维"
            "高中政治" -> "1. 理解基本原理\n2. 掌握核心概念\n3. 学习时事政治\n4. 分析实际问题\n5. 培养政治思维"
            else -> "1. 了解课程大纲\n2. 制定学习计划\n3. 系统学习知识\n4. 实践应用练习\n5. 复习巩固提升"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TOPIC_ID = "topic_id"
        private const val ARG_TOPIC_TITLE = "topic_title"
        private const val ARG_TOPIC_DESCRIPTION = "topic_description"

        fun newInstance(topic: TopicEntity): PresetDetailFragment {
            return PresetDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOPIC_ID, topic.id)
                    putString(ARG_TOPIC_TITLE, topic.title)
                    putString(ARG_TOPIC_DESCRIPTION, topic.description)
                }
            }
        }
    }
}
