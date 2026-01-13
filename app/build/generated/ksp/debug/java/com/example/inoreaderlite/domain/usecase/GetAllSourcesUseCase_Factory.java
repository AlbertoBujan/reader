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
public final class GetAllSourcesUseCase_Factory implements Factory<GetAllSourcesUseCase> {
  private final Provider<FeedRepository> repositoryProvider;

  public GetAllSourcesUseCase_Factory(Provider<FeedRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetAllSourcesUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetAllSourcesUseCase_Factory create(Provider<FeedRepository> repositoryProvider) {
    return new GetAllSourcesUseCase_Factory(repositoryProvider);
  }

  public static GetAllSourcesUseCase newInstance(FeedRepository repository) {
    return new GetAllSourcesUseCase(repository);
  }
}
