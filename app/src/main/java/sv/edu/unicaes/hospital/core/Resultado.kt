package sv.edu.unicaes.hospital.core

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

/** Cuerpo de error de la API: `{ statusCode, success: false, code, message }`. */
@Serializable
private data class CuerpoError(
    val code: String? = null,
)

private val jsonError = Json { ignoreUnknownKeys = true }

/** Decide por el `code` de la API cuando la UI necesita distinguirlo; si no, por el status HTTP. */
private fun errorDesdeRespuesta(e: HttpException): AppError {
    val codigo =
        runCatching {
            e
                .response()
                ?.errorBody()
                ?.string()
                ?.let { jsonError.decodeFromString<CuerpoError>(it).code }
        }.getOrNull()
    return when (codigo) {
        "CUENTA_INACTIVA" -> AppError.CuentaInactiva
        "PERFIL_INCOMPLETO" -> AppError.PerfilIncompleto
        else -> errorDesdeCodigo(e.code())
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
