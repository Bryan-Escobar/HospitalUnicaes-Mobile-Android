package sv.edu.unicaes.hospital.ui.modelo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// minSdk 24 sin desugaring: SimpleDateFormat en lugar de java.time.

private val ESPANOL = Locale.forLanguageTag("es")

/** ISO 8601 con zona (`2026-10-28T16:30:00.000Z`, `2026-10-28T16:30:00+00:00`) → Date, o null si no se entiende. */
fun fechaDesdeIso(iso: String): Date? {
    val sinFraccion = iso.trim().replace(Regex("""\.\d+"""), "")
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { isLenient = false }.parse(sinFraccion)
    }.getOrNull()
}

private fun formato(
    patron: String,
    zona: TimeZone,
) = SimpleDateFormat(patron, ESPANOL).apply { timeZone = zona }

/** `19 oct`, como en el diseño (sin el punto de la abreviatura). */
fun diaMes(
    fecha: Date,
    zona: TimeZone = TimeZone.getDefault(),
): String = formato("d MMM", zona).format(fecha).replace(".", "")

/** `28/10`. */
fun diaMesNumerico(
    fecha: Date,
    zona: TimeZone = TimeZone.getDefault(),
): String = formato("dd/MM", zona).format(fecha)

/** `4:40 p. m.`, para mostrar. */
fun hora(
    fecha: Date,
    zona: TimeZone = TimeZone.getDefault(),
): String = hora12(horaClave(fecha, zona))

/** `16:40`, en 24 horas: es el formato interno y el que recibe la API. Para mostrar usar [hora] o [hora12]. */
fun horaClave(
    fecha: Date,
    zona: TimeZone = TimeZone.getDefault(),
): String = formato("HH:mm", zona).format(fecha)

private val HORA_24 = Regex("""([01]\d|2[0-3]):([0-5]\d)""")

/** `16:40` → `4:40 p. m.`, `00:00` → `12:00 a. m.`. Lo que no sea `HH:mm` se devuelve igual. */
fun hora12(hora24: String): String {
    val partes = HORA_24.matchEntire(hora24.trim()) ?: return hora24
    val horas = partes.groupValues[1].toInt()
    val sufijo = if (horas < MEDIODIA) "a. m." else "p. m."
    val horas12 = if (horas % MEDIODIA == 0) MEDIODIA else horas % MEDIODIA
    return "$horas12:${partes.groupValues[2]} $sufijo"
}

private const val MEDIODIA = 12

private val HORA_12 = Regex("""(\d{1,2})(?::([0-5]\d))?\s*([ap])\.?\s*m\.?""", RegexOption.IGNORE_CASE)

/**
 * Lo que escribe el usuario (`7:00 a. m.`, `7:00am`, `3 PM`) → `HH:mm` para la API, o null si no es una hora de
 * 12 horas válida. Sin a. m. / p. m. no se acepta: `7:00` podría ser de la mañana o de la noche.
 */
fun hora24Desde12(texto: String): String? {
    val partes = HORA_12.matchEntire(texto.trim()) ?: return null
    val horas = partes.groupValues[1].toInt()
    if (horas !in 1..MEDIODIA) return null
    val minutos = partes.groupValues[2].ifEmpty { "00" }
    val tarde = partes.groupValues[3].equals("p", ignoreCase = true)
    val horas24 = horas % MEDIODIA + if (tarde) MEDIODIA else 0
    return "%02d:%s".format(Locale.US, horas24, minutos)
}

/** Días completos desde [desde] hasta [hasta]. */
fun diasEntre(
    desde: Date,
    hasta: Date = Date(),
): Int =
    TimeUnit.MILLISECONDS
        .toDays(hasta.time - desde.time)
        .toInt()
        .coerceAtLeast(0)

/** Zona del hospital (UTC−6, sin horario de verano). Las citas y los turnos se agendan en esta hora. */
val ZONA_HOSPITAL: TimeZone = TimeZone.getTimeZone("America/El_Salvador")

/** `2026-10-29T10:30:00-06:00`, para mandarlo a Supabase. */
fun isoConZona(fecha: Date): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        .apply {
            timeZone =
                ZONA_HOSPITAL
        }.format(fecha)

/** Día en la zona del hospital como `2026-10-29`; se usa de clave en rutas y chips. */
fun claveDia(fecha: Date): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = ZONA_HOSPITAL }.format(fecha)

/** Fecha y hora (`2026-10-29`, `10:30`) en la zona del hospital, o null si no se entiende. */
fun fechaHoraHospital(
    dia: String,
    hora: String = "00:00",
): Date? =
    runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .apply {
                timeZone = ZONA_HOSPITAL
                isLenient = false
            }.parse("$dia $hora")
    }.getOrNull()

/** `Jue 29`, como los chips de días del diseño. */
fun etiquetaDia(fecha: Date): String =
    formato("EEE d", ZONA_HOSPITAL)
        .format(fecha)
        .replace(".", "")
        .replaceFirstChar { it.uppercase() }

/** Claves de [cantidad] días seguidos desde [desde], en la zona del hospital. */
fun proximosDias(
    cantidad: Int,
    desde: Date = Date(),
): List<String> {
    val hoy = fechaHoraHospital(claveDia(desde)) ?: return emptyList()
    // Mediodía de cada día: sumar 24 h nunca salta ni repite un día.
    return (0 until cantidad).map { claveDia(Date(hoy.time + it * UN_DIA_MS + MEDIO_DIA_MS)) }
}

const val UN_DIA_MS = 24 * 60 * 60 * 1000L
private const val MEDIO_DIA_MS = UN_DIA_MS / 2
