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
import sv.edu.unicaes.hospital.data.remote.dto.AltaBody
import sv.edu.unicaes.hospital.data.remote.dto.AmbulanciaDto
import sv.edu.unicaes.hospital.data.remote.dto.ApiRespuesta
import sv.edu.unicaes.hospital.data.remote.dto.BitacoraAmbulanciaDto
import sv.edu.unicaes.hospital.data.remote.dto.CambiarEstadoCamaBody
import sv.edu.unicaes.hospital.data.remote.dto.CambiarEstadoExamenDto
import sv.edu.unicaes.hospital.data.remote.dto.CancelarSolicitudBody
import sv.edu.unicaes.hospital.data.remote.dto.CerrarSolicitudBody
import sv.edu.unicaes.hospital.data.remote.dto.CrearHospitalizacionBody
import sv.edu.unicaes.hospital.data.remote.dto.HabitacionMapaDto
import sv.edu.unicaes.hospital.data.remote.dto.HospitalizacionDto
import sv.edu.unicaes.hospital.data.remote.dto.OrdenExamenDto
import sv.edu.unicaes.hospital.data.remote.dto.OrdenLaboratorioDto
import sv.edu.unicaes.hospital.data.remote.dto.PacienteBodyDto
import sv.edu.unicaes.hospital.data.remote.dto.PacienteDto
import sv.edu.unicaes.hospital.data.remote.dto.PaginaDto
import sv.edu.unicaes.hospital.data.remote.dto.PersonalDto
import sv.edu.unicaes.hospital.data.remote.dto.SolicitudAmbulanciaDto
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

    // Ambulancia (HUNI-70, HUNI-71). Recepción despacha, cierra y cancela; el estado llega por Realtime.

    /** La única ambulancia y, si va en camino, el resumen del servicio abierto. */
    @GET("ambulancia")
    suspend fun ambulancia(): ApiRespuesta<AmbulanciaDto>

    /** Solicitudes, de la más reciente a la más antigua. `estado=pendiente` son las que faltan por atender. */
    @GET("ambulancia/solicitudes")
    suspend fun listarSolicitudesAmbulancia(
        @Query("estado") estado: String?,
        @Query("pagina") pagina: Int,
        @Query("por_pagina") porPagina: Int,
    ): ApiRespuesta<PaginaDto<SolicitudAmbulanciaDto>>

    @GET("ambulancia/solicitudes/{id}")
    suspend fun obtenerSolicitudAmbulancia(
        @Path("id") id: String,
    ): ApiRespuesta<SolicitudAmbulanciaDto>

    /**
     * Pasa la ambulancia a `en_servicio`. 409 AMBULANCIA_NO_DISPONIBLE si otro la despachó primero o está
     * en mantenimiento; 422 TRANSICION_INVALIDA si la solicitud ya no está pendiente.
     */
    @POST("ambulancia/solicitudes/{id}/despachar")
    suspend fun despacharAmbulancia(
        @Path("id") id: String,
    )

    /** Vuelve a `disponible` y deja la emergencia en la bitácora. 422 TRANSICION_INVALIDA si no iba en camino. */
    @POST("ambulancia/solicitudes/{id}/cerrar")
    suspend fun cerrarSolicitudAmbulancia(
        @Path("id") id: String,
        @Body body: CerrarSolicitudBody,
    )

    /** Cancela una pendiente (rechazo de recepción). 422 TRANSICION_INVALIDA si ya está cerrada. */
    @POST("ambulancia/solicitudes/{id}/cancelar")
    suspend fun cancelarSolicitudAmbulancia(
        @Path("id") id: String,
        @Body body: CancelarSolicitudBody,
    )

    /** Cada cambio de estado. `estado` filtra por el estado nuevo; `desde` y `hasta` son AAAA-MM-DD. */
    @GET("ambulancia/bitacora")
    suspend fun bitacoraAmbulancia(
        @Query("estado") estado: String?,
        @Query("tipo_emergencia") tipo: String?,
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?,
        @Query("pagina") pagina: Int,
        @Query("por_pagina") porPagina: Int,
    ): ApiRespuesta<PaginaDto<BitacoraAmbulanciaDto>>
}
