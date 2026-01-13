package com.example.inoreaderlite.domain.usecase;

import com.example.inoreaderlite.domain.repository.FeedRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SyncFeedsUseCase_Factory implements Factory<SyncFeedsUseCase> {
  private final Provider<FeedRepository> repositoryProvider;

  public SyncFeedsUseCase_Factory(Provider<FeedRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SyncFeedsUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static SyncFeedsUseCase_Factory create(Provider<FeedRepository> repositoryProvider) {
    return new SyncFeedsUseCase_Factory(repositoryProvider);
  }

  public static SyncFeedsUseCase newInstance(FeedRepository repository) {
    return new SyncFeedsUseCase(repository);
  }
}
