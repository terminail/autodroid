package com.autodroid.teachitback.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.teachitback.databinding.ItemTopicBinding
import com.autodroid.teachitback.model.TopicEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TopicsAdapter(
    private val onClick: (TopicEntity) -> Unit
) : RecyclerView.Adapter<TopicsAdapter.TopicViewHolder>() {

    private var topics = listOf<TopicEntity>()

    fun submitList(newTopics: List<TopicEntity>) {
        topics = newTopics
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding = ItemTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(topics[position])
    }

    override fun getItemCount(): Int = topics.size

    class TopicViewHolder(private val binding: ItemTopicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(topic: TopicEntity) {
            binding.titleText.text = topic.title
            binding.descriptionText.text = topic.description
            binding.progressText.text = "掌握度: ${topic.masteryLevel}%"

            val date = Date(topic.lastAccessed)
            binding.dateText.text = dateFormat.format(date)

            binding.root.setOnClickListener {
                onClick(topic)
            }
        }
    }
}
