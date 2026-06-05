package com.bughunter.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserAgentInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val builder = req.newBuilder()
            .header("User-Agent", USER_AGENT)
        if (req.header("Accept") == null) {
            builder.header("Accept", "application/json")
        }
        return chain.proceed(builder.build())
    }

    companion object {
        const val USER_AGENT = "Bug Hunter Android/2.8.0"
    }
}
