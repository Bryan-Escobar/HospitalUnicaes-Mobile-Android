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
        "CUENTA_INACTIVA" -> AppError.CuentaInactiva
        "PERFIL_INCOMPLETO" -> AppError.PerfilIncompleto
        "REGISTRO_DUPLICADO" -> AppError.Duplicado
        "VALIDACION" -> AppError.CamposInvalidos(cuerpo.errors.associate { it.field to it.message })
        else -> errorDesdeCodigo(e.code())
    }
}

/** Códigos con los que las funciones y triggers de Supabase anteponen un mensaje legible. */
private val CODIGOS_RECHAZO = setOf("CITA_DUPLICADA", "CITA_FUERA_DE_TURNO", "REGLA_DE_NEGOCIO", "TRANSICION_INVALIDA")

/** `CITA_DUPLICADA: el doctor ya tiene una cita…` → [AppError.Rechazada] con el texto después del código. */
fun rechazoDesdeMensaje(mensaje: String): AppError.Rechazada? {
    val codigo = mensaje.substringBefore(':', missingDelimiterValue = "").trim()
    if (codigo !in CODIGOS_RECHAZO) return null
    val detalle = mensaje.substringAfter(':').trim()
    return detalle.takeIf { it.isNotEmpty() }?.let { AppError.Rechazada(it.replaceFirstChar(Char::uppercase)) }
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
