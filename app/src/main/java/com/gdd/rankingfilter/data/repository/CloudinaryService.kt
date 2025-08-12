package com.gdd.rankingfilter.data.repository

import com.gdd.rankingfilter.data.model.Image
import com.gdd.rankingfilter.data.model.Song
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.data.repository.CloudinaryRepository.CloudinaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CloudinaryService {
    @GET("v1_1/{cloud_name}/resources/by_asset_folder")
    suspend fun getVideos(
        @Path("cloud_name") cloudName: String,
        @Query("asset_folder") folder: String,
        @Query("max_results") maxResults: Int = 50,
        @Query("next_cursor") nextCursor: String? = null,
        @Query("tags") tags: Boolean = true,
        @Query("context") context: Boolean = true
    ):  Response<CloudinaryResponse<Video>>


    @GET("v1_1/{cloud_name}/resources/by_asset_folder")
    suspend fun getSongs(
        @Path("cloud_name") cloudName: String,
        @Query("asset_folder") folder: String,
        @Query("max_results") maxResults: Int = 50,
        @Query("next_cursor") nextCursor: String? = null,
    ): Response<CloudinaryResponse<Song>>

    @GET("v1_1/{cloud_name}/resources/by_asset_folder")
    suspend fun getImages(
        @Path("cloud_name") cloudName: String,
        @Query("asset_folder") folder: String,
        @Query("max_results") maxResults: Int = 50,
        @Query("next_cursor") nextCursor: String? = null,
    ): Response<CloudinaryResponse<Image>>
}