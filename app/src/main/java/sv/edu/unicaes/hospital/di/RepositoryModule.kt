package sv.edu.unicaes.hospital.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sv.edu.unicaes.hospital.data.repository.AuthRepository
import sv.edu.unicaes.hospital.data.repository.AuthRepositoryImpl
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPacientesRepository(impl: PacientesRepositoryImpl): PacientesRepository
}
