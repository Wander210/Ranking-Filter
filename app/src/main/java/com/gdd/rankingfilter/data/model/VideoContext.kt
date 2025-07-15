package com.gdd.rankingfilter.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoContext(
    val author: String?,
    val likes: String?
) : Parcelable
