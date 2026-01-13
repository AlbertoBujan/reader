package com.example.inoreaderlite.di

import android.content.Context
import androidx.room.Room
import com.example.inoreaderlite.data.local.AppDatabase
import com.example.inoreaderlite.data.local.dao.FeedDao
import com.example.inoreaderlite.data.remote.FeedService
import com.example.inoreaderlite.data.remote.RssParser
import com.example.inoreaderlite.data.repository.FeedRepositoryImpl
import com.example.inoreaderlite.domain.repository.FeedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "inoreader_lite.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideFeedDao(db: AppDatabase): FeedDao = db.feedDao()

    @Provides
    @Singleton
    fun provideRssParser(): RssParser = RssParser()

    @Provides
    @Singleton
    fun provideFeedService(): FeedService {
        return Retrofit.Builder()
            .baseUrl("https://localhost/") // Dummy Base URL as we use dynamic @Url
            .client(OkHttpClient.Builder().build())
            .build()
            .create(FeedService::class.java)
    }

    @Provides
    @Singleton
    fun provideFeedRepository(
        feedDao: FeedDao,
        feedService: FeedService,
        rssParser: RssParser
    ): FeedRepository {
        return FeedRepositoryImpl(feedDao, feedService, rssParser)
    }
}
