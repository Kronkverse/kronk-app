package info.kronk.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import info.kronk.app.ui.hub.HubRepository
import info.kronk.core.network.api.KornersApi
import info.kronk.feature.auth.data.AuthStorage
import javax.inject.Singleton

// :app-owned providers. Repositories for screens that live in :app
// (Hub for now) go here. When a screen graduates into its own
// :feature:<name> module the provider moves with it.

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHubRepository(
        korners: KornersApi,
        storage: AuthStorage,
    ): HubRepository = HubRepository(korners, storage)
}
