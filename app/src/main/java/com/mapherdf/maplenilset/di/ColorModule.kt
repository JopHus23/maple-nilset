package com.mapherdf.maplenilset.di

import com.mapherdf.maplenilset.data.repository.ColorRepository
import com.mapherdf.maplenilset.data.repository.LocalColorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ColorModule {

    @Binds
    abstract fun bindColorRepository(impl: LocalColorRepository): ColorRepository
}
