package sv.edu.unicaes.hospital.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import sv.edu.unicaes.hospital.data.remote.dto.AltaBody
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.CambiarEstadoCamaBody
import sv.edu.unicaes.hospital.data.remote.dto.CrearHospitalizacionBody
import sv.edu.unicaes.hospital.data.remote.dto.HabitacionMapaDto
import sv.edu.unicaes.hospital.data.remote.dto.HospitalizacionDto
import sv.edu.unicaes.hospital.data.remote.dto.PacienteBodyDto
import sv.edu.unicaes.hospital.data.remote.dto.PacienteDto
import sv.edu.unicaes.hospital.data.remote.dto.PaginaDto
import sv.edu.unicaes.hospital.data.remote.dto.PersonalDto
import sv.edu.unicaes.hospital.data.remote.dto.TrasladoBody
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

    /** Las 11 habitaciones con sus 17 camas y el ocupante de cada una (HUNI-66). */
    @GET("habitaciones")
    suspend fun habitaciones(): ApiRespuesta<List<HabitacionMapaDto>>

    /**
     * Ingresa al paciente en la cama elegida. La BD bloquea la fila de la cama: si dos personas
     * piden la misma, solo una gana. 409 CAMA_OCUPADA, CAMA_NO_DISPONIBLE, PACIENTE_YA_HOSPITALIZADO,
     * SIN_CAMAS_DISPONIBLES.
     */
    @POST("hospitalizaciones")
    suspend fun crearHospitalizacion(
        @Body body: CrearHospitalizacionBody,
    )

    /** Libera la cama actual y ocupa la nueva en una sola transacción. */
    @POST("hospitalizaciones/{id}/traslado")
    suspend fun trasladar(
        @Path("id") hospitalizacionId: String,
        @Body body: TrasladoBody,
    )

    /** Cierra la hospitalización; la cama pasa a `limpieza`. 409 HOSPITALIZACION_FINALIZADA. */
    @POST("hospitalizaciones/{id}/alta")
    suspend fun darAlta(
        @Path("id") hospitalizacionId: String,
        @Body body: AltaBody,
    )

    /** `limpieza → disponible` después de un alta. 409 CAMA_OCUPADA. */
    @PATCH("camas/{id}/estado")
    suspend fun cambiarEstadoCama(
        @Path("id") camaId: String,
        @Body body: CambiarEstadoCamaBody,
    )

    @GET("personal")
    suspend fun listarPersonal(
        @Query("activo") activo: String,
        @Query("por_pagina") porPagina: Int,
    ): ApiRespuesta<PaginaDto<PersonalDto>>
}
