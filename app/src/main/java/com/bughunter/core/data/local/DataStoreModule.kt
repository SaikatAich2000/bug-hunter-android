package com.bughunter.core.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier annotation class AppPrefsStore
@Qualifier annotation class AuthPrefsStore
@Qualifier annotation class CookiePrefs

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    @Provides
    @Singleton
    @AppPrefsStore
    fun provideAppPrefsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appPrefsDataStore

    @Provides
    @Singleton
    @AuthPrefsStore
    fun provideAuthPrefsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.authPrefsDataStore

    @Provides
    @Singleton
    fun provideAppPrefs(@AppPrefsStore store: DataStore<Preferences>): AppPrefs = AppPrefs(store)

    @Provides
    @Singleton
    fun provideAuthPrefs(@AuthPrefsStore store: DataStore<Preferences>): AuthPrefs = AuthPrefs(store)

    @Provides
    @Singleton
    @CookiePrefs
    fun provideCookieSharedPrefs(@ApplicationContext context: Context): SharedPreferences =
        EncryptedPrefs.cookies(context)
}
