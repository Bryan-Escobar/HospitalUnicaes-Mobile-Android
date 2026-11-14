package sv.edu.unicaes.hospital.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.core.rechazoDesdeMensaje
import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.ui.modelo.etiquetaSemana
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital
import sv.edu.unicaes.hospital.ui.modelo.horaValida
import sv.edu.unicaes.hospital.ui.modelo.misTurnosSemana
import sv.edu.unicaes.hospital.ui.modelo.semanaDe
import sv.edu.unicaes.hospital.ui.turnos.MisTurnosViewModel
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class TurnosTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `la semana va de lunes a domingo aunque hoy sea domingo`() {
        val domingo = fechaHoraHospital("2026-11-15", "10:00")!!
        assertEquals(
            listOf("2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13", "2026-11-14", "2026-11-15"),
            semanaDe(0, domingo),
        )
        assertEquals("2026-11-16", semanaDe(1, domingo).first())
        assertEquals("2026-11-02", semanaDe(-1, domingo).first())
    }

    @Test
    fun `cada dia de la semana empieza en lunes`() {
        val lunes = semanaDe(0).first()
        val dia = Calendar.getInstance().apply { time = fechaHoraHospital(lunes, "12:00")!! }
        assertEquals(Calendar.MONDAY, dia.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `etiqueta de la semana`() {
        assertEquals("9 – 15 nov", etiquetaSemana(semanaDe(0, fechaHoraHospital("2026-11-13")!!)))
        assertEquals("26 oct – 1 nov", etiquetaSemana(semanaDe(0, fechaHoraHospital("2026-10-28")!!)))
    }

    @Test
    fun `valida horas HH mm`() {
        assertTrue(horaValida("07:00"))
        assertTrue(horaValida("23:59"))
        assertFalse(horaValida("24:00"))
        assertFalse(horaValida("7:00"))
        assertFalse(horaValida("07:60"))
    }

    @Test
    fun `el solape de la base se muestra tal cual`() {
        val error = rechazoDesdeMensaje("TURNO_SOLAPADO: el empleado ya tiene un turno el 22/10/2026 de 14:00 a 22:00")
        assertEquals("El empleado ya tiene un turno el 22/10/2026 de 14:00 a 22:00", error?.detalle)
    }

    @Test
    fun `mis turnos marca dias libres, en curso y terminados`() {
        val dias = semanaDe(0, fechaHoraHospital("2026-11-11")!!)
        val turnos =
            listOf(
                turno("1", "2026-11-09", inicio = "07:00", fin = "15:00"),
                turno("2", "2026-11-11", inicio = "08:00", fin = "16:00"),
                turno("3", "2026-11-13", inicio = "19:00", fin = "07:00", tipoTurno = TipoTurno.NOCHE).copy(
                    fin = "2026-11-14T07:00:00-06:00",
                ),
            )
        val ahora = fechaHoraHospital("2026-11-11", "10:00")!!

        val semana = misTurnosSemana(dias, turnos, ahora)

        assertEquals(7, semana.size)
        assertTrue(semana[0].terminado)
        assertTrue(semana[1].sinTurno)
        assertTrue(semana[2].enCurso)
        assertEquals("Emergencia · nocturno", semana[4].area)
        assertEquals("19:00–07:00", semana[4].horario)
    }

    @Test
    fun `mis turnos queda vacio si no hay ninguno en dos semanas`() {
        val vm = MisTurnosViewModel(FakeTurnosRepository())
        assertEquals(UiState.Vacio, vm.uiState.value)
    }

    @Test
    fun `mis turnos separa esta semana y la siguiente`() {
        val esta = semanaDe(0)
        val siguiente = semanaDe(1)
        val repo =
            FakeTurnosRepository(
                mios = Resultado.Exito(listOf(turno("1", esta[0]), turno("2", siguiente[2]))),
            )

        val (estaSemana, otra) = (MisTurnosViewModel(repo).uiState.value as UiState.Exito).datos

        assertFalse(estaSemana[0].sinTurno)
        assertTrue(estaSemana.drop(1).all { it.sinTurno })
        assertFalse(otra[2].sinTurno)
    }
}
