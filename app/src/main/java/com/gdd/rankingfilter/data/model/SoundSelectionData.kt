package com.gdd.rankingfilter.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SoundSelectionData(
    val selectedPosition: Int,
    val clipStartTimeMs: Long = 0L,
    val clipDurationMs: Long = 0L
) : Parcelable