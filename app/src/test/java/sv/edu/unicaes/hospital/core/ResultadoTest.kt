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

    private fun httpError(
        codigo: Int,
        cuerpo: String,
    ) = HttpException(Response.error<Unit>(codigo, cuerpo.toResponseBody()))

    @Test
    fun `409 REGISTRO_DUPLICADO devuelve Duplicado`() =
        runTest {
            val error =
                httpError(409, """{"statusCode":409,"success":false,"code":"REGISTRO_DUPLICADO","message":"x"}""")
            assertEquals(Resultado.Fallo(AppError.Duplicado), safeApiCall { throw error })
        }

    @Test
    fun `400 VALIDACION devuelve los campos rechazados`() =
        runTest {
            val error =
                httpError(
                    400,
                    """
                    {"statusCode":400,"success":false,"code":"VALIDACION","message":"x",
                     "errors":[{"field":"telefono","message":"Too small"},{"field":"nombres","message":"Required"}]}
                    """.trimIndent(),
                )
            assertEquals(
                Resultado.Fallo(AppError.CamposInvalidos(mapOf("telefono" to "Too small", "nombres" to "Required"))),
                safeApiCall { throw error },
            )
        }

    @Test
    fun `sin red devuelve SinConexion`() =
        runTest {
            assertEquals(Resultado.Fallo(AppError.SinConexion), safeApiCall { throw IOException("timeout") })
        }

    @Test
    fun `los mensajes de las reglas de citas se muestran sin el codigo`() {
        assertEquals(
            AppError.Rechazada("El doctor ya tiene una cita el 30/10/2026 a las 10:30"),
            rechazoDesdeMensaje("CITA_DUPLICADA: el doctor ya tiene una cita el 30/10/2026 a las 10:30"),
        )
        assertEquals(
            AppError.Rechazada("El doctor no está de turno el 30/10/2026 de 16:00 a 16:30"),
            rechazoDesdeMensaje("CITA_FUERA_DE_TURNO: el doctor no está de turno el 30/10/2026 de 16:00 a 16:30"),
        )
        assertEquals(
            AppError.Rechazada("La cita no puede quedar en el pasado"),
            rechazoDesdeMensaje("REGLA_DE_NEGOCIO: la cita no puede quedar en el pasado"),
        )
    }

    @Test
    fun `otros mensajes de Supabase no son rechazos`() {
        assertEquals(null, rechazoDesdeMensaje("ACCESO_DENEGADO: solo recepción"))
        assertEquals(null, rechazoDesdeMensaje("JWT expired"))
        assertEquals(null, rechazoDesdeMensaje("CITA_DUPLICADA:"))
    }
}
