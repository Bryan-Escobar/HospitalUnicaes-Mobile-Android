package sv.edu.unicaes.hospital.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sv.edu.unicaes.hospital.data.local.ArchivosLocales
import sv.edu.unicaes.hospital.data.local.ArchivosLocalesImpl
import sv.edu.unicaes.hospital.data.repository.AuthRepository
import sv.edu.unicaes.hospital.data.repository.AuthRepositoryImpl
import sv.edu.unicaes.hospital.data.repository.CitasRepository
import sv.edu.unicaes.hospital.data.repository.CitasRepositoryImpl
import sv.edu.unicaes.hospital.data.repository.HospitalizacionesRepository
import sv.edu.unicaes.hospital.data.repository.HospitalizacionesRepositoryImpl
import sv.edu.unicaes.hospital.data.repository.LaboratorioRepository
import sv.edu.unicaes.hospital.data.repository.LaboratorioRepositoryImpl
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepositoryImpl
import sv.edu.unicaes.hospital.data.repository.TurnosRepository
import sv.edu.unicaes.hospital.data.repository.TurnosRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindHospitalizacionesRepository(impl: HospitalizacionesRepositoryImpl): HospitalizacionesRepository

    @Binds
    @Singleton
    abstract fun bindCitasRepository(impl: CitasRepositoryImpl): CitasRepository

    @Binds
    @Singleton
    abstract fun bindTurnosRepository(impl: TurnosRepositoryImpl): TurnosRepository

    @Binds
    @Singleton
    abstract fun bindLaboratorioRepository(impl: LaboratorioRepositoryImpl): LaboratorioRepository

    @Binds
    @Singleton
    abstract fun bindArchivosLocales(impl: ArchivosLocalesImpl): ArchivosLocales
}
