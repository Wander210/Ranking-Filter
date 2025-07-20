package com.gdd.rankingfilter.data.repository

import android.content.Context
import android.util.Log
import com.gdd.rankingfilter.data.model.Song
import com.gdd.rankingfilter.util.ConfigManager
import com.gdd.rankingfilter.data.model.Video
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

    suspend fun getVideos(): List<Video> {
        return try {
            val cfg = configManager.getCloudinaryConfig() ?: return emptyList()
            val apiResp = cloudinaryService.getVideos(
                cloudName = cfg.cloud_name,
                folder = "ranking_filter_videos")
            if (apiResp.isSuccessful) apiResp.body()?.resources ?: emptyList()
            else {
                Log.e(
                    "CloudinaryRepository",
                    "HTTP ${apiResp.code()} ${apiResp.message()}"
                )
                emptyList()
            }
        } catch (t: Throwable) {
            Log.e("CloudinaryRepository", "Exception type: ${t::class.java.simpleName}")
            Log.e("CloudinaryRepository", "Error message: ${t.message}")
            emptyList()
        }
    }

    suspend fun getSongs(): List<Song> {
        return try {
            val cfg = configManager.getCloudinaryConfig() ?: return emptyList()
            val apiResp = cloudinaryService.getSongs(
                cloudName = cfg.cloud_name,
                folder = "ranking_filter_songs")
            if (apiResp.isSuccessful) (apiResp.body()?.resources ?: emptyList())
            else {
                Log.e(
                    "CloudinaryRepository2",
                    "HTTP ${apiResp.code()} ${apiResp.message()}"
                )
                emptyList()
            }
        } catch (t: Throwable) {
            Log.e("CloudinaryRepository", "Exception type: ${t::class.java.simpleName}")
            Log.e("CloudinaryRepository", "Error message: ${t.message}")
            emptyList()
        }
    }
}