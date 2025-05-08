package com.example.notaflow.di

import com.example.notaflow.data.preferences.UserPreferencesRepository
import com.example.notaflow.ui.theme.ThemeViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object ViewModelModule {

    @Provides
    @ActivityRetainedScoped
    fun provideThemeViewModel(
        userPreferencesRepository: UserPreferencesRepository
    ): ThemeViewModel {
        return ThemeViewModel(userPreferencesRepository)
    }
}