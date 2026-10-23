package sv.edu.unicaes.hospital.core

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ResultadoTest {
    private fun httpError(codigo: Int) = HttpException(Response.error<Unit>(codigo, "".toResponseBody()))

    @Test
    fun `devuelve Exito con el valor de la llamada`() =
        runTest {
            assertEquals(Resultado.Exito(42), safeApiCall { 42 })
        }

    @Test
    fun `traduce codigos HTTP de la API`() =
        runTest {
            assertEquals(Resultado.Fallo(AppError.NoAutorizado), safeApiCall { throw httpError(401) })
            assertEquals(Resultado.Fallo(AppError.SinPermiso), safeApiCall { throw httpError(403) })
            assertEquals(Resultado.Fallo(AppError.Conflicto), safeApiCall { throw httpError(409) })
            assertEquals(Resultado.Fallo(AppError.Validacion), safeApiCall { throw httpError(422) })
            assertEquals(Resultado.Fallo(AppError.Servidor(500)), safeApiCall { throw httpError(500) })
        }

    @Test
    fun `sin red devuelve SinConexion`() =
        runTest {
            assertEquals(Resultado.Fallo(AppError.SinConexion), safeApiCall { throw IOException("timeout") })
        }
}
