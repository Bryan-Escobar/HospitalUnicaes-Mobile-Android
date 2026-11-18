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

    /** Hospitalizaciones en curso que la API deja ver; al doctor, las de los pacientes que atiende. */
    suspend fun activas(): Resultado<List<Hospitalizacion>>
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

        override suspend fun activas(): Resultado<List<Hospitalizacion>> =
            safeApiCall {
                api
                    .listarHospitalizaciones(null, estado = "activa", porPagina = MAXIMO_POR_PAGINA)
                    .data.items
                    .map { it.aDominio() }
            }

        private companion object {
            /** Límite de `por_pagina` en la API; hay 17 camas, así que cabe en una página. */
            const val MAXIMO_POR_PAGINA = 100
        }
    }
