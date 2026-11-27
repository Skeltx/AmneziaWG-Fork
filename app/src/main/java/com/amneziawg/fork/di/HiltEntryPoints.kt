package com.amneziawg.fork.di

import android.content.Context
import androidx.datastore.preferences.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.vpn.RoutingEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Provider

@EntryPoint
@InstallIn(ApplicationComponent::class)
interface AppEntryPoint {
    fun database(): AmneziaDatabase
    fun dataStore(): DataStore<Preferences>
    fun routingEngine(): Provider<RoutingEngine>
}

@EntryPoint
@InstallIn(ApplicationComponent::class)
interface ServiceEntryPoint {
    fun database(): AmneziaDatabase
    fun routingEngine(): RoutingEngine
}