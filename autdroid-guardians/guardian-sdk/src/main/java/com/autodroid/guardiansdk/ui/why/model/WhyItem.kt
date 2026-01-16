package com.autodroid.guardiansdk.ui.why.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class WhyItemType : Parcelable {
    HEADER,
    OVERVIEW,
    FEATURES,
    BENEFITS,
    SECURITY_STORY,
    STATISTICS,
    IMAGE_CARD
}

@Parcelize
data class WhyItem(
    val id: Int,
    val type: WhyItemType,
    val title: String,
    val content: String,
    val iconRes: Int? = null,
    val imageRes: Int? = null,
    val statistics: Map<String, String> = emptyMap()
) : Parcelable