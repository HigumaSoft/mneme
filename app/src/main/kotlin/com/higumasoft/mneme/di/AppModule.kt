package com.higumasoft.mneme.di

import com.higumasoft.mneme.data.drive.DriveSync
import com.higumasoft.mneme.data.drive.NoOpDriveSync
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDriveSync(impl: NoOpDriveSync): DriveSync
}
