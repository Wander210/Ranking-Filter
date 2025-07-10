package com.gdd.rankingfilter.data.repository

import com.gdd.rankingfilter.data.model.CloudinaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CloudinaryService {
    @GET("v1_1/{cloud_name}/resources/video")
    suspend fun getVideos(
        @Path("cloud_name") cloudName: String,
        @Query("next_cursor") nextCursor: String? = null,
        @Query("tags") tags: Boolean = true,
        @Query("context") context: Boolean = true
    ): Response<CloudinaryResponse>
}