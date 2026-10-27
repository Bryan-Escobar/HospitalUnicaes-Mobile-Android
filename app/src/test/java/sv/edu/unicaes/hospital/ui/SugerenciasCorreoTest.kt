package sv.edu.unicaes.hospital.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import sv.edu.unicaes.hospital.ui.auth.sugerenciasCorreo

class SugerenciasCorreoTest {
    private val dominios = listOf("gmail.com", "hotmail.com", "outlook.com", "yahoo.com")

    @Test
    fun `sin arroba sugiere los primeros dominios`() {
        assertEquals(
            listOf("ana@gmail.com", "ana@hotmail.com", "ana@outlook.com"),
            sugerenciasCorreo("ana", dominios),
        )
    }

    @Test
    fun `filtra por el dominio a medio escribir sin importar mayusculas`() {
        assertEquals(listOf("ana@hotmail.com"), sugerenciasCorreo("ana@HO", dominios))
    }

    @Test
    fun `no sugiere si el correo ya esta completo o no es valido`() {
        assertEquals(emptyList<String>(), sugerenciasCorreo("ana@gmail.com", dominios))
        assertEquals(emptyList<String>(), sugerenciasCorreo("", dominios))
        assertEquals(emptyList<String>(), sugerenciasCorreo("@gm", dominios))
        assertEquals(emptyList<String>(), sugerenciasCorreo("ana@hospital.sv", dominios))
    }
}
