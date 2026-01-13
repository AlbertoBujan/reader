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
public final class MarkArticleReadUseCase_Factory implements Factory<MarkArticleReadUseCase> {
  private final Provider<FeedRepository> repositoryProvider;

  public MarkArticleReadUseCase_Factory(Provider<FeedRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public MarkArticleReadUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static MarkArticleReadUseCase_Factory create(Provider<FeedRepository> repositoryProvider) {
    return new MarkArticleReadUseCase_Factory(repositoryProvider);
  }

  public static MarkArticleReadUseCase newInstance(FeedRepository repository) {
    return new MarkArticleReadUseCase(repository);
  }
}
