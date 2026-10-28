package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Pagina
import sv.edu.unicaes.hospital.domain.model.Sexo

/** Esquema `Paciente` de /pacientes (ver /docs de la API). */
@Serializable
data class PacienteDto(
    val id: String,
    val nombres: String,
    val apellidos: String,
    @SerialName("documento_identidad") val documentoIdentidad: String? = null,
    @SerialName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    @SerialName("contacto_emergencia") val contactoEmergencia: String? = null,
    @SerialName("telefono_emergencia") val telefonoEmergencia: String? = null,
    @SerialName("numero_expediente") val numeroExpediente: String = "",
    val activo: Boolean = true,
)

/** Respuesta `paginado(...)`: `{ items, paginacion }`. */
@Serializable
data class PaginaDto<T>(
    val items: List<T>,
    val paginacion: PaginacionDto,
)

@Serializable
data class PaginacionDto(
    val pagina: Int,
    @SerialName("total_paginas") val totalPaginas: Int,
)

/**
 * Body de POST y PATCH /pacientes. Los null no se envían (Json con `explicitNulls = false`):
 * en el PATCH solo viajan los campos que cambiaron.
 */
@Serializable
data class PacienteBodyDto(
    val nombres: String? = null,
    val apellidos: String? = null,
    @SerialName("documento_identidad") val documentoIdentidad: String? = null,
    @SerialName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    @SerialName("contacto_emergencia") val contactoEmergencia: String? = null,
    @SerialName("telefono_emergencia") val telefonoEmergencia: String? = null,
)

fun PacienteDto.aDominio(): Paciente =
    Paciente(
        id = id,
        nombres = nombres,
        apellidos = apellidos,
        documento = documentoIdentidad,
        fechaNacimiento = fechaNacimiento,
        sexo = Sexo.desdeApi(sexo),
        telefono = telefono,
        direccion = direccion,
        contactoEmergencia = contactoEmergencia,
        telefonoEmergencia = telefonoEmergencia,
        numeroExpediente = numeroExpediente,
        activo = activo,
    )

fun <T, R> PaginaDto<T>.aDominio(transformar: (T) -> R): Pagina<R> =
    Pagina(items = items.map(transformar), pagina = paginacion.pagina, totalPaginas = paginacion.totalPaginas)

fun DatosPaciente.aBody(): PacienteBodyDto =
    PacienteBodyDto(
        nombres = nombres,
        apellidos = apellidos,
        documentoIdentidad = documento,
        fechaNacimiento = fechaNacimiento,
        sexo = sexo?.valorApi,
        telefono = telefono,
        direccion = direccion,
        contactoEmergencia = contactoEmergencia,
        telefonoEmergencia = telefonoEmergencia,
    )
