package com.bughunter.core.data.repository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RepositoryModule {

    // A monotonic-ish epoch-ms clock injected into repos & ViewModels that stamp
    // timestamps (chat turns, cache fetched-at). Constructor defaults look like
    // they would suffice, but Hilt ignores Kotlin default values — it needs an
    // explicit @Provides for kotlin.jvm.functions.Function0<Long>.
    @Provides
    @Singleton
    fun provideClock(): () -> Long = System::currentTimeMillis
}
