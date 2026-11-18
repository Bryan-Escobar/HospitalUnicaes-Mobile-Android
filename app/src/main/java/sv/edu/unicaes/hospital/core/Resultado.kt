package sv.edu.unicaes.hospital.core

import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

sealed interface Resultado<out T> {
    data class Exito<T>(
        val datos: T,
    ) : Resultado<T>

    data class Fallo(
        val error: AppError,
    ) : Resultado<Nothing>
}

/** Ejecuta una llamada a la API y traduce las excepciones a [AppError]. */
suspend fun <T> safeApiCall(llamada: suspend () -> T): Resultado<T> =
    try {
        Resultado.Exito(llamada())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Resultado.Fallo(errorDesdeRespuesta(e))
    } catch (e: IOException) {
        Resultado.Fallo(AppError.SinConexion)
    } catch (e: Exception) {
        Resultado.Fallo(AppError.Desconocido(e))
    }

/** Igual que [safeApiCall], para lecturas directas a Supabase (Postgrest con RLS). */
suspend fun <T> safeSupabaseCall(llamada: suspend () -> T): Resultado<T> =
    try {
        Resultado.Exito(llamada())
    } catch (e: CancellationException) {
        throw e
    } catch (e: RestException) {
        Resultado.Fallo(rechazoDesdeMensaje(e.error) ?: errorDesdeCodigo(e.statusCode))
    } catch (e: HttpRequestException) {
        Resultado.Fallo(AppError.SinConexion)
    } catch (e: Exception) {
        Resultado.Fallo(AppError.Desconocido(e))
    }

/** Cuerpo de error de la API: `{ statusCode, success: false, code, message, errors? }`. */
@Serializable
private data class CuerpoError(
    val code: String? = null,
    val message: String? = null,
    val errors: List<ErrorCampo> = emptyList(),
)

/** Detalle de un 400 VALIDACION: `{ field, message }`. */
@Serializable
private data class ErrorCampo(
    val field: String,
    val message: String,
)

private val jsonError = Json { ignoreUnknownKeys = true }

/** Decide por el `code` de la API cuando la UI necesita distinguirlo; si no, por el status HTTP. */
private fun errorDesdeRespuesta(e: HttpException): AppError {
    val cuerpo =
        runCatching {
            e
                .response()
                ?.errorBody()
                ?.string()
                ?.let { jsonError.decodeFromString<CuerpoError>(it) }
        }.getOrNull()
    return when (cuerpo?.code) {
        "CUENTA_INACTIVA" -> {
            AppError.CuentaInactiva
        }

        "PERFIL_INCOMPLETO" -> {
            AppError.PerfilIncompleto
        }

        "REGISTRO_DUPLICADO" -> {
            AppError.Duplicado
        }

        "VALIDACION" -> {
            AppError.CamposInvalidos(cuerpo.errors.associate { it.field to it.message })
        }

        in CODIGOS_CON_MENSAJE -> {
            cuerpo?.message?.takeIf { it.isNotBlank() }?.let { AppError.Rechazada(it) }
        }

        in CODIGOS_CON_CODIGO -> {
            cuerpo?.message?.takeIf { it.isNotBlank() }?.let { AppError.Rechazada(it, cuerpo.code) }
        }

        else -> {
            null
        }
    } ?: errorDesdeCodigo(e.code())
}

/**
 * Códigos de la API cuyo `message` ya está redactado para el usuario (p. ej. "Otro usuario ya inició
 * este examen; actualiza la orden"): se muestran tal cual como [AppError.Rechazada].
 */
private val CODIGOS_CON_MENSAJE =
    setOf(
        "TRANSICION_INVALIDA",
        "RESULTADO_REQUERIDO",
        "SOLICITUD_INVALIDA",
        // Consulta del doctor (HUNI-61): "El paciente está desactivado", "Uno o más exámenes no existen…".
        "REGLA_DE_NEGOCIO",
        "REFERENCIA_INVALIDA",
    )

/**
 * 409 de camas con `message` para el usuario. Se conserva el código en [AppError.Rechazada.codigo]
 * porque la pantalla de camas recarga el mapa cuando la cama elegida ya no se puede asignar.
 */
private val CODIGOS_CAMAS =
    setOf("CAMA_OCUPADA", "CAMA_NO_DISPONIBLE", "PACIENTE_YA_HOSPITALIZADO", "SIN_CAMAS_DISPONIBLES")

/**
 * 409 cuyo `message` se muestra y cuyo `code` la pantalla necesita (recargar el mapa, avisar que la
 * ambulancia ya no se puede despachar, o dar por cerrada una consulta al reintentar).
 */
private val CODIGOS_CON_CODIGO = CODIGOS_CAMAS + CODIGO_AMBULANCIA_NO_DISPONIBLE + CODIGO_CONSULTA_CERRADA

/** Códigos con los que las funciones y triggers de Supabase anteponen un mensaje legible. */
private val CODIGOS_RECHAZO =
    setOf(
        CODIGO_AREA_INVALIDA,
        "CITA_DUPLICADA",
        "CITA_FUERA_DE_TURNO",
        "REGLA_DE_NEGOCIO",
        "TRANSICION_INVALIDA",
        "TURNO_SOLAPADO",
    )

/** Rechazos de la base cuyo código necesita la pantalla (p. ej. recargar las áreas tras `AREA_INVALIDA`). */
private val CODIGOS_RECHAZO_CON_CODIGO = setOf(CODIGO_AREA_INVALIDA)

/** `CITA_DUPLICADA: el doctor ya tiene una cita…` → [AppError.Rechazada] con el texto después del código. */
fun rechazoDesdeMensaje(mensaje: String): AppError.Rechazada? {
    val codigo = mensaje.substringBefore(':', missingDelimiterValue = "").trim()
    if (codigo !in CODIGOS_RECHAZO) return null
    val detalle = mensaje.substringAfter(':').trim()
    return detalle.takeIf { it.isNotEmpty() }?.let {
        AppError.Rechazada(it.replaceFirstChar(Char::uppercase), codigo.takeIf { c -> c in CODIGOS_RECHAZO_CON_CODIGO })
    }
}

fun errorDesdeCodigo(codigo: Int): AppError =
    when (codigo) {
        401 -> AppError.NoAutorizado
        403 -> AppError.SinPermiso
        404 -> AppError.NoEncontrado
        409 -> AppError.Conflicto
        422 -> AppError.Validacion
        else -> AppError.Servidor(codigo)
    }
