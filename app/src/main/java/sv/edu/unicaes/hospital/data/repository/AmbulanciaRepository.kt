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
import sv.edu.unicaes.hospital.data.remote.dto.CancelarSolicitudBody
import sv.edu.unicaes.hospital.data.remote.dto.CerrarSolicitudBody
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.Ambulancia
import sv.edu.unicaes.hospital.domain.model.EntradaBitacora
import sv.edu.unicaes.hospital.domain.model.FiltroBitacora
import sv.edu.unicaes.hospital.domain.model.SolicitudAmbulancia
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Despacho de la única ambulancia (HUNI-71, API de HUNI-70). Las escrituras van a Express, que bloquea
 * la fila: dos despachos a la vez dejan uno en 409. Supabase Realtime avisa de cambios en `ambulancias`
 * y `solicitudes_ambulancia`; el aviso solo dice que algo cambió y la pantalla vuelve a pedir la API.
 */
interface AmbulanciaRepository {
    suspend fun estado(): Resultado<Ambulancia>

    /** Solicitudes `pendiente`, de la más reciente a la más antigua (iOS y el botón público). */
    suspend fun pendientes(): Resultado<List<SolicitudAmbulancia>>

    suspend fun obtener(id: String): Resultado<SolicitudAmbulancia>

    suspend fun despachar(id: String): Resultado<Unit>

    suspend fun cerrar(
        id: String,
        descripcion: String?,
    ): Resultado<Unit>

    suspend fun cancelar(id: String): Resultado<Unit>

    suspend fun bitacora(filtro: FiltroBitacora): Resultado<List<EntradaBitacora>>

    /** Emite cada vez que cambia el estado o una solicitud. Si Realtime falla, no emite. */
    fun cambios(): Flow<Unit>
}

@Singleton
class AmbulanciaRepositoryImpl
    @Inject
    constructor(
        private val api: HospitalApi,
        private val supabase: SupabaseClient,
    ) : AmbulanciaRepository {
        override suspend fun estado(): Resultado<Ambulancia> = safeApiCall { api.ambulancia().data.aDominio() }

        override suspend fun pendientes(): Resultado<List<SolicitudAmbulancia>> =
            safeApiCall {
                api
                    .listarSolicitudesAmbulancia(ESTADO_PENDIENTE, PAGINA, POR_PAGINA)
                    .data.items
                    .map { it.aDominio() }
            }

        override suspend fun obtener(id: String): Resultado<SolicitudAmbulancia> =
            safeApiCall { api.obtenerSolicitudAmbulancia(id).data.aDominio() }

        override suspend fun despachar(id: String): Resultado<Unit> = safeApiCall { api.despacharAmbulancia(id) }

        override suspend fun cerrar(
            id: String,
            descripcion: String?,
        ): Resultado<Unit> = safeApiCall { api.cerrarSolicitudAmbulancia(id, CerrarSolicitudBody(descripcion)) }

        override suspend fun cancelar(id: String): Resultado<Unit> =
            safeApiCall { api.cancelarSolicitudAmbulancia(id, CancelarSolicitudBody()) }

        override suspend fun bitacora(filtro: FiltroBitacora): Resultado<List<EntradaBitacora>> =
            safeApiCall {
                api
                    .bitacoraAmbulancia(
                        estado = filtro.estado?.valorApi,
                        tipo = filtro.tipo,
                        desde = filtro.desde,
                        hasta = filtro.hasta,
                        pagina = PAGINA,
                        porPagina = POR_PAGINA,
                    ).data.items
                    .map { it.aDominio() }
            }

        @OptIn(FlowPreview::class)
        override fun cambios(): Flow<Unit> =
            channelFlow {
                val canal = supabase.channel("ambulancia-${UUID.randomUUID()}")
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
            }.debounce(AGRUPAR_CAMBIOS_MS)
                .catch { if (it is CancellationException) throw it }

        private companion object {
            const val ESTADO_PENDIENTE = "pendiente"
            const val PAGINA = 1
            const val POR_PAGINA = 100
            const val AGRUPAR_CAMBIOS_MS = 300L
            val TABLAS = listOf("ambulancias", "solicitudes_ambulancia")
        }
    }
