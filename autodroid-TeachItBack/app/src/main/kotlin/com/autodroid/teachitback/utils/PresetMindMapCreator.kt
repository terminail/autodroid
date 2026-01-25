package com.autodroid.teachitback.utils

import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MindMapEntity
import com.autodroid.teachitback.model.MindMapNode
import com.autodroid.teachitback.model.TopicEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 预置课程详细MindMap结构创建器
 * 为每个预置课程创建详细的树形MindMap结构
 */
class PresetMindMapCreator(private val database: AppDatabase) {

    /**
     * 根据主题类型创建详细的MindMap结构
     */
    suspend fun createDetailedMindMapStructure(mindMapId: String, topic: String) = withContext(Dispatchers.IO) {
        android.util.Log.d("PresetMindMapCreator", "Creating detailed structure for mindMapId: $mindMapId, topic: $topic")
        
        val result = when (topic) {
            "CFP财务规划" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched CFP财务规划")
                createCFPStructure(mindMapId)
            }
            "投资组合管理" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 投资组合管理")
                createPortfolioManagementStructure(mindMapId)
            }
            "税务规划" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 税务规划")
                createTaxPlanningStructure(mindMapId)
            }
            "高中数学" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 高中数学")
                createMathematicsStructure(mindMapId)
            }
            "高中物理" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 高中物理")
                createPhysicsStructure(mindMapId)
            }
            "高中化学" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 高中化学")
                createChemistryStructure(mindMapId)
            }
            "高中生物" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 高中生物")
                createBiologyStructure(mindMapId)
            }
            "高中英语" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 高中英语")
                createEnglishStructure(mindMapId)
            }
            "高中历史" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 高中历史")
                createHistoryStructure(mindMapId)
            }
            "高中地理" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 高中地理")
                createGeographyStructure(mindMapId)
            }
            "高中政治" -> {
                android.util.Log.d("PresetMindMapCreator", "Matched 高中政治")
                createPoliticsStructure(mindMapId)
            }
            else -> {
                android.util.Log.d("PresetMindMapCreator", "No match found, using default structure")
                createDefaultStructure(mindMapId, topic)
            }
        }
        
        android.util.Log.d("PresetMindMapCreator", "Finished creating detailed structure for mindMapId: $mindMapId")
    }

    /**
     * 创建高中生物详细结构
     */
    private suspend fun createDetailedBiologyStructure(mindMapId: String) {
        // 创建根节点
        val cellBiology = MindMapNode(mindMapId = mindMapId, title = "细胞生物学", progress = 0)
        database.mindMapDao().insertNode(cellBiology)
        
        // 创建子节点
        val cellStructure = MindMapNode(mindMapId = mindMapId, parentId = cellBiology.id, title = "细胞结构", progress = 0)
        val cellFunction = MindMapNode(mindMapId = mindMapId, parentId = cellBiology.id, title = "细胞功能", progress = 0)
        val cellDivision = MindMapNode(mindMapId = mindMapId, parentId = cellBiology.id, title = "细胞分裂", progress = 0)
        
        listOf(cellStructure, cellFunction, cellDivision).forEach { database.mindMapDao().insertNode(it) }
        
        // 创建孙子节点
        val dnaStructure = MindMapNode(mindMapId = mindMapId, parentId = cellStructure.id, title = "DNA结构", progress = 0)
        val rnaFunction = MindMapNode(mindMapId = mindMapId, parentId = cellFunction.id, title = "RNA功能", progress = 0)
        val mitosis = MindMapNode(mindMapId = mindMapId, parentId = cellDivision.id, title = "有丝分裂", progress = 0)
        val meiosis = MindMapNode(mindMapId = mindMapId, parentId = cellDivision.id, title = "减数分裂", progress = 0)
        
        listOf(dnaStructure, rnaFunction, mitosis, meiosis).forEach { database.mindMapDao().insertNode(it) }
        
        // 创建遗传学节点
        val genetics = MindMapNode(mindMapId = mindMapId, title = "遗传学", progress = 0)
        database.mindMapDao().insertNode(genetics)
        
        val mendelGenetics = MindMapNode(mindMapId = mindMapId, parentId = genetics.id, title = "孟德尔遗传", progress = 0)
        val molecularGenetics = MindMapNode(mindMapId = mindMapId, parentId = genetics.id, title = "分子遗传学", progress = 0)
        
        listOf(mendelGenetics, molecularGenetics).forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中物理详细结构
     */
    private suspend fun createDetailedPhysicsStructure(mindMapId: String) {
        // 力学部分
        val mechanics = MindMapNode(mindMapId = mindMapId, title = "力学", progress = 0)
        database.mindMapDao().insertNode(mechanics)
        
        val kinematics = MindMapNode(mindMapId = mindMapId, parentId = mechanics.id, title = "运动学", progress = 0)
        val dynamics = MindMapNode(mindMapId = mindMapId, parentId = mechanics.id, title = "动力学", progress = 0)
        val statics = MindMapNode(mindMapId = mindMapId, parentId = mechanics.id, title = "静力学", progress = 0)
        
        listOf(kinematics, dynamics, statics).forEach { database.mindMapDao().insertNode(it) }
        
        // 电学部分
        val electricity = MindMapNode(mindMapId = mindMapId, title = "电学", progress = 0)
        database.mindMapDao().insertNode(electricity)
        
        val electrostatics = MindMapNode(mindMapId = mindMapId, parentId = electricity.id, title = "静电学", progress = 0)
        val circuits = MindMapNode(mindMapId = mindMapId, parentId = electricity.id, title = "电路分析", progress = 0)
        
        listOf(electrostatics, circuits).forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中化学详细结构
     */
    private suspend fun createDetailedChemistryStructure(mindMapId: String) {
        // 无机化学
        val inorganic = MindMapNode(mindMapId = mindMapId, title = "无机化学", progress = 0)
        database.mindMapDao().insertNode(inorganic)
        
        val periodicTable = MindMapNode(mindMapId = mindMapId, parentId = inorganic.id, title = "元素周期表", progress = 0)
        val chemicalBonds = MindMapNode(mindMapId = mindMapId, parentId = inorganic.id, title = "化学键", progress = 0)
        
        listOf(periodicTable, chemicalBonds).forEach { database.mindMapDao().insertNode(it) }
        
        // 有机化学
        val organic = MindMapNode(mindMapId = mindMapId, title = "有机化学", progress = 0)
        database.mindMapDao().insertNode(organic)
        
        val hydrocarbons = MindMapNode(mindMapId = mindMapId, parentId = organic.id, title = "碳氢化合物", progress = 0)
        val functionalGroups = MindMapNode(mindMapId = mindMapId, parentId = organic.id, title = "官能团", progress = 0)
        
        listOf(hydrocarbons, functionalGroups).forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建微积分基础详细结构
     */
    private suspend fun createDetailedCalculusStructure(mindMapId: String) {
        // 微分学
        val differential = MindMapNode(mindMapId = mindMapId, title = "微分学", progress = 0)
        database.mindMapDao().insertNode(differential)
        
        val limits = MindMapNode(mindMapId = mindMapId, parentId = differential.id, title = "极限", progress = 0)
        val derivatives = MindMapNode(mindMapId = mindMapId, parentId = differential.id, title = "导数", progress = 0)
        val applications = MindMapNode(mindMapId = mindMapId, parentId = differential.id, title = "导数应用", progress = 0)
        
        listOf(limits, derivatives, applications).forEach { database.mindMapDao().insertNode(it) }
        
        // 积分学
        val integral = MindMapNode(mindMapId = mindMapId, title = "积分学", progress = 0)
        database.mindMapDao().insertNode(integral)
        
        val indefinite = MindMapNode(mindMapId = mindMapId, parentId = integral.id, title = "不定积分", progress = 0)
        val definite = MindMapNode(mindMapId = mindMapId, parentId = integral.id, title = "定积分", progress = 0)
        
        listOf(indefinite, definite).forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建编程入门详细结构
     */
    private suspend fun createDetailedProgrammingStructure(mindMapId: String) {
        // 编程基础
        val basics = MindMapNode(mindMapId = mindMapId, title = "编程基础", progress = 0)
        database.mindMapDao().insertNode(basics)
        
        val variables = MindMapNode(mindMapId = mindMapId, parentId = basics.id, title = "变量与数据类型", progress = 0)
        val controlFlow = MindMapNode(mindMapId = mindMapId, parentId = basics.id, title = "控制流", progress = 0)
        val functions = MindMapNode(mindMapId = mindMapId, parentId = basics.id, title = "函数", progress = 0)
        
        listOf(variables, controlFlow, functions).forEach { database.mindMapDao().insertNode(it) }
        
        // 面向对象编程
        val oop = MindMapNode(mindMapId = mindMapId, title = "面向对象编程", progress = 0)
        database.mindMapDao().insertNode(oop)
        
        val classes = MindMapNode(mindMapId = mindMapId, parentId = oop.id, title = "类与对象", progress = 0)
        val inheritance = MindMapNode(mindMapId = mindMapId, parentId = oop.id, title = "继承与多态", progress = 0)
        
        listOf(classes, inheritance).forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建默认结构
     */
    private suspend fun createDefaultStructure(mindMapId: String, topic: String) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "基础知识", progress = 0)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "核心概念", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "应用实例", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "疑难解答", progress = 0)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 为预置课程创建完整的MindMap结构
     */
    suspend fun createPresetCourseMindMap(topicId: String, title: String) = withContext(Dispatchers.IO) {
        android.util.Log.d("PresetMindMapCreator", "Creating MindMap for topicId: $topicId, title: $title")
        
        // 创建MindMap实体
        val mindMap = MindMapEntity(
            topicId = topicId,
            title = "${title}学习路径",
            structure = "{}"
        )
        database.mindMapDao().insert(mindMap)
        
        android.util.Log.d("PresetMindMapCreator", "Inserted MindMap with id: ${mindMap.id} for topicId: $topicId")
        
        // 创建详细结构
        createDetailedMindMapStructure(mindMap.id, title)
        
        android.util.Log.d("PresetMindMapCreator", "Created MindMap structure for mindMapId: ${mindMap.id}")
        
        mindMap.id
    }

    /**
     * 创建CFP财务规划结构
     */
    private suspend fun createCFPStructure(mindMapId: String) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "CFP财务规划", progress = 0)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "财务规划流程", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "风险管理", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "投资规划", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "税务规划", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "退休规划", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "遗产规划", progress = 0)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建投资组合管理结构
     */
    private suspend fun createPortfolioManagementStructure(mindMapId: String) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "投资组合管理", progress = 0)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "资产配置理论", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "风险评估模型", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "投资组合优化", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "市场分析工具", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "再平衡策略", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "绩效评估", progress = 0)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建税务规划结构
     */
    private suspend fun createTaxPlanningStructure(mindMapId: String) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "税务规划", progress = 0)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "个人所得税", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "企业所得税", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "财产税", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "税收优惠政策", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "税务筹划技巧", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "合规避税方法", progress = 0)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中数学结构
     */
    private suspend fun createMathematicsStructure(mindMapId: String) {
        android.util.Log.d("PresetMindMapCreator", "Creating mathematics structure for mindMapId: $mindMapId")
        
        val rootNode = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            title = "高中数学", 
            progress = 0
        )
        database.mindMapDao().insertNode(rootNode)
        
        android.util.Log.d("PresetMindMapCreator", "Inserted root node: ${rootNode.id}")
        
        // 第一级子节点
        val setFunction = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = rootNode.id, 
            title = "集合与函数", 
            progress = 0
        )
        
        val trigFunction = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = rootNode.id, 
            title = "三角函数", 
            progress = 0
        )
        
        val sequence = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = rootNode.id, 
            title = "数列", 
            progress = 0
        )
        
        val inequality = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = rootNode.id, 
            title = "不等式", 
            progress = 0
        )
        
        val analyticGeometry = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = rootNode.id, 
            title = "解析几何", 
            progress = 0
        )
        
        val derivative = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = rootNode.id, 
            title = "导数与积分", 
            progress = 0
        )
        
        // 插入第一级子节点
        listOf(setFunction, trigFunction, sequence, inequality, analyticGeometry, derivative).forEach { node ->
            android.util.Log.d("PresetMindMapCreator", "Inserting first level node: ${node.title}")
            database.mindMapDao().insertNode(node)
        }
        
        // 第二级子节点：三角函数的子节点
        val trigBasic = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = trigFunction.id, 
            title = "三角函数基础", 
            progress = 0
        )
        
        val trigGraph = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = trigFunction.id, 
            title = "三角函数图像", 
            progress = 0
        )
        
        val trigEquation = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = trigFunction.id, 
            title = "三角方程", 
            progress = 0
        )
        
        val trigApplication = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = trigFunction.id, 
            title = "三角函数应用", 
            progress = 0
        )
        
        // 插入三角函数的子节点
        listOf(trigBasic, trigGraph, trigEquation, trigApplication).forEach { node ->
            android.util.Log.d("PresetMindMapCreator", "Inserting trig child node: ${node.title}")
            database.mindMapDao().insertNode(node)
        }
        
        // 第三级子节点：三角函数应用下的子节点
        val derivativeConcept = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = trigApplication.id, 
            title = "导数概念", 
            progress = 0
        )
        
        val derivativeDefinition = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = trigApplication.id, 
            title = "导数定义", 
            progress = 0
        )
        
        val derivativeRule = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            parentId = trigApplication.id, 
            title = "求导法则", 
            progress = 0
        )
        
        // 插入三角函数应用的子节点
        listOf(derivativeConcept, derivativeDefinition, derivativeRule).forEach { node ->
            android.util.Log.d("PresetMindMapCreator", "Inserting trig application child node: ${node.title}")
            database.mindMapDao().insertNode(node)
        }
        
        android.util.Log.d("PresetMindMapCreator", "Successfully created mathematics structure with 3 levels for mindMapId: $mindMapId")
    }

    /**
     * 创建高中物理结构
     */
    private suspend fun createPhysicsStructure(mindMapId: String) {
        val rootNode = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            title = "高中物理", 
            progress = 0
        )
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "运动学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "动力学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "电磁学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "热学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "光学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "原子物理", 
                progress = 0
            )
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中化学结构
     */
    private suspend fun createChemistryStructure(mindMapId: String) {
        val rootNode = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            title = "高中化学", 
            progress = 0
        )
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "无机化学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "有机化学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "物理化学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "分析化学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "化学实验", 
                progress = 0
            )
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中生物结构
     */
    private suspend fun createBiologyStructure(mindMapId: String) {
        val rootNode = MindMapNode(
            id = java.util.UUID.randomUUID().toString(),
            mindMapId = mindMapId, 
            title = "高中生物", 
            progress = 0
        )
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "细胞生物学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "遗传学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "生态学", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "进化论", 
                progress = 0
            ),
            MindMapNode(
                id = java.util.UUID.randomUUID().toString(),
                mindMapId = mindMapId, 
                parentId = rootNode.id, 
                title = "人体生理", 
                progress = 0
            )
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中英语结构
     */
    private suspend fun createEnglishStructure(mindMapId: String) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "高中英语", progress = 0)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "词汇与语法", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "阅读理解", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "听力训练", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "口语表达", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "写作技巧", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "翻译能力", progress = 0)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中历史结构
     */
    private suspend fun createHistoryStructure(mindMapId: String) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "高中历史", progress = 0)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "中国古代史", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "中国近现代史", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "世界古代史", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "世界近现代史", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "历史文化", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "历史评价", progress = 0)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中地理结构
     */
    private suspend fun createGeographyStructure(mindMapId: String) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "高中地理", progress = 0)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "地球与地图", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "自然地理", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "人文地理", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "区域地理", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "地理信息技术", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "地理实践", progress = 0)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    /**
     * 创建高中政治结构
     */
    private suspend fun createPoliticsStructure(mindMapId: String) {
        val rootNode = MindMapNode(mindMapId = mindMapId, title = "高中政治", progress = 0)
        database.mindMapDao().insertNode(rootNode)
        
        val childNodes = listOf(
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "经济生活", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "政治生活", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "文化生活", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "生活与哲学", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "时事政治", progress = 0),
            MindMapNode(mindMapId = mindMapId, parentId = rootNode.id, title = "政治参与", progress = 0)
        )
        childNodes.forEach { database.mindMapDao().insertNode(it) }
    }

    suspend fun createAllPresetMindMaps() = withContext(Dispatchers.IO) {
        val topics = database.topicDao().getAllTopics().first()
        val presetList = topics.filter { topic: TopicEntity -> topic.isPreset }
        
        presetList.forEach { topic: TopicEntity ->
            val existingMindMap = database.mindMapDao().getByTopicId(topic.id)
            if (existingMindMap == null) {
                createPresetCourseMindMap(topic.id, topic.title)
            }
        }
    }
}