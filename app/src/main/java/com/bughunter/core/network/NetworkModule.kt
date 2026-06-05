package com.bughunter.core.network

import android.content.SharedPreferences
import com.bughunter.BuildConfig
import com.bughunter.core.data.local.CookiePrefs
import com.bughunter.core.network.api.AuditApi
import com.bughunter.core.network.api.AuthApi
import com.bughunter.core.network.api.BrandingApi
import com.bughunter.core.network.api.BugsApi
import com.bughunter.core.network.api.ChatApi
import com.bughunter.core.network.api.CustomFieldsApi
import com.bughunter.core.network.api.EventsApi
import com.bughunter.core.network.api.InvitationsApi
import com.bughunter.core.network.api.MembershipsApi
import com.bughunter.core.network.api.MetaApi
import com.bughunter.core.network.api.OrganizationApi
import com.bughunter.core.network.api.ProjectsApi
import com.bughunter.core.network.api.SavedViewsApi
import com.bughunter.core.network.api.SessionsApi
import com.bughunter.core.network.api.StatsApi
import com.bughunter.core.network.api.TotpApi
import com.bughunter.core.network.api.UsersApi
import com.bughunter.core.network.api.WebhooksApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideCookiePersistence(@CookiePrefs prefs: SharedPreferences): CookiePersistence =
        EncryptedCookiePersistence(prefs)

    @Provides
    @Singleton
    fun provideEncryptedCookieJar(persistence: CookiePersistence): EncryptedCookieJar =
        EncryptedCookieJar(persistence)

    @Provides
    @Singleton
    fun provideCookieJar(jar: EncryptedCookieJar): CookieJar = jar

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        return HttpLoggingInterceptor().apply {
            this.level = level
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            redactHeader("X-CSRF-Token")
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: CookieJar,
        userAgentInterceptor: UserAgentInterceptor,
        csrfInterceptor: CsrfInterceptor,
        authInterceptor: AuthInterceptor,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(csrfInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        baseUrlProvider: BaseUrlProvider,
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(runCatching { baseUrlProvider.currentBlocking() }.getOrDefault(BaseUrlProvider.DEFAULT_BASE_URL))
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides @Singleton fun provideMetaApi(retrofit: Retrofit): MetaApi = retrofit.create(MetaApi::class.java)

    @Provides @Singleton fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton fun provideTotpApi(retrofit: Retrofit): TotpApi = retrofit.create(TotpApi::class.java)

    @Provides @Singleton fun provideOrganizationApi(retrofit: Retrofit): OrganizationApi =
        retrofit.create(OrganizationApi::class.java)

    @Provides @Singleton fun provideBrandingApi(retrofit: Retrofit): BrandingApi =
        retrofit.create(BrandingApi::class.java)

    @Provides @Singleton fun provideUsersApi(retrofit: Retrofit): UsersApi = retrofit.create(UsersApi::class.java)

    @Provides @Singleton fun provideInvitationsApi(retrofit: Retrofit): InvitationsApi =
        retrofit.create(InvitationsApi::class.java)

    @Provides @Singleton fun provideProjectsApi(retrofit: Retrofit): ProjectsApi =
        retrofit.create(ProjectsApi::class.java)

    @Provides @Singleton fun provideMembershipsApi(retrofit: Retrofit): MembershipsApi =
        retrofit.create(MembershipsApi::class.java)

    @Provides @Singleton fun provideBugsApi(retrofit: Retrofit): BugsApi = retrofit.create(BugsApi::class.java)

    @Provides @Singleton fun provideEventsApi(retrofit: Retrofit): EventsApi = retrofit.create(EventsApi::class.java)

    @Provides @Singleton fun provideStatsApi(retrofit: Retrofit): StatsApi = retrofit.create(StatsApi::class.java)

    @Provides @Singleton fun provideSavedViewsApi(retrofit: Retrofit): SavedViewsApi =
        retrofit.create(SavedViewsApi::class.java)

    @Provides @Singleton fun provideCustomFieldsApi(retrofit: Retrofit): CustomFieldsApi =
        retrofit.create(CustomFieldsApi::class.java)

    @Provides @Singleton fun provideAuditApi(retrofit: Retrofit): AuditApi = retrofit.create(AuditApi::class.java)

    @Provides @Singleton fun provideSessionsApi(retrofit: Retrofit): SessionsApi =
        retrofit.create(SessionsApi::class.java)

    @Provides @Singleton fun provideWebhooksApi(retrofit: Retrofit): WebhooksApi =
        retrofit.create(WebhooksApi::class.java)

    @Provides @Singleton fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)
}
