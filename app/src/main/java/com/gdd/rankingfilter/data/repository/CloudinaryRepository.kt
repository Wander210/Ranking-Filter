package com.gdd.rankingfilter.data.repository

import android.content.Context
import android.util.Log
import com.gdd.rankingfilter.constant.MyConstant
import com.gdd.rankingfilter.data.model.Image
import com.gdd.rankingfilter.data.model.RankingItem
import com.gdd.rankingfilter.data.model.Song
import com.gdd.rankingfilter.data.model.Video
import com.gdd.rankingfilter.utils.ConfigManager
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

            val folders = getFolderNames("ranking_filter_images/$type")
            for (folderName in folders) {
                Log.e("Flower", "Folder found: $folderName")
                try {
                    val fullFolderPath = "ranking_filter_images/$type/$folderName"
                    var images = getImages(fullFolderPath)

                    if (images.isEmpty()) {
                        Log.w("Flower", "No images found in folder: $fullFolderPath — skipping")
                        continue
                    }

                    val firstSourceUrl = images.firstOrNull()?.secure_url
                    if (firstSourceUrl.isNullOrBlank()) {
                        Log.w("Flower", "First image has no secure_url in $fullFolderPath — skipping")
                        continue
                    }

                    val rankingItem = RankingItem(
                        id = folderName,
                        coverUrl = firstSourceUrl,
                        type = type,
                        imageList = images
                    )
                    rankingItemList += rankingItem
                } catch (e: Throwable) {
                    Log.e("Flower", "Error processing folder $folderName: ${e::class.java.simpleName} - ${e.message}")
                }
            }
        }
        return rankingItemList
    }


    suspend fun getFolderNames(parentFolderPath: String): List<String> {
        return try {
            val cfg = configManager.getCloudinaryConfig() ?: return emptyList()
            val names = mutableListOf<String>()
            var nextCursor: String? = null

            do {
                val resp = cloudinaryService.getSubFolders(
                    cfg.cloud_name,
                    parentFolderPath,
                    maxResults = 100,
                    nextCursor = nextCursor
                )
                if (!resp.isSuccessful) {
                    Log.e("CloudinaryFolders", "HTTP ${resp.code()} ${resp.message()}")
                    break
                }
                val body = resp.body() ?: break
                names += body.folders.map { it.name }
                nextCursor = body.next_cursor
            } while (!nextCursor.isNullOrBlank())
            names
        } catch (e: Throwable) {
            Log.e(
                "CloudinaryFolders",
                "Error fetching folder names: ${e::class.java.simpleName} - ${e.message}"
            )
            emptyList()
        }
    }

    data class CloudinaryResponse<T>(val resources: List<T>)
}