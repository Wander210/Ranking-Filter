package com.gdd.rankingfilter.data.repository

import android.content.Context
import android.util.Log
import com.gdd.rankingfilter.data.model.Image
import com.gdd.rankingfilter.data.model.RankingItem
import com.gdd.rankingfilter.data.model.Song
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.utils.ConfigManager
import com.gdd.rankingfilter.constant.MyConstant
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Responsibilities
 * 1. Read API credentials from assets/config.json via ConfigManager.
 * 2. Build an OkHttp client that injects Basic Auth on every request.
 * 3. Expose suspend functions that return strongly‑typed models.
 */
class CloudinaryRepository(context: Context) {

    private val configManager = ConfigManager(context)
    private val cloudinaryService: CloudinaryService

    init {
        // Verbose network logging
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Custom client that appends Basic Auth on each request
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Read credentials on the fly; throw if missing
                val cfg = configManager.getCloudinaryConfig()
                    ?: throw IllegalStateException("Cloudinary config not found")
                // Create Basic Auth header
                val credentials = Credentials.basic(cfg.api_key, cfg.api_secret)
                val authedRequest = chain.request().newBuilder()
                    .addHeader("Authorization", credentials)
                    .build()

                chain.proceed(authedRequest)
            }
            .build()

        // Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        cloudinaryService = retrofit.create(CloudinaryService::class.java)
    }

    private suspend inline fun <T> fetchItems(
        folder: String,
        tag: String,
        crossinline apiCall: suspend CloudinaryService.(String, String) -> Response<CloudinaryResponse<T>>
    ): List<T> {
        return try {
            val cfg = configManager.getCloudinaryConfig() ?: return emptyList()
            val resp = cloudinaryService.apiCall(cfg.cloud_name, folder)
            if (resp.isSuccessful) resp.body()?.resources ?: emptyList()
            else {
                Log.e(tag, "HTTP ${resp.code()} ${resp.message()}")
                emptyList()
            }
        } catch (e: Throwable) {
            Log.e(tag, "Error fetching items: ${e::class.java.simpleName} - ${e.message}")
            emptyList()
        }
    }

    /** Fetch all videos */
    suspend fun getVideos(): List<Video> =
        fetchItems(
            folder = "ranking_filter_videos",
            tag = "CloudinaryVideos",
            apiCall = CloudinaryService::getVideos
        )

    /** Fetch all songs */
    suspend fun getSongs(): List<Song> =
        fetchItems(
            folder = "ranking_filter_songs",
            tag = "CloudinarySongs",
            apiCall = CloudinaryService::getSongs
        )
    suspend fun getImages(folder: String): List<Image> =
        fetchItems(
            folder = folder,
            tag = "CloudinaryImages",
            apiCall = CloudinaryService::getImages
        )

    suspend fun getRankingItems(): List<RankingItem> {
        val rankingItemList = mutableListOf<RankingItem>()
        MyConstant.RANKING_TYPES.forEach { type ->
            var folderId = 1
            while (true) {
                val folderName = "ranking_filter_images/$type/$folderId"
                //call API, if it fails, break the loop
                val images = try {
                    getImages(folderName)
                } catch (_: Exception) {
                    break
                }
                if (images.isEmpty()) break
                val firstSourceUrl = images.first().secure_url
                val rankingItem = RankingItem(id = folderId, coverUrl = firstSourceUrl, type = type, imageList = images)
                rankingItemList += rankingItem
                folderId++
            }
        }
        return rankingItemList
    }


    data class CloudinaryResponse<T>(val resources: List<T>)
}