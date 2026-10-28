package sv.edu.unicaes.hospital.data

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sv.edu.unicaes.hospital.data.remote.AuthInterceptor
import java.lang.reflect.Proxy

class AuthInterceptorTest {
    /** Cadena falsa que devuelve la petición tal como le llegó al siguiente eslabón. */
    private fun cadena(request: Request): Pair<Interceptor.Chain, () -> Request> {
        var enviada: Request? = null
        val chain =
            Proxy.newProxyInstance(
                Interceptor.Chain::class.java.classLoader,
                arrayOf(Interceptor.Chain::class.java),
            ) { _, method, args ->
                when (method.name) {
                    "request" -> {
                        request
                    }

                    "proceed" -> {
                        enviada = args[0] as Request
                        Response
                            .Builder()
                            .request(enviada!!)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("".toResponseBody())
                            .build()
                    }

                    else -> {
                        throw UnsupportedOperationException(method.name)
                    }
                }
            } as Interceptor.Chain
        return chain to { enviada!! }
    }

    private val request = Request.Builder().url("https://api.ejemplo.sv/api/me").build()

    @Test
    fun `agrega el token como Bearer`() {
        val (chain, enviada) = cadena(request)
        AuthInterceptor { "jwt-123" }.intercept(chain)
        assertEquals("Bearer jwt-123", enviada().header("Authorization"))
    }

    @Test
    fun `sin sesion no agrega cabecera`() {
        val (chain, enviada) = cadena(request)
        AuthInterceptor { null }.intercept(chain)
        assertNull(enviada().header("Authorization"))
    }
}
