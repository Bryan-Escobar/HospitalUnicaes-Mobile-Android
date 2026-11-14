package sv.edu.unicaes.hospital.ui.modelo

import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.domain.model.Turno
import java.util.Calendar
import java.util.Date

// Turnos del personal (HUNI-53): semanas de lunes a domingo en la zona del hospital.

const val DIAS_SEMANA = 7

/** Claves (`2026-10-26`) de lunes a domingo de la semana de [hoy] desplazada [desplazamiento] semanas. */
fun semanaDe(
    desplazamiento: Int,
    hoy: Date = Date(),
): List<String> {
    val inicioHoy = fechaHoraHospital(claveDia(hoy)) ?: return emptyList()
    val diaSemana =
        Calendar
            .getInstance(ZONA_HOSPITAL)
            .apply { time = inicioHoy }
            .get(Calendar.DAY_OF_WEEK)
    // Calendar: domingo = 1 … sábado = 7; la semana del hospital empieza el lunes.
    val desdeLunes = (diaSemana + DIAS_SEMANA - Calendar.MONDAY) % DIAS_SEMANA
    val lunes = Date(inicioHoy.time + (desplazamiento * DIAS_SEMANA - desdeLunes) * UN_DIA_MS)
    return proximosDias(DIAS_SEMANA, lunes)
}

/** `19 – 25 oct`, o `27 oct – 2 nov` si la semana cambia de mes. */
fun etiquetaSemana(dias: List<String>): String {
    val primero = dias.firstOrNull()?.let { fechaHoraHospital(it) } ?: return ""
    val ultimo = dias.lastOrNull()?.let { fechaHoraHospital(it) } ?: return ""
    val inicio = diaMes(primero, ZONA_HOSPITAL)
    val fin = diaMes(ultimo, ZONA_HOSPITAL)
    val mismoMes = inicio.substringAfter(' ') == fin.substringAfter(' ')
    return if (mismoMes) "${inicio.substringBefore(' ')} – $fin" else "$inicio – $fin"
}

/** `Lun 26` para los chips y las filas de la semana. */
fun etiquetaClave(clave: String): String = fechaHoraHospital(clave)?.let { etiquetaDia(it) } ?: clave

/** `07:00` o `7:5` → válido solo `HH:mm` de 00:00 a 23:59. */
fun horaValida(hora: String): Boolean = Regex("""([01]\d|2[0-3]):[0-5]\d""").matches(hora)

/** Horario sugerido al elegir el tipo de turno en el formulario. */
fun horarioSugerido(tipo: TipoTurno): Pair<String, String> =
    when (tipo) {
        TipoTurno.MANANA -> "07:00" to "15:00"
        TipoTurno.TARDE -> "15:00" to "23:00"
        TipoTurno.NOCHE -> "19:00" to "07:00"
        TipoTurno.GUARDIA -> "07:00" to "19:00"
    }

/** El nombre del catálogo `tipos_personal` en la etiqueta corta que colorea [EtiquetaPersonal]. */
fun etiquetaTipoPersonal(tipo: String): String =
    when (tipo) {
        "Médico de emergencia" -> "Emergencia"
        "Enfermero/a" -> "Enfermería"
        else -> tipo
    }

fun Turno.enCurso(ahora: Date = Date()): Boolean {
    val desde = fechaDesdeIso(inicio) ?: return false
    val hasta = fechaDesdeIso(fin) ?: return false
    return !ahora.before(desde) && ahora.before(hasta)
}

fun Turno.terminado(ahora: Date = Date()): Boolean = fechaDesdeIso(fin)?.let { !ahora.before(it) } ?: false

/** `07:00–15:00`. */
val Turno.horario: String get() = "$horaInicio–$horaFin"

/** Turnos de un día agrupados por área (alfabético) y, dentro de cada área, por tipo de personal y hora. */
fun agruparPorArea(
    turnos: List<Turno>,
    ahora: Date = Date(),
): List<AreaTurnosUi> =
    turnos
        .groupBy { it.area }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .map { (area, delArea) ->
            AreaTurnosUi(
                area = area,
                turnos =
                    delArea
                        .sortedWith(compareBy({ it.tipoPersonal }, { it.horaInicio }, { it.persona }))
                        .map { turno ->
                            TurnoPersonalUi(
                                id = turno.id,
                                persona = turno.persona,
                                area = turno.area,
                                horario = turno.horario,
                                tipo = etiquetaTipoPersonal(turno.tipoPersonal),
                                enCurso = turno.enCurso(ahora),
                            )
                        },
            )
        }

/**
 * Mis turnos de la semana [dias]: uno por turno en el orden de la semana y "Sin turno" en los días libres.
 * El turno de noche se anota en el día en que empieza.
 */
fun misTurnosSemana(
    dias: List<String>,
    turnos: List<Turno>,
    ahora: Date = Date(),
): List<TurnoUi> =
    dias.flatMap { clave ->
        val etiqueta = etiquetaClave(clave)
        val delDia = turnos.filter { it.fecha == clave }.sortedBy { it.horaInicio }
        if (delDia.isEmpty()) {
            listOf(TurnoUi(dia = etiqueta, horario = "", area = "", sinTurno = true))
        } else {
            delDia.map { turno ->
                TurnoUi(
                    dia = etiqueta,
                    horario = turno.horario,
                    area = if (turno.tipoTurno == TipoTurno.NOCHE) "${turno.area} · nocturno" else turno.area,
                    enCurso = turno.enCurso(ahora),
                    terminado = turno.terminado(ahora),
                )
            }
        }
    }
