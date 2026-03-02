package com.khadr.forge.features.reminders.di

import com.khadr.forge.core.data.ForgeDatabase
import com.khadr.forge.features.reminders.data.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReminderModule {

    @Provides
    @Singleton
    fun provideReminderDao(db: ForgeDatabase): ReminderDao = db.reminderDao()
}