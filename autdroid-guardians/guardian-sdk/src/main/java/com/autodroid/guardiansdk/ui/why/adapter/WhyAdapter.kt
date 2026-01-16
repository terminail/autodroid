package com.autodroid.guardiansdk.ui.why.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.databinding.*
import com.autodroid.guardiansdk.ui.why.model.WhyItem
import com.autodroid.guardiansdk.ui.why.model.WhyItemType

class WhyAdapter : ListAdapter<WhyItem, RecyclerView.ViewHolder>(WhyItemDiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_OVERVIEW = 1
        private const val TYPE_FEATURES = 2
        private const val TYPE_BENEFITS = 3
        private const val TYPE_SECURITY_STORY = 4
        private const val TYPE_STATISTICS = 5
        private const val TYPE_IMAGE_CARD = 6
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            WhyItemType.HEADER -> TYPE_HEADER
            WhyItemType.OVERVIEW -> TYPE_OVERVIEW
            WhyItemType.FEATURES -> TYPE_FEATURES
            WhyItemType.BENEFITS -> TYPE_BENEFITS
            WhyItemType.SECURITY_STORY -> TYPE_SECURITY_STORY
            WhyItemType.STATISTICS -> TYPE_STATISTICS
            WhyItemType.IMAGE_CARD -> TYPE_IMAGE_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                WhyItemHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_OVERVIEW -> OverviewViewHolder(
                WhyItemOverviewBinding.inflate(inflater, parent, false)
            )
            TYPE_FEATURES -> FeaturesViewHolder(
                WhyItemFeaturesBinding.inflate(inflater, parent, false)
            )
            TYPE_BENEFITS -> BenefitsViewHolder(
                WhyItemBenefitsBinding.inflate(inflater, parent, false)
            )
            TYPE_SECURITY_STORY -> SecurityStoryViewHolder(
                WhyItemSecurityStoryBinding.inflate(inflater, parent, false)
            )
            TYPE_STATISTICS -> StatisticsViewHolder(
                WhyItemStatisticsBinding.inflate(inflater, parent, false)
            )
            TYPE_IMAGE_CARD -> ImageCardViewHolder(
                WhyItemImageCardBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.bind(item)
            is OverviewViewHolder -> holder.bind(item)
            is FeaturesViewHolder -> holder.bind(item)
            is BenefitsViewHolder -> holder.bind(item)
            is SecurityStoryViewHolder -> holder.bind(item)
            is StatisticsViewHolder -> holder.bind(item)
            is ImageCardViewHolder -> holder.bind(item)
        }
    }

    inner class HeaderViewHolder(private val binding: WhyItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.subtitle.text = item.content
        }
    }

    inner class OverviewViewHolder(private val binding: WhyItemOverviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
        }
    }

    inner class FeaturesViewHolder(private val binding: WhyItemFeaturesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
        }
    }

    inner class BenefitsViewHolder(private val binding: WhyItemBenefitsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
        }
    }

    inner class SecurityStoryViewHolder(private val binding: WhyItemSecurityStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
        }
    }

    inner class StatisticsViewHolder(private val binding: WhyItemStatisticsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            
            // Bind statistics
            if (item.statistics.isNotEmpty()) {
                binding.stat1Label.text = item.statistics.keys.firstOrNull() ?: ""
                binding.stat1Value.text = item.statistics.values.firstOrNull() ?: ""
                
                if (item.statistics.size > 1) {
                    binding.stat2Label.text = item.statistics.keys.elementAtOrNull(1) ?: ""
                    binding.stat2Value.text = item.statistics.values.elementAtOrNull(1) ?: ""
                }
                
                if (item.statistics.size > 2) {
                    binding.stat3Label.text = item.statistics.keys.elementAtOrNull(2) ?: ""
                    binding.stat3Value.text = item.statistics.values.elementAtOrNull(2) ?: ""
                }
            }
        }
    }

    inner class ImageCardViewHolder(private val binding: WhyItemImageCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.imageRes?.let { binding.image.setImageResource(it) }
        }
    }
}

object WhyItemDiffCallback : DiffUtil.ItemCallback<WhyItem>() {
    override fun areItemsTheSame(oldItem: WhyItem, newItem: WhyItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: WhyItem, newItem: WhyItem): Boolean {
        return oldItem == newItem
    }
}