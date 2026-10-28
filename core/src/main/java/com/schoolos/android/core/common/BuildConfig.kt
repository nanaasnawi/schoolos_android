package com.schoolos.android.core.common

object BuildConfig {
    @Volatile
    var API_BASE_URL: String = "https://schoolosbackend-production.up.railway.app/api/v1/"
    val DEBUG: Boolean = com.schoolos.android.core.BuildConfig.DEBUG
    @Volatile
    var APP_VERSION: String = "1.2.8"
}

