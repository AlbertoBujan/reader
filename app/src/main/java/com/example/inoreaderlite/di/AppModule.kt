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
    fun provideFeedService(): FeedService {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://localhost/") // Dummy Base URL as we use dynamic @Url
            .client(client)
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
