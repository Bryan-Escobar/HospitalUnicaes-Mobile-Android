package sv.edu.unicaes.hospital.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Fuente del JWT de la sesión actual (Supabase Auth). */
fun interface TokenProvider {
    fun tokenActual(): String?
}

/**
 * Agrega `Authorization: Bearer <jwt>` a cada llamada a la API. La API valida
 * el token de Supabase y aplica RBAC; la app nunca decide permisos por sí sola.
 */
class AuthInterceptor
    @Inject
    constructor(
        private val tokenProvider: TokenProvider,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val token = tokenProvider.tokenActual()
            if (token == null || request.header(HEADER) != null) {
                return chain.proceed(request)
            }
            return chain.proceed(
                request
                    .newBuilder()
                    .header(HEADER, "Bearer $token")
                    .build(),
            )
        }

        private companion object {
            const val HEADER = "Authorization"
        }
    }
