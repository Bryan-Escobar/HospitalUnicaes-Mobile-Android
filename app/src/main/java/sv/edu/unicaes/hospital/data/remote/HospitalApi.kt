package sv.edu.unicaes.hospital.data.remote

import retrofit2.http.GET
import sv.edu.unicaes.hospital.data.remote.dto.UsuarioDto

/** Endpoints de la API Express. La URL base sale de BuildConfig.API_BASE_URL. */
interface HospitalApi {
    /** Perfil y rol del usuario autenticado. */
    @GET("me")
    suspend fun me(): UsuarioDto
}
