package com.gdd.rankingfilter.data.model

data class Song(
    val public_id: String,
    val secure_url: String,
    var duration: Long? = 0L,
    var isSelected: Boolean = false
)

