package com.bughunter.core.network

import okhttp3.Cookie
import okhttp3.HttpUrl

interface CookiePersistence {
    fun loadAll(): List<Cookie>
    fun saveAll(cookies: List<Cookie>)
    fun mergeAndSave(host: String, fresh: List<Cookie>)
    fun clear()
    fun matching(url: HttpUrl): List<Cookie>
}
