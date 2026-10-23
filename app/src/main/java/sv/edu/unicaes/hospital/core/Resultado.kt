package sv.edu.unicaes.hospital.core

import kotlinx.coroutines.CancellationException
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
        Resultado.Fallo(errorDesdeCodigo(e.code()))
    } catch (e: IOException) {
        Resultado.Fallo(AppError.SinConexion)
    } catch (e: Exception) {
        Resultado.Fallo(AppError.Desconocido(e))
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
