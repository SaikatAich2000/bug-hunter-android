package com.bughunter.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CsrfInterceptor @Inject constructor(
    private val cookieJar: EncryptedCookieJar,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val method = req.method
        if (method !in MUTATING_METHODS) return chain.proceed(req)

        val path = req.url.encodedPath
        if (path in EXEMPT_PATHS) return chain.proceed(req)

        val csrf = cookieJar.csrfToken() ?: return chain.proceed(req)
        val withHeader = req.newBuilder().header(CSRF_HEADER, csrf).build()
        return chain.proceed(withHeader)
    }

    companion object {
        const val CSRF_HEADER = "X-CSRF-Token"
        private val MUTATING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private val EXEMPT_PATHS = setOf(
            "/api/auth/login",
            "/api/auth/login/totp",
            "/api/auth/signup",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/invitations/accept",
            "/api/health",
        )
    }
}
