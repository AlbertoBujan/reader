package com.example.inoreaderlite.di;

import com.example.inoreaderlite.data.remote.RssParser;
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
public final class AppModule_ProvideRssParserFactory implements Factory<RssParser> {
  @Override
  public RssParser get() {
    return provideRssParser();
  }

  public static AppModule_ProvideRssParserFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RssParser provideRssParser() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideRssParser());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideRssParserFactory INSTANCE = new AppModule_ProvideRssParserFactory();
  }
}
