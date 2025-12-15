package com.autodroid.workscripts.utils

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.autodroid.workscripts.R

class ControlGridAdapter(
    private val context: Context,
    private val controlList: List<ControlInfo>
) : BaseAdapter() {
    
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    
    override fun getCount(): Int {
        return controlList.size
    }
    
    override fun getItem(position: Int): ControlInfo {
        return controlList[position]
    }
    
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View
        
        if (convertView == null) {
            view = inflater.inflate(R.layout.item_control, parent, false)
            holder = ViewHolder()
            holder.textView = view.findViewById(R.id.tv_control_text)
            holder.typeView = view.findViewById(R.id.tv_control_type)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }
        
        val control = getItem(position)
        
        // 设置显示文本
        holder.textView.text = control.getDisplayText()
        holder.typeView.text = control.type
        
        // 根据不同控件类型设置不同背景色
        val bgColor = getColorForControlType(control.type)
        view.setBackgroundColor(bgColor)
        
        return view
    }
    
    private fun getColorForControlType(type: String): Int {
        return when (type) {
            "Button" -> Color.parseColor("#E3F2FD") // 浅蓝色
            "TextView" -> Color.parseColor("#E8F5E9") // 浅绿色
            "EditText" -> Color.parseColor("#FFF3E0") // 浅橙色
            "ImageButton" -> Color.parseColor("#F3E5F5") // 浅紫色
            "ImageView" -> Color.parseColor("#E0F7FA") // 浅青色
            else -> Color.parseColor("#F5F5F5") // 浅灰色
        }
    }
    
    private class ViewHolder {
        lateinit var textView: TextView
        lateinit var typeView: TextView
    }
}