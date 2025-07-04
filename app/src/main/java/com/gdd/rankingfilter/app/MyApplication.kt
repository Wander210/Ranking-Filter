package com.gdd.rankingfilter.app

import android.app.Application
import com.gdd.rankingfilter.preference.MyPreferences

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MyPreferences.init(this)
    }
}