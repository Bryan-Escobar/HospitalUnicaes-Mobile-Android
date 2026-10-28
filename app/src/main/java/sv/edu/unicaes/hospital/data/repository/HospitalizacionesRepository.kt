package sv.edu.unicaes.hospital.data.repository

import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeApiCall
import sv.edu.unicaes.hospital.data.remote.HospitalApi
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.Hospitalizacion
import javax.inject.Inject
import javax.inject.Singleton

interface HospitalizacionesRepository {
    /** La hospitalización en curso del paciente, o null si no está internado. */
    suspend fun activaDe(pacienteId: String): Resultado<Hospitalizacion?>
}

@Singleton
class HospitalizacionesRepositoryImpl
    @Inject
    constructor(
        private val api: HospitalApi,
    ) : HospitalizacionesRepository {
        // Un paciente tiene a lo sumo una hospitalización activa (la API responde 409 si se intenta otra).
        override suspend fun activaDe(pacienteId: String): Resultado<Hospitalizacion?> =
            safeApiCall {
                api
                    .listarHospitalizaciones(pacienteId, estado = "activa", porPagina = 1)
                    .data.items
                    .firstOrNull()
                    ?.aDominio()
            }
    }
