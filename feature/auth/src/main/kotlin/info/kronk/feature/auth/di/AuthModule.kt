package info.kronk.feature.auth.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import info.kronk.core.network.KronkRetrofit
import info.kronk.core.network.api.AccountsApi
import info.kronk.core.network.api.AppsApi
import info.kronk.core.network.api.OAuthApi
import info.kronk.core.network.api.TimelinesApi
import info.kronk.core.network.create
import info.kronk.feature.auth.data.AuthRepository
import info.kronk.feature.auth.data.AuthStorage
import info.kronk.feature.auth.network.AuthInterceptor
import javax.inject.Singleton

// Hilt module for auth. Also owns the KronkRetrofit provider because
// auth is the first consumer + KronkRetrofit is a small central
// object. If/when a second cross-cutting network consumer appears
// that :feature:auth doesn't depend on, promote the Retrofit provider
// to :app's own module.

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthStorage(@ApplicationContext context: Context): AuthStorage =
        AuthStorage(context)

    @Provides
    @Singleton
    fun provideAuthInterceptor(storage: AuthStorage): AuthInterceptor =
        AuthInterceptor(storage)

    @Provides
    @Singleton
    fun provideKronkRetrofit(interceptor: AuthInterceptor): KronkRetrofit =
        KronkRetrofit(
            // Debug/release toggle for OkHttp logging. Read BuildConfig
            // from :app in a follow-up; for now, always off to keep
            // this module free of :app dep.
            debug = false,
            extraInterceptors = listOf(interceptor),
        )

    @Provides
    @Singleton
    fun provideAppsApi(retrofit: KronkRetrofit): AppsApi = retrofit.create()

    @Provides
    @Singleton
    fun provideOAuthApi(retrofit: KronkRetrofit): OAuthApi = retrofit.create()

    @Provides
    @Singleton
    fun provideAccountsApi(retrofit: KronkRetrofit): AccountsApi = retrofit.create()

    @Provides
    @Singleton
    fun provideTimelinesApi(retrofit: KronkRetrofit): TimelinesApi = retrofit.create()

    @Provides
    @Singleton
    fun provideAuthRepository(
        storage: AuthStorage,
        apps: AppsApi,
        oauth: OAuthApi,
        accounts: AccountsApi,
    ): AuthRepository = AuthRepository(storage, apps, oauth, accounts)
}
