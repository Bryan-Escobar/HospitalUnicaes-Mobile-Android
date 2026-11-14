package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.DURACION_CITA_MINUTOS
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.TurnoMedico

/** Fila de la vista `v_citas` de Supabase (citas con los nombres del paciente y del doctor). */
@Serializable
data class CitaDto(
    val id: String,
    @SerialName("doctor_nombres") val doctorNombres: String,
    @SerialName("doctor_apellidos") val doctorApellidos: String,
    @SerialName("fecha_hora") val fechaHora: String,
    val motivo: String? = null,
    val estado: String,
    @SerialName("paciente_id") val pacienteId: String = "",
    @SerialName("paciente_nombres") val pacienteNombres: String = "",
    @SerialName("paciente_apellidos") val pacienteApellidos: String = "",
    @SerialName("doctor_id") val doctorId: String = "",
    val especialidad: String? = null,
    @SerialName("duracion_minutos") val duracionMinutos: Int = DURACION_CITA_MINUTOS,
)

/** Fila de la vista `v_turnos`; `inicio` y `fin` ya vienen como timestamptz. */
@Serializable
data class TurnoDto(
    @SerialName("personal_id") val personalId: String,
    val nombres: String,
    val apellidos: String,
    val especialidad: String? = null,
    val inicio: String,
    val fin: String,
)

/** Fila que devuelve la RPC `doctores_disponibles`. */
@Serializable
data class DoctorDisponibleDto(
    val id: String,
    val nombres: String,
    val apellidos: String,
    val especialidad: String? = null,
)

fun CitaDto.aDominio(): Cita =
    Cita(
        id = id,
        fechaHora = fechaHora,
        doctor = "$doctorNombres $doctorApellidos".trim(),
        motivo = motivo,
        estado = EstadoCita.desdeApi(estado),
        pacienteId = pacienteId,
        paciente = "$pacienteNombres $pacienteApellidos".trim(),
        doctorId = doctorId,
        especialidad = especialidad,
        duracionMinutos = duracionMinutos,
    )

fun TurnoDto.aDominio(): TurnoMedico =
    TurnoMedico(
        doctorId = personalId,
        doctor = "$nombres $apellidos".trim(),
        especialidad = especialidad,
        inicio = inicio,
        fin = fin,
    )

fun DoctorDisponibleDto.aDominio(): DoctorDisponible =
    DoctorDisponible(id = id, nombre = "$nombres $apellidos".trim(), especialidad = especialidad)
