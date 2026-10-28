package sv.edu.unicaes.hospital.ui.modelo

import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.DURACION_CITA_MINUTOS
import sv.edu.unicaes.hospital.domain.model.TurnoMedico
import java.util.Date

// Agenda del día de recepción (AND-R5/R6): huecos de 30 min dentro de los turnos y sin choque con citas.

private const val MINUTO_MS = 60_000L
private const val INTERVALO_MS = DURACION_CITA_MINUTOS * MINUTO_MS

/**
 * Inicios de cita libres dentro de los [turnos] de un doctor y del día [[desde], [hasta]): cada hueco cabe
 * entero en el turno, no se cruza con ninguna de sus [citas] y no está en el pasado.
 */
fun huecosLibres(
    turnos: List<TurnoMedico>,
    citas: List<Cita>,
    desde: Date,
    hasta: Date,
    ahora: Date = Date(),
): List<Date> {
    val ocupados =
        citas.mapNotNull { cita ->
            fechaDesdeIso(cita.fechaHora)?.let { it.time until it.time + cita.duracionMinutos * MINUTO_MS }
        }
    return turnos
        .flatMap { turno ->
            val inicio = fechaDesdeIso(turno.inicio)?.time ?: return@flatMap emptyList()
            val fin = fechaDesdeIso(turno.fin)?.time ?: return@flatMap emptyList()
            generateSequence(inicio) { it + INTERVALO_MS }
                .takeWhile { it + INTERVALO_MS <= fin && it < hasta.time }
                .filter { it >= desde.time && it >= ahora.time }
                .filter { hueco -> ocupados.none { hueco < it.last + 1 && it.first < hueco + INTERVALO_MS } }
                .toList()
        }.distinct()
        .sorted()
        .map(::Date)
}

/**
 * Agenda del día por doctor: primero los de turno (en orden de entrada), después los que tienen citas sin
 * turno registrado. Las horas van en la zona del hospital.
 */
fun agruparAgenda(
    citas: List<Cita>,
    turnos: List<TurnoMedico>,
    desde: Date,
    hasta: Date,
    ahora: Date = Date(),
): List<AgendaDoctorUi> {
    val doctores = (turnos.sortedBy { it.inicio }.map { it.doctorId } + citas.map { it.doctorId }).distinct()
    return doctores.map { doctorId ->
        val susTurnos = turnos.filter { it.doctorId == doctorId }
        val susCitas = citas.filter { it.doctorId == doctorId }
        val referencia = susCitas.firstOrNull()
        AgendaDoctorUi(
            doctorId = doctorId,
            doctor = susTurnos.firstOrNull()?.doctor ?: referencia?.doctor.orEmpty(),
            especialidad = susTurnos.firstNotNullOfOrNull { it.especialidad } ?: referencia?.especialidad.orEmpty(),
            horario = susTurnos.joinToString(" · ") { "${horaHospital(it.inicio)}–${horaHospital(it.fin)}" },
            citas = susCitas.map { it.aAgendaUi() },
            horasLibres = huecosLibres(susTurnos, susCitas, desde, hasta, ahora).map { hora(it, ZONA_HOSPITAL) },
        )
    }
}

/** Horas en que al menos un doctor está libre, sin repetir y en orden. */
fun horasDisponibles(agendas: List<AgendaDoctorUi>): List<String> =
    agendas.flatMap { it.horasLibres }.distinct().sorted()

private fun horaHospital(iso: String): String = fechaDesdeIso(iso)?.let { hora(it, ZONA_HOSPITAL) }.orEmpty()

private fun Cita.aAgendaUi(): CitaUi =
    CitaUi(
        id = id,
        hora = horaHospital(fechaHora),
        paciente = paciente,
        doctor = doctor,
        motivo = motivo.orEmpty(),
        estado = estado,
    )

/** Horas de [horas] del día [dia] (`2026-10-29`) en que una cita de 30 min se cruzaría con alguna de [citas]. */
fun horasConCita(
    dia: String,
    horas: List<String>,
    citas: List<Cita>,
): Set<String> {
    val ocupados =
        citas.mapNotNull { cita ->
            fechaDesdeIso(cita.fechaHora)?.let { it.time to it.time + cita.duracionMinutos * MINUTO_MS }
        }
    return horas
        .filter { hora ->
            val inicio = fechaHoraHospital(dia, hora)?.time ?: return@filter false
            ocupados.any { (desde, hasta) -> inicio < hasta && desde < inicio + INTERVALO_MS }
        }.toSet()
}
