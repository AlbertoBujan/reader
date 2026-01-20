package com.example.riffle.di

import android.content.Context
import androidx.room.Room
import com.example.riffle.data.local.AppDatabase
import com.example.riffle.data.local.dao.FeedDao
import com.example.riffle.data.remote.ClearbitService
import com.example.riffle.data.remote.FeedService
import com.example.riffle.data.remote.FeedSearchService
import com.example.riffle.data.remote.RssParser
import com.example.riffle.data.repository.FeedRepositoryImpl
import com.example.riffle.domain.repository.FeedRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideFeedDao(db: AppDatabase): FeedDao = db.feedDao()

    @Provides
    @Singleton
    fun provideRssParser(): RssParser = RssParser()

    @Provides
    @Singleton
    fun provideUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        return OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    @Provides
    @Singleton
    fun provideFeedService(client: OkHttpClient): FeedService {
        return Retrofit.Builder()
            .baseUrl("https://localhost/") // Dummy Base URL as we use dynamic @Url
            .client(client)
            .build()
            .create(FeedService::class.java)
    }

    @Provides
    @Singleton
    fun provideClearbitService(client: OkHttpClient): ClearbitService {
        return Retrofit.Builder()
            .baseUrl("https://autocomplete.clearbit.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClearbitService::class.java)
    }

    @Provides
    @Singleton
    fun provideFeedSearchService(client: OkHttpClient): FeedSearchService {
        return Retrofit.Builder()
            .baseUrl("https://feedsearch.dev/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FeedSearchService::class.java)
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
