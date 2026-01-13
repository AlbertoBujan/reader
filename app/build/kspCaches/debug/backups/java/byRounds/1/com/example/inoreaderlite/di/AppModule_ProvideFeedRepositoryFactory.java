package com.example.inoreaderlite.di;

import com.example.inoreaderlite.data.local.dao.FeedDao;
import com.example.inoreaderlite.data.remote.FeedService;
import com.example.inoreaderlite.data.remote.RssParser;
import com.example.inoreaderlite.domain.repository.FeedRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class AppModule_ProvideFeedRepositoryFactory implements Factory<FeedRepository> {
  private final Provider<FeedDao> feedDaoProvider;

  private final Provider<FeedService> feedServiceProvider;

  private final Provider<RssParser> rssParserProvider;

  public AppModule_ProvideFeedRepositoryFactory(Provider<FeedDao> feedDaoProvider,
      Provider<FeedService> feedServiceProvider, Provider<RssParser> rssParserProvider) {
    this.feedDaoProvider = feedDaoProvider;
    this.feedServiceProvider = feedServiceProvider;
    this.rssParserProvider = rssParserProvider;
  }

  @Override
  public FeedRepository get() {
    return provideFeedRepository(feedDaoProvider.get(), feedServiceProvider.get(), rssParserProvider.get());
  }

  public static AppModule_ProvideFeedRepositoryFactory create(Provider<FeedDao> feedDaoProvider,
      Provider<FeedService> feedServiceProvider, Provider<RssParser> rssParserProvider) {
    return new AppModule_ProvideFeedRepositoryFactory(feedDaoProvider, feedServiceProvider, rssParserProvider);
  }

  public static FeedRepository provideFeedRepository(FeedDao feedDao, FeedService feedService,
      RssParser rssParser) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFeedRepository(feedDao, feedService, rssParser));
  }
}
