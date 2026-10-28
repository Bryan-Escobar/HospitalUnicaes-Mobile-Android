package sv.edu.unicaes.hospital.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.HospitalizacionDto
import sv.edu.unicaes.hospital.data.remote.dto.PacienteBodyDto
import sv.edu.unicaes.hospital.data.remote.dto.PacienteDto
import sv.edu.unicaes.hospital.data.remote.dto.PaginaDto
import sv.edu.unicaes.hospital.data.remote.dto.UsuarioDto

/** Endpoints de la API Express. La URL base sale de BuildConfig.API_BASE_URL. */
interface HospitalApi {
    /** Perfil y rol del usuario autenticado (403 CUENTA_INACTIVA / PERFIL_INCOMPLETO). */
    @GET("auth/me")
    suspend fun me(): ApiRespuesta<UsuarioDto>

    /** Pacientes activos; `buscar` (mín. 2 caracteres) filtra por nombre, apellido o documento. */
    @GET("pacientes")
    suspend fun listarPacientes(
        @Query("buscar") buscar: String?,
        @Query("pagina") pagina: Int,
        @Query("por_pagina") porPagina: Int,
    ): ApiRespuesta<PaginaDto<PacienteDto>>

    @GET("pacientes/{id}")
    suspend fun obtenerPaciente(
        @Path("id") id: String,
    ): ApiRespuesta<PacienteDto>

    /** 409 REGISTRO_DUPLICADO si el documento ya existe; 400 VALIDACION con el detalle por campo. */
    @POST("pacientes")
    suspend fun crearPaciente(
        @Body body: PacienteBodyDto,
    ): ApiRespuesta<PacienteDto>

    @PATCH("pacientes/{id}")
    suspend fun actualizarPaciente(
        @Path("id") id: String,
        @Body body: PacienteBodyDto,
    ): ApiRespuesta<PacienteDto>

    /** Hospitalizaciones filtradas por paciente y estado (`activa` o `alta`). */
    @GET("hospitalizaciones")
    suspend fun listarHospitalizaciones(
        @Query("paciente_id") pacienteId: String,
        @Query("estado") estado: String,
        @Query("por_pagina") porPagina: Int,
    ): ApiRespuesta<PaginaDto<HospitalizacionDto>>
}
