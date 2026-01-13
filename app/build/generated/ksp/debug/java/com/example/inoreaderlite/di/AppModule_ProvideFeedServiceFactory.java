package com.example.inoreaderlite.di;

import com.example.inoreaderlite.data.remote.FeedService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideFeedServiceFactory implements Factory<FeedService> {
  @Override
  public FeedService get() {
    return provideFeedService();
  }

  public static AppModule_ProvideFeedServiceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FeedService provideFeedService() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFeedService());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideFeedServiceFactory INSTANCE = new AppModule_ProvideFeedServiceFactory();
  }
}
