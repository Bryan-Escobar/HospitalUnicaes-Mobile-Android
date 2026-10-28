package sv.edu.unicaes.hospital.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.domain.model.Rol

class RolTest {
    @Test
    fun `desdeApi tolera mayusculas y espacios`() {
        assertEquals(Rol.DOCTOR, Rol.desdeApi(" doctor "))
        assertEquals(Rol.RECEPCIONISTA, Rol.desdeApi("Recepcionista"))
    }

    @Test
    fun `desdeApi devuelve DESCONOCIDO ante valores nuevos o nulos`() {
        assertEquals(Rol.DESCONOCIDO, Rol.desdeApi("CONSERJE"))
        assertEquals(Rol.DESCONOCIDO, Rol.desdeApi(null))
    }

    @Test
    fun `solo recepcion y asistente usan la app de recepcion`() {
        assertTrue(Rol.RECEPCIONISTA.usaAppRecepcion)
        assertTrue(Rol.ASISTENTE.usaAppRecepcion)
        assertFalse(Rol.DOCTOR.usaAppRecepcion)
        assertFalse(Rol.PACIENTE.usaAppRecepcion)
    }
}
