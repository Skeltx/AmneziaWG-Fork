package com.amneziawg.fork.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.amneziawg.fork.AmneziaWGApplication
import com.amneziawg.fork.BuildConfig
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.model.RoutingAction
import com.amneziawg.fork.model.RoutingConfig
import com.amneziawg.fork.model.WireGuardConfig
import com.amneziawg.fork.vpn.RoutingEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AmneziaDatabase =
        AmneziaDatabase.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("amneziawg_prefs") }
        )
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
}

@Module
@InstallIn(SingletonComponent::class)
object VpnModule {

    @Provides
    @Singleton
    fun provideRoutingEngine(
        @ApplicationContext context: Context,
        database: AmneziaDatabase
    ): RoutingEngine = RoutingEngine(context, defaultConfig())

    private fun defaultConfig(): RoutingConfig = RoutingConfig(
        name = "Default",
        wireguardConfig = WireGuardConfig(
            name = "Default",
            privateKey = "",
            publicKey = "",
            address = emptyList(),
            endpoint = ""
        ),
        defaultAction = RoutingAction.ROUTE_VIA_VPN
    )
}

@Module
@InstallIn(SingletonComponent::class)
interface ApplicationModule {

    @Binds
    @Singleton
    fun bindApplication(impl: AmneziaWGApplication): Application
}
