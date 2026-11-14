package sv.edu.unicaes.hospital.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeApiCall
import sv.edu.unicaes.hospital.data.remote.HospitalApi
import sv.edu.unicaes.hospital.data.remote.dto.AltaBody
import sv.edu.unicaes.hospital.data.remote.dto.CambiarEstadoCamaBody
import sv.edu.unicaes.hospital.data.remote.dto.CrearHospitalizacionBody
import sv.edu.unicaes.hospital.data.remote.dto.TrasladoBody
import sv.edu.unicaes.hospital.data.remote.dto.aDoctor
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.EstadoCama
import sv.edu.unicaes.hospital.domain.model.Habitacion
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Habitaciones, camas e ingresos (HUNI-66/67). Las escrituras van a la API Express, que asigna con
 * bloqueo de fila: la BD garantiza 1 paciente por cama, 1 cama por paciente y la capacidad de cada
 * habitación aunque dos recepcionistas pidan la misma cama a la vez. Los cambios de ocupación
 * llegan por Supabase Realtime (tablas `camas` y `hospitalizaciones`).
 */
interface CamasRepository {
    suspend fun habitaciones(): Resultado<List<Habitacion>>

    suspend fun ingresar(
        pacienteId: String,
        doctorId: String,
        camaId: String,
        motivo: String?,
    ): Resultado<Unit>

    suspend fun trasladar(
        hospitalizacionId: String,
        camaId: String,
    ): Resultado<Unit>

    suspend fun darAlta(
        hospitalizacionId: String,
        notas: String?,
    ): Resultado<Unit>

    /** Después de la limpieza, la cama vuelve a estar disponible para ingresos. */
    suspend fun marcarDisponible(camaId: String): Resultado<Unit>

    /** Médicos activos que pueden ser tratantes. */
    suspend fun doctores(): Resultado<List<DoctorDisponible>>

    /** Emite cada vez que cambia la ocupación en cualquier dispositivo. Si Realtime falla, no emite. */
    fun cambios(): Flow<Unit>
}

@Singleton
class CamasRepositoryImpl
    @Inject
    constructor(
        private val api: HospitalApi,
        private val supabase: SupabaseClient,
    ) : CamasRepository {
        override suspend fun habitaciones(): Resultado<List<Habitacion>> =
            safeApiCall {
                api
                    .habitaciones()
                    .data
                    .map { it.aDominio() }
                    .sortedBy { it.codigo }
            }

        override suspend fun ingresar(
            pacienteId: String,
            doctorId: String,
            camaId: String,
            motivo: String?,
        ): Resultado<Unit> =
            safeApiCall {
                api.crearHospitalizacion(
                    CrearHospitalizacionBody(
                        pacienteId = pacienteId,
                        doctorTratanteId = doctorId,
                        camaId = camaId,
                        motivoIngreso = motivo?.trim()?.ifEmpty { null },
                    ),
                )
            }

        override suspend fun trasladar(
            hospitalizacionId: String,
            camaId: String,
        ): Resultado<Unit> = safeApiCall { api.trasladar(hospitalizacionId, TrasladoBody(camaId)) }

        override suspend fun darAlta(
            hospitalizacionId: String,
            notas: String?,
        ): Resultado<Unit> = safeApiCall { api.darAlta(hospitalizacionId, AltaBody(notas?.trim()?.ifEmpty { null })) }

        override suspend fun marcarDisponible(camaId: String): Resultado<Unit> =
            safeApiCall { api.cambiarEstadoCama(camaId, CambiarEstadoCamaBody(EstadoCama.DISPONIBLE.valorApi)) }

        override suspend fun doctores(): Resultado<List<DoctorDisponible>> =
            safeApiCall {
                api
                    .listarPersonal(activo = "true", porPagina = MAXIMO_PERSONAL)
                    .data.items
                    .filter { it.rol == ROL_DOCTOR && it.activo }
                    .map { it.aDoctor() }
                    .sortedBy { it.nombre }
            }

        @OptIn(FlowPreview::class)
        override fun cambios(): Flow<Unit> =
            channelFlow {
                // Un canal por suscriptor: el mapa y el ingreso pueden estar escuchando a la vez.
                val canal = supabase.channel("ocupacion-camas-${UUID.randomUUID()}")
                val tablas =
                    TABLAS.map { tabla ->
                        canal.postgresChangeFlow<PostgresAction>(schema = "public") { table = tabla }
                    }
                launch { merge(*tablas.toTypedArray()).collect { send(Unit) } }
                try {
                    canal.subscribe()
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) { runCatching { supabase.realtime.removeChannel(canal) } }
                }
            }
                // Un alta cambia `camas` y `hospitalizaciones` a la vez: se agrupan en una sola recarga.
                .debounce(AGRUPAR_CAMBIOS_MS)
                .catch { if (it is CancellationException) throw it }

        private companion object {
            const val ROL_DOCTOR = "doctor"
            const val MAXIMO_PERSONAL = 100
            const val AGRUPAR_CAMBIOS_MS = 300L
            val TABLAS = listOf("camas", "hospitalizaciones")
        }
    }
