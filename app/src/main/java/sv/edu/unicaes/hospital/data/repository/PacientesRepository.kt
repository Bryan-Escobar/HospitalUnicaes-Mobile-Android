package sv.edu.unicaes.hospital.data.repository

import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeApiCall
import sv.edu.unicaes.hospital.data.remote.HospitalApi
import sv.edu.unicaes.hospital.data.remote.dto.aBody
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Pagina
import javax.inject.Inject
import javax.inject.Singleton

interface PacientesRepository {
    suspend fun listar(
        buscar: String?,
        pagina: Int,
    ): Resultado<Pagina<Paciente>>

    suspend fun obtener(id: String): Resultado<Paciente>

    suspend fun crear(datos: DatosPaciente): Resultado<Paciente>

    suspend fun actualizar(
        id: String,
        datos: DatosPaciente,
    ): Resultado<Paciente>
}

@Singleton
class PacientesRepositoryImpl
    @Inject
    constructor(
        private val api: HospitalApi,
    ) : PacientesRepository {
        override suspend fun listar(
            buscar: String?,
            pagina: Int,
        ): Resultado<Pagina<Paciente>> =
            safeApiCall {
                api.listarPacientes(buscar, pagina, POR_PAGINA).data.aDominio { it.aDominio() }
            }

        override suspend fun obtener(id: String): Resultado<Paciente> =
            safeApiCall { api.obtenerPaciente(id).data.aDominio() }

        override suspend fun crear(datos: DatosPaciente): Resultado<Paciente> =
            safeApiCall { api.crearPaciente(datos.aBody()).data.aDominio() }

        override suspend fun actualizar(
            id: String,
            datos: DatosPaciente,
        ): Resultado<Paciente> = safeApiCall { api.actualizarPaciente(id, datos.aBody()).data.aDominio() }

        private companion object {
            const val POR_PAGINA = 20
        }
    }
