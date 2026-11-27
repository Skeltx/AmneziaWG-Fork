package com.amneziawg.fork.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.vpn.RoutingEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun database(): AmneziaDatabase
    fun dataStore(): DataStore<Preferences>
    fun routingEngine(): Provider<RoutingEngine>
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceEntryPoint {
    fun database(): AmneziaDatabase
    fun routingEngine(): RoutingEngine
}
