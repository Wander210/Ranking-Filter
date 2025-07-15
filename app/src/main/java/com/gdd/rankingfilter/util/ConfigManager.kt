package com.gdd.rankingfilter.util

import android.content.Context
import android.util.Log
import com.gdd.rankingfilter.data.model.CloudinaryConfig
import com.google.gson.Gson

class ConfigManager(private val context: Context) {

    fun getCloudinaryConfig(): CloudinaryConfig? {
        return try {
            val configJson = context.assets.open("config.json").bufferedReader().use { it.readText() }
            Gson().fromJson(configJson, CloudinaryConfig::class.java)
        } catch (e: Exception) {
            Log.e("ConfigManager", "Error reading config", e)
            null
        }
    }
}