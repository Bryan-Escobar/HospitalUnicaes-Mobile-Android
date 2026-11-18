package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.Ambulancia
import sv.edu.unicaes.hospital.domain.model.EntradaBitacora
import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.domain.model.SolicitudActiva
import sv.edu.unicaes.hospital.domain.model.SolicitudAmbulancia

// Esquemas de /ambulancia (HUNI-70). Los campos que el listado no trae quedan en null.

@Serializable
data class AmbulanciaDto(
    val id: String,
    val placa: String,
    val estado: String,
    @SerialName("solicitud_activa") val solicitudActiva: SolicitudActivaDto? = null,
)

@Serializable
data class SolicitudActivaDto(
    val id: String,
    val prioridad: String = "urgente",
    val estado: String = "despachada",
    @SerialName("tipo_emergencia") val tipoEmergencia: String? = null,
    @SerialName("direccion_origen") val direccionOrigen: String? = null,
    @SerialName("fecha_despacho") val fechaDespacho: String? = null,
)

@Serializable
data class SolicitudAmbulanciaDto(
    val id: String,
    val canal: String = "app",
    val paciente: PersonaResumenDto? = null,
    @SerialName("direccion_origen") val direccionOrigen: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val destino: String? = null,
    @SerialName("descripcion_emergencia") val descripcionEmergencia: String? = null,
    @SerialName("tipo_emergencia") val tipoEmergencia: String? = null,
    val prioridad: String = "urgente",
    val estado: String = "pendiente",
    @SerialName("nombre_contacto") val nombreContacto: String? = null,
    @SerialName("fecha_solicitud") val fechaSolicitud: String = "",
    @SerialName("fecha_despacho") val fechaDespacho: String? = null,
    @SerialName("fecha_cierre") val fechaCierre: String? = null,
)

@Serializable
data class BitacoraAmbulanciaDto(
    val id: String,
    @SerialName("estado_anterior") val estadoAnterior: String? = null,
    @SerialName("estado_nuevo") val estadoNuevo: String,
    val descripcion: String? = null,
    @SerialName("registrado_por") val registradoPor: PersonaResumenDto? = null,
    val solicitud: SolicitudAmbulanciaDto? = null,
    @SerialName("created_at") val createdAt: String,
)

/** Body de POST /ambulancia/solicitudes/{id}/cerrar. */
@Serializable
data class CerrarSolicitudBody(
    val descripcion: String? = null,
)

/** Body de POST /ambulancia/solicitudes/{id}/cancelar. En una pendiente el motivo es opcional. */
@Serializable
data class CancelarSolicitudBody(
    val motivo: String? = null,
)

fun AmbulanciaDto.aDominio(): Ambulancia =
    Ambulancia(
        id = id,
        placa = placa,
        estado = EstadoAmbulancia.desdeApi(estado),
        solicitudActiva = solicitudActiva?.aDominio(),
    )

fun SolicitudActivaDto.aDominio(): SolicitudActiva =
    SolicitudActiva(
        id = id,
        prioridad = prioridad,
        estado = estado,
        tipo = tipoEmergencia,
        direccion = direccionOrigen,
        fechaDespacho = fechaDespacho,
    )

fun SolicitudAmbulanciaDto.aDominio(): SolicitudAmbulancia =
    SolicitudAmbulancia(
        id = id,
        paciente = paciente?.nombreCompleto?.ifBlank { null },
        nombreContacto = nombreContacto,
        direccion = direccionOrigen,
        latitud = latitud,
        longitud = longitud,
        destino = destino,
        descripcion = descripcionEmergencia,
        tipo = tipoEmergencia,
        prioridad = prioridad,
        estado = estado,
        fechaSolicitud = fechaSolicitud,
        fechaDespacho = fechaDespacho,
        fechaCierre = fechaCierre,
        canal = canal,
    )

fun BitacoraAmbulanciaDto.aDominio(): EntradaBitacora =
    EntradaBitacora(
        id = id,
        estadoAnterior = estadoAnterior?.let { EstadoAmbulancia.desdeApi(it) },
        estadoNuevo = EstadoAmbulancia.desdeApi(estadoNuevo),
        descripcion = descripcion,
        registradoPor = registradoPor?.nombreCompleto?.ifBlank { null },
        solicitud = solicitud?.aDominio(),
        creadaEn = createdAt,
    )
