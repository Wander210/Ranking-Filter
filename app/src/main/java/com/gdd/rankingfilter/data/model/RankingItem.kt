package com.gdd.rankingfilter.data.model

data class RankingItem(
    val id: Int,
    val coverUrl : String,
    val type : String,
    val imageList: List<Image>
)
