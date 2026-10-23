package sv.edu.unicaes.hospital.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.safeApiCall
import sv.edu.unicaes.hospital.data.remote.HospitalApi
import sv.edu.unicaes.hospital.data.remote.dto.aDominio
import sv.edu.unicaes.hospital.domain.model.Usuario
import javax.inject.Inject
import javax.inject.Singleton

enum class EstadoSesion { CARGANDO, ACTIVA, INACTIVA }

interface AuthRepository {
    val estadoSesion: Flow<EstadoSesion>

    suspend fun iniciarSesion(
        correo: String,
        contrasena: String,
    ): Resultado<Unit>

    suspend fun cerrarSesion()

    suspend fun usuarioActual(): Resultado<Usuario>
}

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val api: HospitalApi,
    ) : AuthRepository {
        override val estadoSesion: Flow<EstadoSesion> =
            supabase.auth.sessionStatus.map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> EstadoSesion.ACTIVA
                    is SessionStatus.Initializing -> EstadoSesion.CARGANDO
                    is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure -> EstadoSesion.INACTIVA
                }
            }

        override suspend fun iniciarSesion(
            correo: String,
            contrasena: String,
        ): Resultado<Unit> =
            try {
                supabase.auth.signInWith(Email) {
                    email = correo
                    password = contrasena
                }
                Resultado.Exito(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RestException) {
                Resultado.Fallo(
                    if (e.statusCode ==
                        400
                    ) {
                        AppError.CredencialesInvalidas
                    } else {
                        AppError.Servidor(e.statusCode)
                    },
                )
            } catch (e: HttpRequestException) {
                Resultado.Fallo(AppError.SinConexion)
            } catch (e: Exception) {
                Resultado.Fallo(AppError.Desconocido(e))
            }

        override suspend fun cerrarSesion() {
            try {
                supabase.auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin red: la sesión local se borra igual y el token caduca solo.
            }
        }

        override suspend fun usuarioActual(): Resultado<Usuario> = safeApiCall { api.me().aDominio() }
    }
