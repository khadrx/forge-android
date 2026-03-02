package com.khadr.forge.features.budget.di

import com.khadr.forge.core.data.ForgeDatabase
import com.khadr.forge.features.budget.data.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BudgetModule {

    @Provides
    @Singleton
    fun provideTransactionDao(db: ForgeDatabase): TransactionDao = db.transactionDao()
}