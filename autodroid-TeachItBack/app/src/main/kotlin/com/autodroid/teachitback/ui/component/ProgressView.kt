package com.autodroid.teachitback.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.autodroid.teachitback.model.ProgressAnalysis

/**
 * 学习进度显示组件
 * 可视化显示整体进度、概念掌握度和知识缺口
 */
class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 20f
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val rect = RectF()
    private var progressAnalysis: ProgressAnalysis? = null

    /**
     * 设置进度分析数据
     */
    fun setProgressAnalysis(analysis: ProgressAnalysis?) {
        this.progressAnalysis = analysis
        invalidate() // 重绘视图
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (minOf(width, height) / 2f) - 40f

        // 绘制背景圆环
        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawOval(rect, backgroundPaint)

        // 绘制进度圆环
        progressAnalysis?.let { analysis ->
            val progress = analysis.overallProgress / 100f
            val angle = progress * 360f

            canvas.drawArc(rect, -90f, angle, false, progressPaint)

            // 绘制进度文本
            val progressText = "${analysis.overallProgress}%"
            canvas.drawText(progressText, centerX, centerY - 10f, textPaint)

            // 绘制标签
            val labelPaint = Paint(textPaint).apply {
                textSize = 24f
            }
            canvas.drawText("学习进度", centerX, centerY + 30f, labelPaint)
        }
    }

    /**
     * 获取进度颜色（根据掌握度）
     */
    private fun getMasteryColor(mastery: Int): Int {
        return when {
            mastery >= 80 -> Color.parseColor("#4CAF50") // 绿色 - 优秀
            mastery >= 60 -> Color.parseColor("#FF9800") // 橙色 - 良好
            else -> Color.parseColor("#F44336") // 红色 - 需要加强
        }
    }
}