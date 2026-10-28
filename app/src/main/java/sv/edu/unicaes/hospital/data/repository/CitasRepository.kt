package sv.edu.unicaes.hospital.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeSupabaseCall
import sv.edu.unicaes.hospital.data.remote.dto.CitaDto
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.Cita
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Citas desde Supabase, no desde la API Express (arquitectura híbrida, HUNI-51). La vista `v_citas`
 * aplica la misma RLS que la tabla: recepción y administración ven todas.
 */
interface CitasRepository {
    /** Las citas más recientes del paciente, de la más nueva a la más antigua. */
    suspend fun dePaciente(pacienteId: String): Resultado<List<Cita>>
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

        private companion object {
            const val MAXIMO_CITAS = 10L
        }
    }
