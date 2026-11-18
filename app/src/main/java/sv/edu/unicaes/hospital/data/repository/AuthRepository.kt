package sv.edu.unicaes.hospital.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

data class ConexionApp(
    val perfilId: String,
    val instante: String,
)

interface AuthRepository {
    val estadoSesion: Flow<EstadoSesion>
    val ultimaConexionApp: StateFlow<ConexionApp?>

    suspend fun iniciarSesion(
        correo: String,
        contrasena: String,
    ): Resultado<Unit>

    suspend fun cerrarSesion()

    suspend fun usuarioActual(): Resultado<Usuario>

    suspend fun registrarConexionApp()
}

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val api: HospitalApi,
    ) : AuthRepository {
        private val _ultimaConexionApp = MutableStateFlow<ConexionApp?>(null)
        override val ultimaConexionApp: StateFlow<ConexionApp?> = _ultimaConexionApp.asStateFlow()

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
                // Sin red signOut no llega a borrar la sesión local: se borra aquí y el token caduca solo.
                supabase.auth.clearSession()
            }
            _ultimaConexionApp.value = null
        }

        override suspend fun usuarioActual(): Resultado<Usuario> = safeApiCall { api.me().data.aDominio() }

        override suspend fun registrarConexionApp() {
            val sesion = supabase.auth.currentSessionOrNull() ?: return
            val perfilId = sesion.user?.id ?: return
            val resultado = safeApiCall { api.registrarConexionApp("Bearer ${sesion.accessToken}").data }
            val perfilActualId =
                supabase.auth
                    .currentSessionOrNull()
                    ?.user
                    ?.id
            if (resultado is Resultado.Exito && perfilActualId == perfilId) {
                _ultimaConexionApp.value = ConexionApp(perfilId, resultado.datos.ultimaConexionAppAt)
            }
        }
    }
