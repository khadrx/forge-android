package com.khadr.forge.core.data.di

import android.content.Context
import androidx.room.Room
import com.khadr.forge.core.data.ForgeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideForgeDatabase(
        @ApplicationContext context: Context
    ): ForgeDatabase = Room.databaseBuilder(
        context,
        ForgeDatabase::class.java,
        ForgeDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()
}