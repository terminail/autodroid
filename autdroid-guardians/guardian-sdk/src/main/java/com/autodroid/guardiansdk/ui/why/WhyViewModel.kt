package com.autodroid.guardiansdk.ui.why

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autodroid.guardiansdk.R
import com.autodroid.guardiansdk.ui.why.model.WhyItem
import com.autodroid.guardiansdk.ui.why.model.WhyItemType

class WhyViewModel(private val context: Context) : ViewModel() {

    private val _whyItems = MutableLiveData<List<WhyItem>>()
    val whyItems: LiveData<List<WhyItem>> = _whyItems

    fun loadWhyItems() {
        val items = mutableListOf<WhyItem>()
        
        // Header
        items.add(WhyItem(
            id = 1,
            type = WhyItemType.HEADER,
            title = context.getString(R.string.why_title),
            content = context.getString(R.string.why_subtitle)
        ))
        
        // Overview
        items.add(WhyItem(
            id = 2,
            type = WhyItemType.OVERVIEW,
            title = context.getString(R.string.why_overview_title),
            content = context.getString(R.string.why_overview_content),
            iconRes = R.drawable.guardian_ic_info
        ))
        
        // Features
        items.add(WhyItem(
            id = 3,
            type = WhyItemType.FEATURES,
            title = context.getString(R.string.why_features_title),
            content = context.getString(R.string.why_features_content),
            iconRes = R.drawable.guardian_ic_check
        ))
        
        // Benefits
        items.add(WhyItem(
            id = 4,
            type = WhyItemType.BENEFITS,
            title = context.getString(R.string.why_benefits_title),
            content = context.getString(R.string.why_benefits_content),
            iconRes = R.drawable.guardian_ic_star
        ))
        
        // Security Story 1
        items.add(WhyItem(
            id = 5,
            type = WhyItemType.SECURITY_STORY,
            title = context.getString(R.string.why_story1_title),
            content = context.getString(R.string.why_story1_content),
            iconRes = R.drawable.guardian_ic_shield
        ))
        
        // Statistics
        items.add(WhyItem(
            id = 6,
            type = WhyItemType.STATISTICS,
            title = context.getString(R.string.why_statistics_title),
            content = context.getString(R.string.why_statistics_content),
            statistics = mapOf(
                context.getString(R.string.why_stat_users) to "10,000+",
                context.getString(R.string.why_stat_alerts) to "5,000+",
                context.getString(R.string.why_stat_success) to "99.8%"
            )
        ))
        
        // Security Story 2
        items.add(WhyItem(
            id = 7,
            type = WhyItemType.SECURITY_STORY,
            title = context.getString(R.string.why_story2_title),
            content = context.getString(R.string.why_story2_content),
            iconRes = R.drawable.guardian_ic_emergency
        ))
        
        // Image Card
        items.add(WhyItem(
            id = 8,
            type = WhyItemType.IMAGE_CARD,
            title = context.getString(R.string.why_image_title),
            content = context.getString(R.string.why_image_content),
            imageRes = R.drawable.guardian_ic_security
        ))
        
        _whyItems.value = items
    }
}