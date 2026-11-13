package sv.edu.unicaes.hospital.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.CambiarEstadoExamenDto
import sv.edu.unicaes.hospital.data.remote.dto.HospitalizacionDto
import sv.edu.unicaes.hospital.data.remote.dto.OrdenExamenDto
import sv.edu.unicaes.hospital.data.remote.dto.OrdenLaboratorioDto
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

    // Laboratorio (HUNI-62, HUNI-63). Recepción ve todas las órdenes y procesa sus exámenes.

    /** Órdenes de la más reciente a la más antigua; `estado` filtra por el estado derivado de la orden. */
    @GET("laboratorio/ordenes")
    suspend fun listarOrdenesLaboratorio(
        @Query("estado") estado: String?,
        @Query("pagina") pagina: Int,
        @Query("por_pagina") porPagina: Int,
    ): ApiRespuesta<PaginaDto<OrdenLaboratorioDto>>

    @GET("laboratorio/ordenes/{id}")
    suspend fun obtenerOrdenLaboratorio(
        @Path("id") id: String,
    ): ApiRespuesta<OrdenLaboratorioDto>

    /** pendiente → en_proceso. 422 TRANSICION_INVALIDA si ya avanzó (también si otro usuario lo hizo antes). */
    @PATCH("laboratorio/ordenes/{id}/examenes/{orden_examen_id}/estado")
    suspend fun iniciarExamen(
        @Path("id") ordenId: String,
        @Path("orden_examen_id") examenId: String,
        @Body body: CambiarEstadoExamenDto,
    ): ApiRespuesta<OrdenExamenDto>

    /**
     * en_proceso → completado con el resultado (`multipart/form-data`): texto y/o `archivo` PDF o imagen
     * de hasta 10 MB. 422 RESULTADO_REQUERIDO sin ninguno; 400 SOLICITUD_INVALIDA si el archivo no sirve.
     */
    @Multipart
    @PUT("laboratorio/ordenes/{id}/examenes/{orden_examen_id}/resultado")
    suspend fun registrarResultado(
        @Path("id") ordenId: String,
        @Path("orden_examen_id") examenId: String,
        @Part("valor") valor: RequestBody?,
        @Part("unidad") unidad: RequestBody?,
        @Part("rango_referencia") rangoReferencia: RequestBody?,
        @Part("observaciones") observaciones: RequestBody?,
        @Part archivo: MultipartBody.Part?,
    ): ApiRespuesta<OrdenExamenDto>
}
