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
public final class GetArticlesUseCase_Factory implements Factory<GetArticlesUseCase> {
  private final Provider<FeedRepository> repositoryProvider;

  public GetArticlesUseCase_Factory(Provider<FeedRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetArticlesUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetArticlesUseCase_Factory create(Provider<FeedRepository> repositoryProvider) {
    return new GetArticlesUseCase_Factory(repositoryProvider);
  }

  public static GetArticlesUseCase newInstance(FeedRepository repository) {
    return new GetArticlesUseCase(repository);
  }
}
