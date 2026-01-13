package com.example.inoreaderlite.ui.viewmodel;

import com.example.inoreaderlite.domain.usecase.AddSourceUseCase;
import com.example.inoreaderlite.domain.usecase.GetArticlesUseCase;
import com.example.inoreaderlite.domain.usecase.SyncFeedsUseCase;
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<GetArticlesUseCase> getArticlesUseCaseProvider;

  private final Provider<AddSourceUseCase> addSourceUseCaseProvider;

  private final Provider<SyncFeedsUseCase> syncFeedsUseCaseProvider;

  public MainViewModel_Factory(Provider<GetArticlesUseCase> getArticlesUseCaseProvider,
      Provider<AddSourceUseCase> addSourceUseCaseProvider,
      Provider<SyncFeedsUseCase> syncFeedsUseCaseProvider) {
    this.getArticlesUseCaseProvider = getArticlesUseCaseProvider;
    this.addSourceUseCaseProvider = addSourceUseCaseProvider;
    this.syncFeedsUseCaseProvider = syncFeedsUseCaseProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(getArticlesUseCaseProvider.get(), addSourceUseCaseProvider.get(), syncFeedsUseCaseProvider.get());
  }

  public static MainViewModel_Factory create(
      Provider<GetArticlesUseCase> getArticlesUseCaseProvider,
      Provider<AddSourceUseCase> addSourceUseCaseProvider,
      Provider<SyncFeedsUseCase> syncFeedsUseCaseProvider) {
    return new MainViewModel_Factory(getArticlesUseCaseProvider, addSourceUseCaseProvider, syncFeedsUseCaseProvider);
  }

  public static MainViewModel newInstance(GetArticlesUseCase getArticlesUseCase,
      AddSourceUseCase addSourceUseCase, SyncFeedsUseCase syncFeedsUseCase) {
    return new MainViewModel(getArticlesUseCase, addSourceUseCase, syncFeedsUseCase);
  }
}
