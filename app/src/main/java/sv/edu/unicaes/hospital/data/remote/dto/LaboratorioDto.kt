package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenOrden
import sv.edu.unicaes.hospital.domain.model.OrdenLaboratorio
import sv.edu.unicaes.hospital.domain.model.PrioridadLaboratorio
import sv.edu.unicaes.hospital.domain.model.ResultadoExamen
import sv.edu.unicaes.hospital.domain.model.TipoArchivoResultado

// Esquemas `OrdenLaboratorio`, `OrdenExamen` y `ResultadoLaboratorio` de /docs (HUNI-62, HUNI-63).

/** `PersonaResumen` / `DoctorResumen` (este último con especialidad). También lo usan las hospitalizaciones. */
@Serializable
data class PersonaResumenDto(
    val id: String = "",
    val nombres: String,
    val apellidos: String,
    val especialidad: String? = null,
) {
    val nombreCompleto: String get() = "$nombres $apellidos".trim()
}

@Serializable
data class OrdenLaboratorioDto(
    val id: String,
    @SerialName("fecha_solicitud") val fechaSolicitud: String,
    val prioridad: String,
    val estado: String,
    val paciente: PersonaResumenDto,
    val doctor: PersonaResumenDto,
    val indicaciones: String? = null,
    val examenes: List<OrdenExamenDto> = emptyList(),
)

@Serializable
data class ExamenCatalogoResumenDto(
    val id: String,
    val nombre: String,
    @SerialName("tipo_muestra") val tipoMuestra: String? = null,
)

@Serializable
data class OrdenExamenDto(
    val id: String,
    val examen: ExamenCatalogoResumenDto,
    val estado: String,
    @SerialName("fecha_inicio") val fechaInicio: String? = null,
    @SerialName("fecha_completado") val fechaCompletado: String? = null,
    val resultado: ResultadoLaboratorioDto? = null,
)

@Serializable
data class ResultadoLaboratorioDto(
    val id: String,
    val valor: String? = null,
    val unidad: String? = null,
    @SerialName("rango_referencia") val rangoReferencia: String? = null,
    val observaciones: String? = null,
    @SerialName("tiene_archivo") val tieneArchivo: Boolean = false,
    /** `pdf` o `imagen` (HUNI-63). Sin el campo, el archivo es un PDF (HUNI-62 solo aceptaba PDF). */
    @SerialName("tipo_archivo") val tipoArchivo: String? = null,
    @SerialName("validado_por") val validadoPor: PersonaResumenDto? = null,
    @SerialName("fecha_resultado") val fechaResultado: String,
)

/**
 * Body de PATCH …/estado: la única transición que se pide por esa ruta. Sin valor por defecto a propósito:
 * el Json de la app no codifica los valores por defecto (`encodeDefaults = false`) y el body saldría `{}`.
 */
@Serializable
data class CambiarEstadoExamenDto(
    val estado: String,
) {
    companion object {
        val EN_PROCESO = CambiarEstadoExamenDto(EstadoLaboratorio.EN_PROCESO.valorApi)
    }
}

fun OrdenLaboratorioDto.aDominio(): OrdenLaboratorio =
    OrdenLaboratorio(
        id = id,
        fechaSolicitud = fechaSolicitud,
        prioridad = PrioridadLaboratorio.desdeApi(prioridad),
        estado = EstadoLaboratorio.desdeApi(estado),
        pacienteId = paciente.id,
        paciente = paciente.nombreCompleto,
        doctor = doctor.nombreCompleto,
        especialidad = doctor.especialidad,
        indicaciones = indicaciones?.takeIf { it.isNotBlank() },
        examenes = examenes.map { it.aDominio() },
    )

fun OrdenExamenDto.aDominio(): ExamenOrden =
    ExamenOrden(
        id = id,
        nombre = examen.nombre,
        tipoMuestra = examen.tipoMuestra,
        estado = EstadoLaboratorio.desdeApi(estado),
        fechaInicio = fechaInicio,
        fechaCompletado = fechaCompletado,
        resultado = resultado?.aDominio(),
    )

fun ResultadoLaboratorioDto.aDominio(): ResultadoExamen =
    ResultadoExamen(
        valor = valor,
        unidad = unidad,
        rangoReferencia = rangoReferencia,
        observaciones = observaciones,
        tipoArchivo =
            when {
                !tieneArchivo -> null
                tipoArchivo == "imagen" -> TipoArchivoResultado.IMAGEN
                else -> TipoArchivoResultado.PDF
            },
        validadoPor = validadoPor?.nombreCompleto,
        fecha = fechaResultado,
    )
