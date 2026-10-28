package sv.edu.unicaes.hospital.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.Sexo
import sv.edu.unicaes.hospital.ui.recepcion.CampoPaciente
import sv.edu.unicaes.hospital.ui.recepcion.FormularioPaciente
import sv.edu.unicaes.hospital.ui.recepcion.aDatos
import sv.edu.unicaes.hospital.ui.recepcion.cambiosRespectoA
import sv.edu.unicaes.hospital.ui.recepcion.edadDesde
import sv.edu.unicaes.hospital.ui.recepcion.fechaParaApi
import sv.edu.unicaes.hospital.ui.recepcion.fechaParaFormulario
import sv.edu.unicaes.hospital.ui.recepcion.formatearDui
import sv.edu.unicaes.hospital.ui.recepcion.validarPaciente
import java.util.Calendar
import java.util.GregorianCalendar

class ValidacionPacienteTest {
    private val hoy = GregorianCalendar(2026, Calendar.OCTOBER, 27)

    private val valido =
        FormularioPaciente(
            nombres = "María",
            apellidos = "López",
            documento = "01234567-8",
            nacimiento = "14/05/1990",
            telefono = "7000-0000",
        )

    private fun errores(form: FormularioPaciente) = validarPaciente(form, hoy)

    @Test
    fun `formulario completo no tiene errores`() {
        assertTrue(errores(valido).isEmpty())
    }

    @Test
    fun `campos obligatorios vacios`() {
        val resultado = errores(FormularioPaciente(nombres = "  "))
        listOf(
            CampoPaciente.NOMBRES,
            CampoPaciente.APELLIDOS,
            CampoPaciente.DOCUMENTO,
            CampoPaciente.NACIMIENTO,
            CampoPaciente.TELEFONO,
        ).forEach { assertEquals(R.string.campo_obligatorio, resultado[it]) }
    }

    @Test
    fun `opcionales vacios no dan error`() {
        val resultado = errores(valido.copy(direccion = "", contactoNombre = "", contactoTelefono = ""))
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `documento con formato de DUI`() {
        assertNull(errores(valido.copy(documento = "01234567-8"))[CampoPaciente.DOCUMENTO])
        listOf("012345678", "1234567-8", "01234567-89", "ABCDEFGH-1").forEach {
            assertEquals(it, R.string.error_dui_formato, errores(valido.copy(documento = it))[CampoPaciente.DOCUMENTO])
        }
    }

    @Test
    fun `formatea el DUI mientras se escribe`() {
        assertEquals("0123", formatearDui("0123"))
        assertEquals("01234567", formatearDui("01234567"))
        assertEquals("01234567-8", formatearDui("012345678"))
        assertEquals("01234567-8", formatearDui("01234567-8"))
        assertEquals("01234567-8", formatearDui("01234567-89"))
    }

    @Test
    fun `telefonos validos e invalidos`() {
        listOf("7000-0000", "70000000", "+503 7000-0000", "+50370000000").forEach {
            assertNull(it, errores(valido.copy(telefono = it))[CampoPaciente.TELEFONO])
        }
        listOf("7000", "7000-00000", "abcd-efgh").forEach {
            assertEquals(
                it,
                R.string.error_telefono_formato,
                errores(valido.copy(telefono = it))[CampoPaciente.TELEFONO],
            )
        }
    }

    @Test
    fun `telefono de emergencia solo se valida si se llena`() {
        assertNull(errores(valido.copy(contactoTelefono = ""))[CampoPaciente.CONTACTO_TELEFONO])
        assertEquals(
            R.string.error_telefono_formato,
            errores(valido.copy(contactoTelefono = "123"))[CampoPaciente.CONTACTO_TELEFONO],
        )
    }

    @Test
    fun `fechas que no existen o futuras`() {
        assertEquals(
            R.string.error_fecha_invalida,
            errores(valido.copy(nacimiento = "31/02/1990"))[CampoPaciente.NACIMIENTO],
        )
        assertEquals(
            R.string.error_fecha_invalida,
            errores(valido.copy(nacimiento = "1990-05-14"))[CampoPaciente.NACIMIENTO],
        )
        assertEquals(
            R.string.error_fecha_invalida,
            errores(valido.copy(nacimiento = "01/01/1800"))[CampoPaciente.NACIMIENTO],
        )
        assertEquals(
            R.string.error_fecha_futura,
            errores(valido.copy(nacimiento = "28/10/2026"))[CampoPaciente.NACIMIENTO],
        )
        assertNull(errores(valido.copy(nacimiento = "29/02/2024"))[CampoPaciente.NACIMIENTO])
        assertNull(errores(valido.copy(nacimiento = "27/10/2026"))[CampoPaciente.NACIMIENTO])
    }

    @Test
    fun `largo maximo`() {
        assertEquals(
            R.string.error_max_caracteres,
            errores(valido.copy(nombres = "a".repeat(101)))[CampoPaciente.NOMBRES],
        )
        assertEquals(
            R.string.error_max_caracteres,
            errores(valido.copy(direccion = "a".repeat(251)))[CampoPaciente.DIRECCION],
        )
    }

    @Test
    fun `convierte la fecha entre formulario y API`() {
        assertEquals("1990-05-14", fechaParaApi("14/05/1990"))
        assertEquals("14/05/1990", fechaParaFormulario("1990-05-14"))
        assertEquals("", fechaParaFormulario(null))
    }

    @Test
    fun `calcula la edad segun el cumpleanos`() {
        assertEquals(36, edadDesde("1990-05-14", hoy))
        assertEquals(35, edadDesde("1990-10-28", hoy))
        assertEquals(36, edadDesde("1990-10-27", hoy))
        assertNull(edadDesde(null, hoy))
    }

    @Test
    fun `al crear los opcionales vacios no se envian`() {
        val datos = valido.copy(nombres = " María ").aDatos()
        assertEquals("María", datos.nombres)
        assertEquals("1990-05-14", datos.fechaNacimiento)
        assertNull(datos.direccion)
        assertNull(datos.telefonoEmergencia)
    }

    @Test
    fun `al editar solo viajan los campos que cambiaron`() {
        val cambios = valido.copy(telefono = "7111-1111", sexo = Sexo.FEMENINO).cambiosRespectoA(valido)
        assertEquals(DatosPaciente(telefono = "7111-1111", sexo = Sexo.FEMENINO), cambios)
    }

    @Test
    fun `sin cambios no hay nada que enviar`() {
        assertNull(valido.copy(nombres = "María ").cambiosRespectoA(valido))
    }
}
