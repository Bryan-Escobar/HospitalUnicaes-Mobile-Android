package sv.edu.unicaes.hospital.data.remote

import retrofit2.http.GET
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.UsuarioDto

/** Endpoints de la API Express. La URL base sale de BuildConfig.API_BASE_URL. */
interface HospitalApi {
    /** Perfil y rol del usuario autenticado (403 CUENTA_INACTIVA / PERFIL_INCOMPLETO). */
    @GET("auth/me")
    suspend fun me(): ApiRespuesta<UsuarioDto>
}
