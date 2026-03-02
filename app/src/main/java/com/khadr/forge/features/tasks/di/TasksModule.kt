package com.khadr.forge.features.tasks.di

import com.khadr.forge.core.data.ForgeDatabase
import com.khadr.forge.features.tasks.data.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TaskModule {

    @Provides
    @Singleton
    fun provideTaskDao(db: ForgeDatabase): TaskDao = db.taskDao()
}