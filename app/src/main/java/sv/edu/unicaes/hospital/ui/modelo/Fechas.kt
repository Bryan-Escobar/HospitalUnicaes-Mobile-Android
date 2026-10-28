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
