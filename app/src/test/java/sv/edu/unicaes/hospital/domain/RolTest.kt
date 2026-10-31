package sv.edu.unicaes.hospital.domain

import org.junit.Assert.assertEquals
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
    fun `desdeApi reconoce los cuatro roles de la base`() {
        assertEquals(Rol.ADMINISTRADOR, Rol.desdeApi("administrador"))
        assertEquals(Rol.DOCTOR, Rol.desdeApi("doctor"))
        assertEquals(Rol.RECEPCIONISTA, Rol.desdeApi("recepcionista"))
        assertEquals(Rol.PACIENTE, Rol.desdeApi("paciente"))
    }

    @Test
    fun `asistente y enfermero ya no son roles`() {
        assertEquals(Rol.DESCONOCIDO, Rol.desdeApi("asistente"))
        assertEquals(Rol.DESCONOCIDO, Rol.desdeApi("enfermero"))
    }
}
