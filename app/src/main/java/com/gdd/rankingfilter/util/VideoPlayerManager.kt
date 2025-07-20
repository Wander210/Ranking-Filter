package com.gdd.rankingfilter.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.util.Collections

/**
 * VideoPlayerManager is a singleton responsible for:
 *
 * 1. Managing a thread-safe pool of ExoPlayer instances to enable reuse,
 *    avoiding repeated creation which would consume unnecessary resources.
 *
 * 2. Setting up a video cache using Media3 SimpleCache to improve playback performance
 *    and support video preloading.
 *
 * 3. Preloading the initial portion (default 1MB) of a video into the cache
 *    for faster playback startup.
 *
 * 4. Releasing all ExoPlayer instances, cache, and preload coroutines when no longer needed
 *    via the releaseAll() method.
 */

class VideoPlayerManager private constructor(
    private val context: Context,
    private val maxPoolSize: Int = DEFAULT_MAX_POOL_SIZE,
    private val preloadSizeBytes: Int = DEFAULT_PRELOAD_SIZE_BYTES
) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: VideoPlayerManager? = null

        private const val TAG = "VideoPlayerManager"
        private const val CACHE_SIZE_BYTES = 100L * 1024 * 1024 // 100MB
        private const val DEFAULT_MAX_POOL_SIZE = 3
        private const val DEFAULT_PRELOAD_SIZE_BYTES = 1 * 1024 * 1024 // 1MB

        fun getInstance(context: Context): VideoPlayerManager =
            instance ?: synchronized(this) {
                instance ?: VideoPlayerManager(context.applicationContext).also {
                    instance = it
                }
            }
    }

    // Pool thread-safe
    private val playerPool = Collections.synchronizedList(mutableListOf<ExoPlayer>())

    // Cache lazy init
    private val cache by lazy { createCache() }

    // Scope for preload jobs
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @SuppressLint("UnsafeOptInUsageError")
    private fun createCache(): SimpleCache {
        val cacheDir = File(context.cacheDir, "video_cache")
        return SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES),
            StandaloneDatabaseProvider(context)
        )
    }

    @OptIn(UnstableApi::class)
    private fun createDataSourceFactory(): DataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("RankingFilter/1.0")
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)
        val defaultFactory = DefaultDataSource.Factory(context, httpFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Get a player from the pool or create the new one */
    fun getPlayer(): ExoPlayer = synchronized(playerPool) {
        if (playerPool.isNotEmpty()) {
            playerPool.removeAt(0).also { Log.d(TAG, "Reusing player from pool") }
        } else {
            createNewPlayer().also { Log.d(TAG, "Created new player") }
        }
    }

    /** Return the player into the pool or release if the pool is full */
    fun returnPlayer(player: ExoPlayer) = synchronized(playerPool) {
        if (playerPool.size < maxPoolSize) {
            player.stop()
            player.clearMediaItems()
            playerPool.add(player)
            Log.d(TAG, "Player returned. Pool size=${playerPool.size}")
        } else {
            player.release()
            Log.d(TAG, "Player released (pool full)")
        }
    }

    @OptIn(UnstableApi::class)
    private fun createNewPlayer(): ExoPlayer =
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(createDataSourceFactory()))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(500, 3000, 200, 500)
                    .build()
            )
            .build()

    /**
     * Preload the video into cache up to preloadSizeBytes.
     */
    @OptIn(UnstableApi::class)
    fun preloadVideo(url: String): Job = preloadScope.launch {
        var totalBytes = 0
        try {
            val dataSource = createDataSourceFactory().createDataSource()
            val spec = DataSpec(url.toUri())
            dataSource.open(spec)

            val buffer = ByteArray(8 * 1024)
            var read: Int = 0
            while (isActive &&
                dataSource.read(buffer, 0, buffer.size)
                    .also { read = it } != C.RESULT_END_OF_INPUT &&
                totalBytes < preloadSizeBytes
            ) {
                totalBytes += read
            }

            Log.d(TAG, "Preloaded $totalBytes bytes for $url")
        } catch (e: Exception) {
            Log.e(TAG, "Preload failed for $url", e)
        }
    }


    /** Release all of players and cache */
    @OptIn(UnstableApi::class)
    fun releaseAll() {
        synchronized(playerPool) {
            playerPool.forEach { it.release() }
            playerPool.clear()
        }
        cache.release()
        preloadScope.coroutineContext.cancelChildren()
    }
}
