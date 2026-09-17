package info.kronk.feature.home.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import info.kronk.core.network.api.TimelinesApi
import info.kronk.feature.auth.data.AuthStorage
import info.kronk.feature.home.data.HomeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {
    @Provides
    @Singleton
    fun provideHomeRepository(
        timelines: TimelinesApi,
        storage: AuthStorage,
    ): HomeRepository = HomeRepository(timelines, storage)
}
