package sv.edu.unicaes.hospital.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeSupabaseCall
import sv.edu.unicaes.hospital.data.remote.dto.EmpleadoDto
import sv.edu.unicaes.hospital.data.remote.dto.TurnoPersonalDto
import sv.edu.unicaes.hospital.data.remote.dto.aBody
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.DatosTurno
import sv.edu.unicaes.hospital.domain.model.Empleado
import sv.edu.unicaes.hospital.domain.model.Turno
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turnos del personal directo desde Supabase (HUNI-51). Todos los usuarios con sesión leen `v_turnos`;
 * recepción y administración crean y ajustan en `turnos_personal`. El trigger `validar_turno` rechaza el
 * solape con `TURNO_SOLAPADO: el empleado ya tiene un turno el …`, que llega como [AppError.Rechazada].
 */
interface TurnosRepository {
    /** Turnos de todo el personal que empiezan entre [desde] y [hasta] (días `2026-10-29`, ambos incluidos). */
    suspend fun entre(
        desde: String,
        hasta: String,
    ): Resultado<List<Turno>>

    /** Igual que [entre], solo los del usuario con sesión; vacío si la cuenta no es de personal. */
    suspend fun mios(
        desde: String,
        hasta: String,
    ): Resultado<List<Turno>>

    /** Personal activo, por apellido. */
    suspend fun empleados(): Resultado<List<Empleado>>

    suspend fun crear(datos: DatosTurno): Resultado<Unit>

    suspend fun actualizar(
        id: String,
        datos: DatosTurno,
    ): Resultado<Unit>
}

@Singleton
class TurnosRepositoryImpl
    @Inject
    constructor(
        private val supabase: SupabaseClient,
    ) : TurnosRepository {
        override suspend fun entre(
            desde: String,
            hasta: String,
        ): Resultado<List<Turno>> = safeSupabaseCall { consultar(desde, hasta, personalId = null) }

        override suspend fun mios(
            desde: String,
            hasta: String,
        ): Resultado<List<Turno>> =
            safeSupabaseCall {
                val personalId =
                    supabase.postgrest
                        .rpc("personal_actual_id")
                        .decodeAs<String?>()
                if (personalId == null) emptyList() else consultar(desde, hasta, personalId)
            }

        private suspend fun consultar(
            desde: String,
            hasta: String,
            personalId: String?,
        ): List<Turno> =
            supabase.postgrest
                .from("v_turnos")
                .select {
                    filter {
                        gte("fecha", desde)
                        lte("fecha", hasta)
                        personalId?.let { eq("personal_id", it) }
                    }
                    order("inicio", Order.ASCENDING)
                }.decodeList<TurnoPersonalDto>()
                .map { it.aDominio() }

        override suspend fun empleados(): Resultado<List<Empleado>> =
            safeSupabaseCall {
                supabase.postgrest
                    .from("personal")
                    .select(Columns.raw("id, nombres, apellidos, tipos_personal(nombre)")) {
                        filter { eq("activo", true) }
                        order("apellidos", Order.ASCENDING)
                        order("nombres", Order.ASCENDING)
                    }.decodeList<EmpleadoDto>()
                    .map { it.aDominio() }
            }

        override suspend fun crear(datos: DatosTurno): Resultado<Unit> =
            safeSupabaseCall<Unit> {
                supabase.postgrest.from("turnos_personal").insert(datos.aBody())
            }

        /** Si la RLS o el id no dejan tocar ninguna fila, el turno no existe para quien llama. */
        override suspend fun actualizar(
            id: String,
            datos: DatosTurno,
        ): Resultado<Unit> =
            when (
                val resultado =
                    safeSupabaseCall {
                        supabase.postgrest
                            .from("turnos_personal")
                            .update(datos.aBody()) {
                                select()
                                filter { eq("id", id) }
                            }.decodeList<JsonObject>()
                    }
            ) {
                is Resultado.Exito -> {
                    if (resultado.datos.isEmpty()) Resultado.Fallo(AppError.NoEncontrado) else Resultado.Exito(Unit)
                }

                is Resultado.Fallo -> {
                    resultado
                }
            }
    }
