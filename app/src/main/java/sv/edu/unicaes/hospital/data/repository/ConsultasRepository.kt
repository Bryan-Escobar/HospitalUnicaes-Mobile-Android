package sv.edu.unicaes.hospital.data.repository

import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.CODIGO_CONSULTA_CERRADA
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeApiCall
import sv.edu.unicaes.hospital.data.remote.HospitalApi
import sv.edu.unicaes.hospital.data.remote.dto.CrearDiagnosticoBody
import sv.edu.unicaes.hospital.data.remote.dto.TIPO_PRINCIPAL
import sv.edu.unicaes.hospital.data.remote.dto.aBody
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.HistorialClinico
import sv.edu.unicaes.hospital.domain.model.NuevaConsulta
import sv.edu.unicaes.hospital.domain.model.NuevaReceta
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Historial clínico y consultas del doctor (API Express, HUNI-57). La API no ofrece un endpoint que guarde
 * consulta, diagnóstico, recetas y órdenes en una sola transacción: cada paso es una llamada y la consulta
 * queda abierta hasta [cerrar]. Quien guarda debe poder reintentar desde el paso que falló.
 */
interface ConsultasRepository {
    /** 403 [AppError.SinPermiso] si el doctor no es médico tratante del paciente. */
    suspend fun historial(pacienteId: String): Resultado<HistorialClinico>

    /** Abre la consulta y devuelve su id. */
    suspend fun crear(consulta: NuevaConsulta): Resultado<String>

    /** Registra el diagnóstico principal de la consulta abierta. */
    suspend fun agregarDiagnostico(
        consultaId: String,
        descripcion: String,
    ): Resultado<Unit>

    suspend fun emitirReceta(
        consultaId: String,
        receta: NuevaReceta,
    ): Resultado<Unit>

    /** Cierra la consulta; si ya estaba cerrada (un reintento tras perder la respuesta) también es éxito. */
    suspend fun cerrar(consultaId: String): Resultado<Unit>
}

@Singleton
class ConsultasRepositoryImpl
    @Inject
    constructor(
        private val api: HospitalApi,
    ) : ConsultasRepository {
        override suspend fun historial(pacienteId: String): Resultado<HistorialClinico> =
            safeApiCall { api.historialPaciente(pacienteId).data.aDominio() }

        override suspend fun crear(consulta: NuevaConsulta): Resultado<String> =
            safeApiCall { api.crearConsulta(consulta.aBody()).data.id }

        override suspend fun agregarDiagnostico(
            consultaId: String,
            descripcion: String,
        ): Resultado<Unit> =
            safeApiCall<Unit> {
                api.agregarDiagnostico(consultaId, CrearDiagnosticoBody(descripcion.trim(), TIPO_PRINCIPAL))
            }

        override suspend fun emitirReceta(
            consultaId: String,
            receta: NuevaReceta,
        ): Resultado<Unit> = safeApiCall<Unit> { api.emitirReceta(consultaId, receta.aBody()) }

        override suspend fun cerrar(consultaId: String): Resultado<Unit> =
            when (val resultado = safeApiCall { api.cerrarConsulta(consultaId) }) {
                is Resultado.Exito -> {
                    Resultado.Exito(Unit)
                }

                is Resultado.Fallo -> {
                    val error = resultado.error
                    if (error is AppError.Rechazada && error.codigo == CODIGO_CONSULTA_CERRADA) {
                        Resultado.Exito(Unit)
                    } else {
                        resultado
                    }
                }
            }
    }
