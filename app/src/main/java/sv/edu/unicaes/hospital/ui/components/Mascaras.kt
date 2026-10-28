package sv.edu.unicaes.hospital.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Muestra solo-dígitos con separadores (`25121990` → `25/12/1990`) sin mover el cursor: el campo
 * guarda los dígitos y [formatear] agrega lo demás al dibujar. Reemplazar el texto en cada tecla
 * deja el cursor antes del dígito recién escrito cuando aparece un separador.
 *
 * [formatear] debe conservar los dígitos en el mismo orden (solo agrega caracteres que no lo son).
 */
class MascaraDigitos(
    private val formatear: (String) -> String,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formateado = formatear(text.text)
        val mapeo =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = posicionEnFormateado(formateado, offset)

                override fun transformedToOriginal(offset: Int): Int =
                    posicionEnDigitos(formateado, offset).coerceAtMost(text.length)
            }
        return TransformedText(AnnotatedString(formateado), mapeo)
    }

    override fun equals(other: Any?): Boolean = other is MascaraDigitos && other.formatear == formatear

    override fun hashCode(): Int = formatear.hashCode()
}

/** Posición en [formateado] justo después del dígito número [digitos] (0 = inicio). */
internal fun posicionEnFormateado(
    formateado: String,
    digitos: Int,
): Int {
    if (digitos <= 0) return 0
    var vistos = 0
    formateado.forEachIndexed { indice, caracter ->
        if (caracter.isDigit() && ++vistos == digitos) return indice + 1
    }
    return formateado.length
}

/** Cuántos dígitos hay en [formateado] antes de la posición [posicion]. */
internal fun posicionEnDigitos(
    formateado: String,
    posicion: Int,
): Int = formateado.take(posicion.coerceIn(0, formateado.length)).count { it.isDigit() }
