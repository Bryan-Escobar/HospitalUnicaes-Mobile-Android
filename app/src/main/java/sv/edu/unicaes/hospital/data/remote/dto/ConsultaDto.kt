package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.ConsultaClinica
import sv.edu.unicaes.hospital.domain.model.Diagnostico
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenCatalogo
import sv.edu.unicaes.hospital.domain.model.Expediente
import sv.edu.unicaes.hospital.domain.model.HistorialClinico
import sv.edu.unicaes.hospital.domain.model.NuevaConsulta
import sv.edu.unicaes.hospital.domain.model.NuevaOrden
import sv.edu.unicaes.hospital.domain.model.NuevaReceta
import sv.edu.unicaes.hospital.domain.model.OrdenResumen
import sv.edu.unicaes.hospital.domain.model.RecetaEmitida

// Esquemas `HistorialClinico`, `ConsultaDetalle`, `Consulta`, `ExamenCatalogo` y los bodies de /consultas
// y /laboratorio/ordenes (HUNI-57). Los bodies no tienen valores por defecto en lo que se debe enviar:
// el Json de la app no codifica los valores por defecto y omite los null (`explicitNulls = false`).

@Serializable
data class HistorialClinicoDto(
    val paciente: PacienteDto,
    val expediente: ExpedienteDto,
    val consultas: List<ConsultaDetalleDto> = emptyList(),
)

@Serializable
data class ExpedienteDto(
    @SerialName("numero_expediente") val numeroExpediente: String = "",
    @SerialName("tipo_sangre") val tipoSangre: String? = null,
    val alergias: String? = null,
    val antecedentes: String? = null,
    @SerialName("enfermedades_cronicas") val enfermedadesCronicas: String? = null,
)

@Serializable
data class ConsultaDetalleDto(
    val id: String,
    @SerialName("cita_id") val citaId: String? = null,
    val fecha: String,
    val doctor: PersonaResumenDto,
    @SerialName("motivo_consulta") val motivoConsulta: String? = null,
    val notas: String? = null,
    @SerialName("cerrada_en") val cerradaEn: String? = null,
    val diagnosticos: List<DiagnosticoDto> = emptyList(),
    val recetas: List<RecetaDto> = emptyList(),
    @SerialName("ordenes_laboratorio") val ordenesLaboratorio: List<OrdenResumenDto> = emptyList(),
)

@Serializable
data class DiagnosticoDto(
    val descripcion: String,
    @SerialName("codigo_cie10") val codigoCie10: String? = null,
    /** `principal` o `secundario`. */
    val tipo: String = TIPO_PRINCIPAL,
)

@Serializable
data class RecetaDto(
    val id: String,
    val indicaciones: String? = null,
    val detalles: List<RecetaDetalleDto> = emptyList(),
)

@Serializable
data class RecetaDetalleDto(
    val medicamento: String,
    val dosis: String? = null,
    val frecuencia: String? = null,
    val duracion: String? = null,
)

@Serializable
data class OrdenResumenDto(
    val id: String,
    @SerialName("fecha_solicitud") val fechaSolicitud: String,
    val estado: String,
    @SerialName("total_examenes") val totalExamenes: Int = 0,
)

/** Respuesta de POST /consultas y POST /consultas/:id/cerrar: solo interesa el id. */
@Serializable
data class ConsultaCreadaDto(
    val id: String,
    @SerialName("cerrada_en") val cerradaEn: String? = null,
)

/** Respuesta de POST /consultas/:id/diagnosticos y …/recetas: solo interesa que se creó. */
@Serializable
data class RegistroCreadoDto(
    val id: String,
)

@Serializable
data class ExamenCatalogoDto(
    val id: String,
    val nombre: String,
    val categoria: String? = null,
    @SerialName("tipo_muestra") val tipoMuestra: String? = null,
    val activo: Boolean = true,
)

@Serializable
data class CrearConsultaBody(
    @SerialName("paciente_id") val pacienteId: String,
    @SerialName("cita_id") val citaId: String?,
    @SerialName("motivo_consulta") val motivoConsulta: String?,
    val notas: String?,
)

@Serializable
data class CrearDiagnosticoBody(
    val descripcion: String,
    val tipo: String,
)

@Serializable
data class CrearRecetaBody(
    val indicaciones: String?,
    val detalles: List<CrearRecetaDetalleBody>,
)

@Serializable
data class CrearRecetaDetalleBody(
    val medicamento: String,
    val dosis: String?,
    val frecuencia: String?,
)

@Serializable
data class CrearOrdenBody(
    @SerialName("consulta_id") val consultaId: String,
    val prioridad: String,
    val indicaciones: String?,
    @SerialName("examen_ids") val examenIds: List<String>,
)

const val TIPO_PRINCIPAL = "principal"

fun HistorialClinicoDto.aDominio(): HistorialClinico =
    HistorialClinico(
        paciente = paciente.aDominio(),
        expediente = expediente.aDominio(),
        consultas = consultas.map { it.aDominio() },
    )

fun ExpedienteDto.aDominio(): Expediente =
    Expediente(
        numeroExpediente = numeroExpediente,
        tipoSangre = tipoSangre.sinBlancos(),
        alergias = alergias.sinBlancos(),
        antecedentes = antecedentes.sinBlancos(),
        enfermedadesCronicas = enfermedadesCronicas.sinBlancos(),
    )

fun ConsultaDetalleDto.aDominio(): ConsultaClinica =
    ConsultaClinica(
        id = id,
        citaId = citaId,
        fecha = fecha,
        doctor = doctor.nombreCompleto,
        motivo = motivoConsulta.sinBlancos(),
        notas = notas.sinBlancos(),
        cerrada = cerradaEn != null,
        diagnosticos =
            diagnosticos.map {
                Diagnostico(
                    descripcion = it.descripcion,
                    codigoCie10 = it.codigoCie10,
                    principal = it.tipo == TIPO_PRINCIPAL,
                )
            },
        recetas =
            recetas.map { receta ->
                RecetaEmitida(
                    id = receta.id,
                    indicaciones = receta.indicaciones.sinBlancos(),
                    medicamentos = receta.detalles.map { it.medicamento },
                )
            },
        ordenes =
            ordenesLaboratorio.map {
                OrdenResumen(
                    id = it.id,
                    fechaSolicitud = it.fechaSolicitud,
                    estado = EstadoLaboratorio.desdeApi(it.estado),
                    totalExamenes = it.totalExamenes,
                )
            },
    )

fun ExamenCatalogoDto.aDominio(): ExamenCatalogo =
    ExamenCatalogo(id = id, nombre = nombre, categoria = categoria, tipoMuestra = tipoMuestra)

fun NuevaConsulta.aBody(): CrearConsultaBody =
    CrearConsultaBody(
        pacienteId = pacienteId,
        citaId = citaId,
        motivoConsulta = motivo.sinBlancos(),
        notas = notas.sinBlancos(),
    )

fun NuevaReceta.aBody(): CrearRecetaBody =
    CrearRecetaBody(
        indicaciones = indicaciones.sinBlancos(),
        detalles =
            listOf(
                CrearRecetaDetalleBody(
                    medicamento = medicamento.trim(),
                    dosis = dosis.sinBlancos(),
                    frecuencia = frecuencia.sinBlancos(),
                ),
            ),
    )

fun NuevaOrden.aBody(consultaId: String): CrearOrdenBody =
    CrearOrdenBody(
        consultaId = consultaId,
        prioridad = prioridad.valorApi,
        indicaciones = indicaciones.sinBlancos(),
        examenIds = examenIds,
    )

/** Texto recortado, o null si queda vacío: la API valida `min(1)` y no tiene sentido guardar espacios. */
private fun String?.sinBlancos(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
