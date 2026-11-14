package sv.edu.unicaes.hospital.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeApiCall
import sv.edu.unicaes.hospital.data.local.ArchivosLocales
import sv.edu.unicaes.hospital.data.remote.HospitalApi
import sv.edu.unicaes.hospital.data.remote.dto.CambiarEstadoExamenDto
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.ArchivoAdjunto
import sv.edu.unicaes.hospital.domain.model.DatosResultado
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenOrden
import sv.edu.unicaes.hospital.domain.model.OrdenLaboratorio
import sv.edu.unicaes.hospital.domain.model.Pagina
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Órdenes y resultados de laboratorio (HUNI-63). La API es la fuente de verdad: valida que cada examen
 * solo avance (pendiente → en_proceso → completado) y que dos usuarios no lo hagan a la vez. Los avisos
 * en tiempo real llegan por Supabase Realtime y solo dicen "algo cambió": el detalle se vuelve a pedir a la API.
 */
interface LaboratorioRepository {
    /** Una página de órdenes; [estado] null las trae todas. */
    suspend fun listar(
        estado: EstadoLaboratorio?,
        pagina: Int,
    ): Resultado<Pagina<OrdenLaboratorio>>

    /** Cuántas órdenes hay en [estado] (para los conteos de las pestañas). */
    suspend fun contar(estado: EstadoLaboratorio): Resultado<Int>

    suspend fun obtener(id: String): Resultado<OrdenLaboratorio>

    /** Pendiente → En Proceso. */
    suspend fun iniciarExamen(
        ordenId: String,
        examenId: String,
    ): Resultado<ExamenOrden>

    /** En Proceso → Completado con el texto y/o el archivo (PDF o imagen). */
    suspend fun registrarResultado(
        ordenId: String,
        examenId: String,
        datos: DatosResultado,
        archivo: ArchivoAdjunto?,
    ): Resultado<ExamenOrden>

    /** Emite cada vez que cambia una orden o un examen que este usuario puede ver (RLS). */
    fun cambios(): Flow<Unit>
}

@Singleton
class LaboratorioRepositoryImpl
    @Inject
    constructor(
        private val api: HospitalApi,
        private val supabase: SupabaseClient,
        private val archivos: ArchivosLocales,
    ) : LaboratorioRepository {
        override suspend fun listar(
            estado: EstadoLaboratorio?,
            pagina: Int,
        ): Resultado<Pagina<OrdenLaboratorio>> =
            safeApiCall {
                api.listarOrdenesLaboratorio(estado?.valorApi, pagina, POR_PAGINA).data.aDominio { it.aDominio() }
            }

        override suspend fun contar(estado: EstadoLaboratorio): Resultado<Int> =
            safeApiCall {
                api
                    .listarOrdenesLaboratorio(estado.valorApi, 1, 1)
                    .data.paginacion.total
            }

        override suspend fun obtener(id: String): Resultado<OrdenLaboratorio> =
            safeApiCall { api.obtenerOrdenLaboratorio(id).data.aDominio() }

        override suspend fun iniciarExamen(
            ordenId: String,
            examenId: String,
        ): Resultado<ExamenOrden> =
            safeApiCall { api.iniciarExamen(ordenId, examenId, CambiarEstadoExamenDto.EN_PROCESO).data.aDominio() }

        override suspend fun registrarResultado(
            ordenId: String,
            examenId: String,
            datos: DatosResultado,
            archivo: ArchivoAdjunto?,
        ): Resultado<ExamenOrden> =
            safeApiCall {
                api
                    .registrarResultado(
                        ordenId = ordenId,
                        examenId = examenId,
                        valor = datos.valor.comoParte(),
                        unidad = datos.unidad.comoParte(),
                        rangoReferencia = datos.rangoReferencia.comoParte(),
                        observaciones = datos.observaciones.comoParte(),
                        archivo =
                            archivo?.let {
                                MultipartBody.Part.createFormData(
                                    "archivo",
                                    it.nombre,
                                    CuerpoArchivo(it) { archivos.abrir(it.uri) },
                                )
                            },
                    ).data
                    .aDominio()
            }

        override fun cambios(): Flow<Unit> =
            flow {
                // Un canal por suscripción: cada pantalla abre el suyo y lo cierra al salir.
                val canal = supabase.channel("laboratorio-${UUID.randomUUID()}")
                val avisos =
                    merge(
                        canal.postgresChangeFlow<PostgresAction>(schema = "public") { table = "orden_examenes" },
                        canal.postgresChangeFlow<PostgresAction>(schema = "public") { table = "ordenes_laboratorio" },
                    )
                try {
                    canal.subscribe()
                    emitAll(avisos.map { })
                } finally {
                    withContext(NonCancellable) { supabase.realtime.removeChannel(canal) }
                }
            }

        private companion object {
            const val POR_PAGINA = 20
            val TEXTO_PLANO = "text/plain; charset=utf-8".toMediaType()

            fun String?.comoParte(): RequestBody? = this?.trim()?.takeIf { it.isNotEmpty() }?.toRequestBody(TEXTO_PLANO)
        }
    }

/** Cuerpo que lee el `content://` mientras se envía, sin cargar el archivo en memoria. */
private class CuerpoArchivo(
    private val archivo: ArchivoAdjunto,
    private val abrir: () -> java.io.InputStream?,
) : RequestBody() {
    override fun contentType(): MediaType = archivo.mime.toMediaType()

    override fun contentLength(): Long = archivo.tamanoBytes.takeIf { it > 0 } ?: -1L

    override fun writeTo(sink: BufferedSink) {
        val entrada = abrir() ?: throw IOException("No se pudo leer el archivo elegido")
        entrada.source().use { sink.writeAll(it) }
    }
}
