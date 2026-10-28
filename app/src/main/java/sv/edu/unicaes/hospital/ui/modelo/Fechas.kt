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

/** `16:40`, en 24 horas. */
fun hora(
    fecha: Date,
    zona: TimeZone = TimeZone.getDefault(),
): String = formato("HH:mm", zona).format(fecha)

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
