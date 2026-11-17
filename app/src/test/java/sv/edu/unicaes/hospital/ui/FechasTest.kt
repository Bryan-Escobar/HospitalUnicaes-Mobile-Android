package sv.edu.unicaes.hospital.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sv.edu.unicaes.hospital.ui.modelo.diaMes
import sv.edu.unicaes.hospital.ui.modelo.diaMesNumerico
import sv.edu.unicaes.hospital.ui.modelo.diasEntre
import sv.edu.unicaes.hospital.ui.modelo.fechaDesdeIso
import sv.edu.unicaes.hospital.ui.modelo.hora
import sv.edu.unicaes.hospital.ui.modelo.hora12
import sv.edu.unicaes.hospital.ui.modelo.horaClave
import java.util.TimeZone

class FechasTest {
    private val elSalvador = TimeZone.getTimeZone("America/El_Salvador")

    @Test
    fun `lee la fecha de la API y la de Supabase`() {
        val api = fechaDesdeIso("2026-10-19T22:40:00.000Z")!!
        val supabase = fechaDesdeIso("2026-10-19T22:40:00+00:00")!!
        assertEquals(api, supabase)
        assertEquals("4:40 p. m.", hora(api, elSalvador))
        assertEquals("16:40", horaClave(api, elSalvador))
    }

    @Test
    fun `muestra las horas en 12 horas y distingue medianoche de mediodia`() {
        assertEquals("12:00 a. m.", hora12("00:00"))
        assertEquals("12:00 p. m.", hora12("12:00"))
        assertEquals("7:00 a. m.", hora12("07:00"))
        assertEquals("3:00 p. m.", hora12("15:00"))
        assertEquals("11:59 p. m.", hora12("23:59"))
        assertEquals("", hora12(""))
    }

    @Test
    fun `formatea dia y mes como el diseno`() {
        val fecha = fechaDesdeIso("2026-10-19T22:40:00Z")!!
        assertEquals("19 oct", diaMes(fecha, elSalvador))
        assertEquals("19/10", diaMesNumerico(fecha, elSalvador))
    }

    @Test
    fun `usa la zona local para el dia`() {
        // 03:00 UTC del 20 son las 21:00 del 19 en El Salvador.
        assertEquals("19/10", diaMesNumerico(fechaDesdeIso("2026-10-20T03:00:00Z")!!, elSalvador))
    }

    @Test
    fun `fecha invalida devuelve null`() {
        assertNull(fechaDesdeIso("ayer"))
        assertNull(fechaDesdeIso("2026-10-19"))
    }

    @Test
    fun `cuenta dias completos`() {
        val ingreso = fechaDesdeIso("2026-10-19T10:00:00Z")!!
        assertEquals(2, diasEntre(ingreso, fechaDesdeIso("2026-10-21T12:00:00Z")!!))
        assertEquals(0, diasEntre(ingreso, fechaDesdeIso("2026-10-18T12:00:00Z")!!))
    }
}
