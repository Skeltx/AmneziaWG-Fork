package com.amneziawg.fork.di

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.vpn.AmneziaVPNService
import com.amneziawg.fork.vpn.RoutingEngine
import com.google.common.util.concurrent.ListenableFuture
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
abstract class DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AmneziaDatabase {
        return AmneziaDatabase.getInstance(context)
    }
}

@Module
@InstallIn(ApplicationComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): androidx.datastore.preferences.DataStore<Preferences> {
        return context.preferencesDataStore("amneziawg_prefs")
    }
}

@Module
@InstallIn(ApplicationComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) okhttp3.logging.HttpLoggingInterceptor.Level.BODY else okhttp3.logging.HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }
}

@Module
@InstallIn(ApplicationComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    @dagger.hilt.android.qualifiers.IO
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @dagger.hilt.android.qualifiers.Main
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    @dagger.hilt.android.qualifiers.Default
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(ApplicationComponent::class)
abstract class WorkerModule {

    @Binds
    abstract fun bindWorkerFactory(factory: HiltWorkerFactory): Configuration.Provider
}

@Module
@InstallIn(ApplicationComponent::class)
object VpnModule {

    @Provides
    @Singleton
    fun provideRoutingEngine(
        @ApplicationContext context: Context,
        database: AmneziaDatabase
    ): RoutingEngine {
        // The active config will be set later
        return RoutingEngine(context, getDefaultConfig())
    }

    private fun getDefaultConfig(): com.amneziawg.fork.model.RoutingConfig {
        return com.amneziawg.fork.model.RoutingConfig(
            name = "Default",
            wireguardConfig = com.amneziawg.fork.model.WireGuardConfig(
                name = "Default",
                privateKey = "",
                publicKey = "",
                address = listOf(),
                endpoint = ""
            ),
            defaultAction = com.amneziawg.fork.model.RoutingAction.ROUTE_VIA_VPN
        )
    }
}

@Module
@InstallIn(ServiceComponent::class)
object VpnServiceModule {

    @Provides
    fun provideVpnService(): AmneziaVPNService {
        // This is provided by the system
        throw UnsupportedOperationException("VPN Service is created by system")
    }
}

@Module
@InstallIn(ApplicationComponent::class)
interface ApplicationModule {

    @Binds
    @Singleton
    fun bindApplication(impl: AmneziaWGApplication): Application
}