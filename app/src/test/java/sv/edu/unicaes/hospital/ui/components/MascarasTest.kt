package sv.edu.unicaes.hospital.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import sv.edu.unicaes.hospital.ui.recepcion.formatearDui
import sv.edu.unicaes.hospital.ui.recepcion.formatearFecha
import sv.edu.unicaes.hospital.ui.recepcion.formatearTelefono

class MascarasTest {
    @Test
    fun `el cursor queda despues del digito recien escrito`() {
        // Escribir "251" muestra "25/1": el cursor va después del 1, no antes.
        assertEquals(4, posicionEnFormateado("25/1", 3))
        // "70000" muestra "7000-0": después del 0 del final.
        assertEquals(6, posicionEnFormateado("7000-0", 5))
        assertEquals(0, posicionEnFormateado("25/1", 0))
        assertEquals(2, posicionEnFormateado("25/1", 2))
    }

    @Test
    fun `tocar el texto formateado lleva al digito correcto`() {
        assertEquals(2, posicionEnDigitos("25/12", 3)) // justo después de la barra
        assertEquals(3, posicionEnDigitos("25/12", 4))
        assertEquals(4, posicionEnDigitos("25/12", 5))
        assertEquals(0, posicionEnDigitos("+503 7000-0004", 0))
        assertEquals(3, posicionEnDigitos("+503 7000-0004", 5))
    }

    @Test
    fun `posiciones fuera de rango no se salen del texto`() {
        assertEquals(4, posicionEnFormateado("25/1", 9))
        assertEquals(3, posicionEnDigitos("25/1", 99))
    }

    @Test
    fun `los formateadores conservan los digitos en orden`() {
        listOf("012345678", "25121990", "70000000", "50370000004").forEach { digitos ->
            listOf(::formatearDui, ::formatearFecha, ::formatearTelefono).forEach { formatear ->
                val formateado = formatear(digitos)
                assertEquals(
                    formateado,
                    digitos.take(formateado.count { it.isDigit() }),
                    formateado.filter { it.isDigit() },
                )
            }
        }
    }
}
