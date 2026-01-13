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
public final class AddSourceUseCase_Factory implements Factory<AddSourceUseCase> {
  private final Provider<FeedRepository> repositoryProvider;

  public AddSourceUseCase_Factory(Provider<FeedRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public AddSourceUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static AddSourceUseCase_Factory create(Provider<FeedRepository> repositoryProvider) {
    return new AddSourceUseCase_Factory(repositoryProvider);
  }

  public static AddSourceUseCase newInstance(FeedRepository repository) {
    return new AddSourceUseCase(repository);
  }
}
