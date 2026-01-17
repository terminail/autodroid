package com.autodroid.guardiansdk.ui.why.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autodroid.guardiansdk.databinding.GuardianWhyItemBenefitsBinding
import com.autodroid.guardiansdk.databinding.GuardianWhyItemFeaturesBinding
import com.autodroid.guardiansdk.databinding.GuardianWhyItemHeaderBinding
import com.autodroid.guardiansdk.databinding.GuardianWhyItemImageCardBinding
import com.autodroid.guardiansdk.databinding.GuardianWhyItemImportWardBinding
import com.autodroid.guardiansdk.databinding.GuardianWhyItemOverviewBinding
import com.autodroid.guardiansdk.databinding.GuardianWhyItemSecurityStoryBinding
import com.autodroid.guardiansdk.databinding.GuardianWhyItemShareGuardianBinding
import com.autodroid.guardiansdk.databinding.GuardianWhyItemStatisticsBinding
import com.autodroid.guardiansdk.ui.why.model.WhyItem
import com.autodroid.guardiansdk.ui.why.model.WhyItemType

class WhyAdapter : ListAdapter<WhyItem, RecyclerView.ViewHolder>(WhyItemDiffCallback) {

    interface OnItemClickListener {
        fun onShareGuardianClick()
        fun onImportWardClick()
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_OVERVIEW = 1
        private const val TYPE_FEATURES = 2
        private const val TYPE_BENEFITS = 3
        private const val TYPE_SECURITY_STORY = 4
        private const val TYPE_STATISTICS = 5
        private const val TYPE_IMAGE_CARD = 6
        private const val TYPE_SHARE_GUARDIAN = 7
        private const val TYPE_IMPORT_WARD = 8
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
            WhyItemType.SHARE_GUARDIAN -> TYPE_SHARE_GUARDIAN
            WhyItemType.IMPORT_WARD -> TYPE_IMPORT_WARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                GuardianWhyItemHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_OVERVIEW -> OverviewViewHolder(
                GuardianWhyItemOverviewBinding.inflate(inflater, parent, false)
            )
            TYPE_FEATURES -> FeaturesViewHolder(
                GuardianWhyItemFeaturesBinding.inflate(inflater, parent, false)
            )
            TYPE_BENEFITS -> BenefitsViewHolder(
                GuardianWhyItemBenefitsBinding.inflate(inflater, parent, false)
            )
            TYPE_SECURITY_STORY -> SecurityStoryViewHolder(
                GuardianWhyItemSecurityStoryBinding.inflate(inflater, parent, false)
            )
            TYPE_STATISTICS -> StatisticsViewHolder(
                GuardianWhyItemStatisticsBinding.inflate(inflater, parent, false)
            )
            TYPE_IMAGE_CARD -> ImageCardViewHolder(
                GuardianWhyItemImageCardBinding.inflate(inflater, parent, false)
            )
            TYPE_SHARE_GUARDIAN -> ShareGuardianViewHolder(
                GuardianWhyItemShareGuardianBinding.inflate(inflater, parent, false)
            )
            TYPE_IMPORT_WARD -> ImportWardViewHolder(
                GuardianWhyItemImportWardBinding.inflate(inflater, parent, false)
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
            is ShareGuardianViewHolder -> holder.bind(item)
            is ImportWardViewHolder -> holder.bind(item)
        }
    }

    inner class HeaderViewHolder(private val binding: GuardianWhyItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.subtitle.text = item.content
        }
    }

    inner class OverviewViewHolder(private val binding: GuardianWhyItemOverviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
        }
    }

    inner class FeaturesViewHolder(private val binding: GuardianWhyItemFeaturesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
        }
    }

    inner class BenefitsViewHolder(private val binding: GuardianWhyItemBenefitsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
        }
    }

    inner class SecurityStoryViewHolder(private val binding: GuardianWhyItemSecurityStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
        }
    }

    inner class StatisticsViewHolder(private val binding: GuardianWhyItemStatisticsBinding) :
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

    inner class ImageCardViewHolder(private val binding: GuardianWhyItemImageCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.imageRes?.let { binding.image.setImageResource(it) }
        }
    }

    inner class ShareGuardianViewHolder(private val binding: GuardianWhyItemShareGuardianBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
            
            binding.root.setOnClickListener {
                onItemClickListener?.onShareGuardianClick()
            }
        }
    }

    inner class ImportWardViewHolder(private val binding: GuardianWhyItemImportWardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WhyItem) {
            binding.title.text = item.title
            binding.content.text = item.content
            item.iconRes?.let { binding.icon.setImageResource(it) }
            
            binding.root.setOnClickListener {
                onItemClickListener?.onImportWardClick()
            }
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