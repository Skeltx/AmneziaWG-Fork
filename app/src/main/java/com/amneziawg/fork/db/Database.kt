package com.amneziawg.fork.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amneziawg.fork.db.converters.RoutingRuleTypeConverter
import com.amneziawg.fork.model.RoutingConfig

@Database(
    entities = [
        RoutingConfigEntity::class,
        RoutingRuleEntity::class,
        AppRoutingProfileEntity::class,
        DomainRoutingProfileEntity::class,
        VpnConnectionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoutingRuleTypeConverter::class)
abstract class AmneziaDatabase : RoomDatabase() {

    abstract fun routingConfigDao(): RoutingConfigDao
    abstract fun routingRuleDao(): RoutingRuleDao
    abstract fun appRoutingProfileDao(): AppRoutingProfileDao
    abstract fun domainRoutingProfileDao(): DomainRoutingProfileDao
    abstract fun vpnConnectionLogDao(): VpnConnectionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AmneziaDatabase? = null

        fun getInstance(context: Context): AmneziaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmneziaDatabase::class.java,
                    "amneziawg_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE?.let {
                it.close()
                INSTANCE = null
            }
        }
    }
}