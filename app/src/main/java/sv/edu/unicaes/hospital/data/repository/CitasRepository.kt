package sv.edu.unicaes.hospital.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeSupabaseCall
import sv.edu.unicaes.hospital.data.remote.dto.CitaDto
import sv.edu.unicaes.hospital.data.remote.dto.DoctorDisponibleDto
import sv.edu.unicaes.hospital.data.remote.dto.TurnoDto
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.DURACION_CITA_MINUTOS
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.TurnoMedico
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Citas y turnos desde Supabase, no desde la API Express (arquitectura híbrida, HUNI-51). Las vistas
 * `v_citas` y `v_turnos` aplican la misma RLS que sus tablas: recepción y administración ven todo.
 * Crear y reprogramar van por RPC, que valida el turno del doctor y los choques de horario.
 * Las fechas son ISO 8601 con zona.
 */
interface CitasRepository {
    /** Las citas más recientes del paciente, de la más nueva a la más antigua. */
    suspend fun dePaciente(pacienteId: String): Resultado<List<Cita>>

    /** Citas no canceladas que empiezan en [desde, hasta), por hora. */
    suspend fun entre(
        desde: String,
        hasta: String,
    ): Resultado<List<Cita>>

    suspend fun obtener(id: String): Resultado<Cita>

    /** Turnos de médicos que se cruzan con [desde, hasta), incluido el de noche del día anterior. */
    suspend fun turnosMedicos(
        desde: String,
        hasta: String,
    ): Resultado<List<TurnoMedico>>

    /** Médicos de turno y libres a esa hora; al reprogramar se ignora la propia cita. */
    suspend fun doctoresDisponibles(
        fechaHora: String,
        excluirCitaId: String? = null,
    ): Resultado<List<DoctorDisponible>>

    suspend fun crear(
        pacienteId: String,
        doctorId: String,
        fechaHora: String,
        motivo: String?,
    ): Resultado<Unit>

    suspend fun reprogramar(
        citaId: String,
        fechaHora: String,
        doctorId: String,
    ): Resultado<Unit>

    suspend fun cancelar(citaId: String): Resultado<Unit>
}

@Singleton
class CitasRepositoryImpl
    @Inject
    constructor(
        private val supabase: SupabaseClient,
    ) : CitasRepository {
        override suspend fun dePaciente(pacienteId: String): Resultado<List<Cita>> =
            safeSupabaseCall {
                supabase.postgrest
                    .from("v_citas")
                    .select {
                        filter { eq("paciente_id", pacienteId) }
                        order("fecha_hora", Order.DESCENDING)
                        limit(MAXIMO_CITAS)
                    }.decodeList<CitaDto>()
                    .map { it.aDominio() }
            }

        override suspend fun entre(
            desde: String,
            hasta: String,
        ): Resultado<List<Cita>> =
            safeSupabaseCall {
                supabase.postgrest
                    .from("v_citas")
                    .select {
                        filter {
                            gte("fecha_hora", desde)
                            lt("fecha_hora", hasta)
                            neq("estado", EstadoCita.CANCELADA.valorApi)
                        }
                        order("fecha_hora", Order.ASCENDING)
                    }.decodeList<CitaDto>()
                    .map { it.aDominio() }
            }

        override suspend fun obtener(id: String): Resultado<Cita> =
            when (
                val resultado =
                    safeSupabaseCall {
                        supabase.postgrest
                            .from("v_citas")
                            .select { filter { eq("id", id) } }
                            .decodeSingleOrNull<CitaDto>()
                    }
            ) {
                is Resultado.Exito -> {
                    resultado.datos?.let { Resultado.Exito(it.aDominio()) }
                        ?: Resultado.Fallo(AppError.NoEncontrado)
                }

                is Resultado.Fallo -> {
                    resultado
                }
            }

        override suspend fun turnosMedicos(
            desde: String,
            hasta: String,
        ): Resultado<List<TurnoMedico>> =
            safeSupabaseCall {
                supabase.postgrest
                    .from("v_turnos")
                    .select {
                        filter {
                            eq("es_medico", true)
                            lt("inicio", hasta)
                            gt("fin", desde)
                        }
                        order("inicio", Order.ASCENDING)
                    }.decodeList<TurnoDto>()
                    .map { it.aDominio() }
            }

        override suspend fun doctoresDisponibles(
            fechaHora: String,
            excluirCitaId: String?,
        ): Resultado<List<DoctorDisponible>> =
            safeSupabaseCall {
                val parametros =
                    buildJsonObject {
                        put("p_fecha_hora", fechaHora)
                        put("p_duracion_minutos", DURACION_CITA_MINUTOS)
                        excluirCitaId?.let { put("p_excluir_cita_id", it) }
                    }
                supabase.postgrest
                    .rpc("doctores_disponibles", parametros)
                    .decodeList<DoctorDisponibleDto>()
                    .map { it.aDominio() }
            }

        override suspend fun crear(
            pacienteId: String,
            doctorId: String,
            fechaHora: String,
            motivo: String?,
        ): Resultado<Unit> =
            safeSupabaseCall {
                val parametros =
                    buildJsonObject {
                        put("p_paciente_id", pacienteId)
                        put("p_doctor_id", doctorId)
                        put("p_fecha_hora", fechaHora)
                        put("p_duracion_minutos", DURACION_CITA_MINUTOS)
                        put("p_motivo", motivo)
                    }
                supabase.postgrest.rpc("crear_cita", parametros)
                Unit
            }

        override suspend fun reprogramar(
            citaId: String,
            fechaHora: String,
            doctorId: String,
        ): Resultado<Unit> =
            safeSupabaseCall {
                val parametros =
                    buildJsonObject {
                        put("p_cita_id", citaId)
                        put("p_fecha_hora", fechaHora)
                        put("p_doctor_id", doctorId)
                    }
                supabase.postgrest.rpc("reprogramar_cita", parametros)
                Unit
            }

        /** La RLS deja a recepción cambiar el estado; si no se tocó ninguna fila, la cita no existe. */
        override suspend fun cancelar(citaId: String): Resultado<Unit> =
            when (
                val resultado =
                    safeSupabaseCall {
                        supabase.postgrest
                            .from("citas")
                            .update({ set("estado", EstadoCita.CANCELADA.valorApi) }) {
                                select()
                                filter { eq("id", citaId) }
                            }.decodeList<JsonObject>()
                    }
            ) {
                is Resultado.Exito -> {
                    if (resultado.datos.isEmpty()) {
                        Resultado.Fallo(
                            AppError.NoEncontrado,
                        )
                    } else {
                        Resultado.Exito(Unit)
                    }
                }

                is Resultado.Fallo -> {
                    resultado
                }
            }

        private companion object {
            const val MAXIMO_CITAS = 10L
        }
    }
