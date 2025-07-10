package com.gdd.rankingfilter.data.model

data class Video(
    val public_id: String,
    val secure_url: String,
    val tags: List<String>?,
    val context: ContextWrapper?
)
