package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.DatosTurno
import sv.edu.unicaes.hospital.domain.model.Empleado
import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.domain.model.Turno

/** Fila completa de la vista `v_turnos` (cualquier empleado, no solo médicos). */
@Serializable
data class TurnoPersonalDto(
    val id: String,
    @SerialName("personal_id") val personalId: String,
    val nombres: String,
    val apellidos: String,
    @SerialName("tipo_personal") val tipoPersonal: String,
    @SerialName("es_medico") val esMedico: Boolean = false,
    val area: String,
    @SerialName("tipo_turno") val tipoTurno: String,
    val fecha: String,
    @SerialName("hora_inicio") val horaInicio: String,
    @SerialName("hora_fin") val horaFin: String,
    val inicio: String,
    val fin: String,
    val notas: String? = null,
)

/** Fila de `personal` con su tipo embebido (`tipos_personal(nombre)`). */
@Serializable
data class EmpleadoDto(
    val id: String,
    val nombres: String,
    val apellidos: String,
    @SerialName("tipos_personal") val tipoPersonal: TipoPersonalDto? = null,
)

/** Cuerpo de insert/update de `turnos_personal`; las horas van como `HH:mm`. */
@Serializable
data class TurnoBodyDto(
    @SerialName("personal_id") val personalId: String,
    val area: String,
    val fecha: String,
    @SerialName("hora_inicio") val horaInicio: String,
    @SerialName("hora_fin") val horaFin: String,
    @SerialName("tipo_turno") val tipoTurno: String,
    val notas: String? = null,
)

/** Postgres devuelve `time` como `07:00:00`; en la app se usa `07:00`. */
internal fun horaCorta(valor: String): String = valor.take(LARGO_HORA)

private const val LARGO_HORA = 5

fun TurnoPersonalDto.aDominio(): Turno =
    Turno(
        id = id,
        personalId = personalId,
        persona = "$nombres $apellidos".trim(),
        tipoPersonal = tipoPersonal,
        esMedico = esMedico,
        area = area,
        tipoTurno = TipoTurno.desdeApi(tipoTurno),
        fecha = fecha,
        horaInicio = horaCorta(horaInicio),
        horaFin = horaCorta(horaFin),
        inicio = inicio,
        fin = fin,
        notas = notas,
    )

fun EmpleadoDto.aDominio(): Empleado =
    Empleado(id = id, nombre = "$nombres $apellidos".trim(), tipoPersonal = tipoPersonal?.nombre.orEmpty())

fun DatosTurno.aBody(): TurnoBodyDto =
    TurnoBodyDto(
        personalId = personalId,
        area = area.trim(),
        fecha = fecha,
        horaInicio = horaInicio,
        horaFin = horaFin,
        tipoTurno = tipoTurno.valorApi,
        notas = notas?.trim()?.ifEmpty { null },
    )
