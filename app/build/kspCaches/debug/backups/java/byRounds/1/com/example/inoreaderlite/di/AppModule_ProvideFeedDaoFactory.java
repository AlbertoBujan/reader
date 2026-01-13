package com.example.inoreaderlite.di;

import com.example.inoreaderlite.data.local.AppDatabase;
import com.example.inoreaderlite.data.local.dao.FeedDao;
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
public final class AppModule_ProvideFeedDaoFactory implements Factory<FeedDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideFeedDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public FeedDao get() {
    return provideFeedDao(dbProvider.get());
  }

  public static AppModule_ProvideFeedDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideFeedDaoFactory(dbProvider);
  }

  public static FeedDao provideFeedDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFeedDao(db));
  }
}
