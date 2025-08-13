package com.gdd.rankingfilter.data.model

import com.google.gson.annotations.SerializedName

data class FoldersResponse(
    val folders: List<FolderItem> = emptyList(),
    @SerializedName("next_cursor") val next_cursor: String? = null
)

data class FolderItem(
    val name: String,
    val path: String,
    val created_at: String? = null
)