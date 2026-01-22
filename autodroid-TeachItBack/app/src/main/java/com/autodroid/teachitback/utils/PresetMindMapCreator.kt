package com.autodroid.teachitback.utils

import com.autodroid.teachitback.database.AppDatabase
import com.autodroid.teachitback.model.MindMapNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 预置课程详细MindMap结构创建器
 * 为每个预置课程创建详细的树形MindMap结构
 */
class PresetMindMapCreator(private val database: AppDatabase) {

    /**
     * 根据科目类型创建详细的MindMap结构
     */
    suspend fun createDetailedMindMapStructure(mindMapId: String, subject: String) = withContext(Dispatchers.IO) {
        when (subject.toLowerCase()) {
            "高中三年级生物" -> createDetailedBiologyStructure(mindMapId)
            "高中物理" -> createDetailedPhysicsStructure(mindMapId)
            "高中化学" -> createDetailedChemistryStructure(mindMapId)
            "微积分基础" -> createDetailedCalculusStructure(mindMapId)
            "编程入门" -> createDetailedProgrammingStructure(mindMapId)
            else -> createDefaultStructure(mindMapId, subject)
        }
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
    private suspend fun createDefaultStructure(mindMapId: String, subject: String) {
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
        // 创建MindMap实体
        val mindMap = com.autodroid.teachitback.model.MindMapEntity(
            topicId = topicId,
            title = "${title}学习路径",
            structure = "{}"
        )
        database.mindMapDao().insert(mindMap)
        
        // 创建详细结构
        createDetailedMindMapStructure(mindMap.id, title)
        
        mindMap.id
    }
}