package com.gdd.rankingfilter.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoContextWrapper(
    val custom: VideoContext
) : Parcelable
