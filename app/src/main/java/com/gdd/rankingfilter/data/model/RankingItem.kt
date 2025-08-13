package com.gdd.rankingfilter.data.model

data class RankingItem(
    val id: String,
    val coverUrl: String,
    val type: String,
    val imageList: List<Image>
)
