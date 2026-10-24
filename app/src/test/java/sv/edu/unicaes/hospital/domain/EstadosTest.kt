package sv.edu.unicaes.hospital.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio

class EstadosTest {
    @Test
    fun `laboratorio solo avanza hacia adelante`() {
        assertEquals(EstadoLaboratorio.EN_PROCESO, EstadoLaboratorio.PENDIENTE.siguiente)
        assertEquals(EstadoLaboratorio.COMPLETADO, EstadoLaboratorio.EN_PROCESO.siguiente)
        assertNull(EstadoLaboratorio.COMPLETADO.siguiente)
    }

    @Test
    fun `la ambulancia solo se despacha si esta disponible`() {
        assertTrue(EstadoAmbulancia.DISPONIBLE.puedeDespachar)
        assertFalse(EstadoAmbulancia.EN_SERVICIO.puedeDespachar)
        assertFalse(EstadoAmbulancia.EN_MANTENIMIENTO.puedeDespachar)
    }
}
