package com.gdd.rankingfilter.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val public_id: String,
    val secure_url: String,
    val tags: List<String>?,
    val context: VideoContextWrapper?
) : Parcelable
