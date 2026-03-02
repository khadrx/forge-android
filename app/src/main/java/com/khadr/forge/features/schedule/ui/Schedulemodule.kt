package com.khadr.forge.features.schedule.di

import com.khadr.forge.core.data.ForgeDatabase
import com.khadr.forge.features.schedule.data.EventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScheduleModule {

    @Provides
    @Singleton
    fun provideEventDao(db: ForgeDatabase): EventDao = db.eventDao()
}