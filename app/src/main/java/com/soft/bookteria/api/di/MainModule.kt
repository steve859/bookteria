package com.soft.bookteria.api.di

import android.content.Context
import com.soft.bookteria.api.BookApi
import com.soft.bookteria.database.BookteriaDatabase
import com.soft.bookteria.database.library.LibraryDAO
import com.soft.bookteria.database.progress.ProgressDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MainModule {
    @Singleton
    @Provides
    fun provideBookApi(@ApplicationContext context: Context): BookApi{
        return BookApi(context)
    }
    
    @Singleton
    @Provides
    fun provideBookteriaDatabase(@ApplicationContext context: Context) = BookteriaDatabase.getInstance(context)
    
    @Singleton
    @Provides
    fun provideLibraryDAO(bookteriaDatabase: BookteriaDatabase): LibraryDAO {
        return bookteriaDatabase.libraryDAO()
    }
    
    @Singleton
    @Provides
    fun provideProgressDAO(bookteriaDatabase: BookteriaDatabase): ProgressDAO {
        return bookteriaDatabase.progressDAO()
    }

}